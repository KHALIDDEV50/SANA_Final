package com.example.sana.Service.CreativityDomainServices;

import com.example.sana.API.ApiException;
import com.example.sana.DTO.CreativityDomainDTOs.MoodboardImageDTO;
import com.example.sana.DTO.CreativityDomainDTOs.MoodboardImageDTO.Image;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

/** Four automatically searched image libraries. Old OpenverseService is left intact for saved boards. */
@Service
@Slf4j
public class MoodboardImageService {
    private final JsonMapper mapper;
    private final RestClient pixabay;
    private final RestClient cleveland;
    private final RestClient met;
    private final RestClient commons;
    private final String pixabayKey;
    private final PixabayImageStorage storage;
    private final Map<String, Cached> pixabayCache = new ConcurrentHashMap<>();
    private record Cached(String body, Instant expires) {}

    // Conservative text-only screening: a substitute for neither visual moderation nor human review.
    private static final Pattern UNSAFE = Pattern.compile(
            "(?iu)(?:\\bnud(?:e|ity)\\b|\\bnaked\\b|\\btopless\\b|\\berotic\\b|\\bporn(?:ographic|ography)?\\b|\\bsexual\\b|\\bgenital\\b|\\bbreast\\b|\\bunderwear\\b|\\blingerie\\b|\\bbikini\\b|\\bswimsuit\\b|\\bgore\\b|\\bdecapitat(?:e|ion)\\b|\\bbloodbath\\b|عاري|عارية|عري|إباح|جنسي|ملابس داخلية|دموي)"
    );

    public MoodboardImageService(JsonMapper mapper, PixabayImageStorage storage,
                                 @Value("${sana.images.pixabay.api-key:}") String pixabayKey) {
        this.mapper = mapper;
        this.storage = storage;
        this.pixabayKey = pixabayKey == null ? "" : pixabayKey.trim();
        if (this.pixabayKey.isBlank()) {
            log.warn("SANA Moodboard: PIXABAY_API_KEY is missing; Pixabay searches are disabled");
        }
        SimpleClientHttpRequestFactory f = new SimpleClientHttpRequestFactory();
        f.setConnectTimeout(Duration.ofSeconds(4));
        f.setReadTimeout(Duration.ofSeconds(8));
        this.pixabay = RestClient.builder().baseUrl("https://pixabay.com").requestFactory(f).build();
        this.cleveland = RestClient.builder().baseUrl("https://openaccess-api.clevelandart.org").requestFactory(f).build();
        this.met = RestClient.builder().baseUrl("https://collectionapi.metmuseum.org").requestFactory(f).build();
        this.commons = RestClient.builder().baseUrl("https://commons.wikimedia.org")
                .defaultHeader("User-Agent", "SANA-Moodboard/1.0 (creative project image reference search)")
                .requestFactory(f).build();
    }

    /** Every request searches all four sources; users don't choose providers. */
    public MoodboardImageDTO.SearchResult searchImages(String keyword, Integer page, Integer pageSize) {
        if (keyword == null || keyword.isBlank() || keyword.length() > 120)
            throw new ApiException("Image search needs 1-120 characters");
        int p = page == null ? 1 : page;
        int size = pageSize == null ? 16 : pageSize;
        if (p < 1 || p > 20 || size < 1 || size > 24) throw new ApiException("Invalid image search page");
        String query = keyword.trim();
        List<List<Image>> sets = new ArrayList<>();
        // Isolated parallel calls: one unavailable museum must not stall the whole search.
        List<CompletableFuture<List<Image>>> searches = new ArrayList<>();
        for (String source : List.of("PIXABAY", "CLEVELAND", "MET", "WIKIMEDIA")) {
            if ("PIXABAY".equals(source) && pixabayKey.isBlank()) continue;
            searches.add(CompletableFuture.supplyAsync(() -> {
                try {
                    List<Image> found = switch (source) {
                        case "PIXABAY" -> searchPixabay(query, p, size);
                        case "CLEVELAND" -> searchCleveland(query, p, size);
                        case "MET" -> searchMet(query, p, size);
                        default -> searchCommons(query, p, size);
                    };
                    log.info("SANA Moodboard provider={} eligibleImages={}", source, found.size());
                    return found;
                } catch (RuntimeException ex) {
                    log.warn("SANA Moodboard {} search failed: {}", source, ex.getMessage());
                    return List.<Image>of();
                }
            }).completeOnTimeout(List.of(), 12, TimeUnit.SECONDS));
        }
        for (var search : searches) sets.add(search.join());
        Map<String,Image> merged = new LinkedHashMap<>();
        // Interleave providers rather than letting one provider monopolize the first page.
        for (int i=0; i<size && merged.size()<size; i++) {
            for (List<Image> set: sets) {
                if (i<set.size()) merged.putIfAbsent(set.get(i).id(), set.get(i));
                if (merged.size()>=size) break;
            }
        }
        log.info("SANA Moodboard merged eligibleImages={} searchedProviders={}", merged.size(), searches.size());
        return new MoodboardImageDTO.SearchResult(p, 0, merged.size(), List.copyOf(merged.values()));
    }

    /** Never trust client-supplied URLs or attribution; re-read the selected ID from its provider. */
    public Image prepareToSave(String providerArg, String suppliedId) {
        if (suppliedId == null || suppliedId.length()>160 || !suppliedId.contains(":"))
            throw new ApiException("Invalid selected image ID");
        String prefix = suppliedId.substring(0,suppliedId.indexOf(':')).toUpperCase(Locale.ROOT);
        String id = suppliedId.substring(suppliedId.indexOf(':')+1);
        if (providerArg != null && !providerArg.isBlank() && !prefix.equalsIgnoreCase(providerArg))
            throw new ApiException("Image provider mismatch");
        Image image = switch(prefix) {
            case "PIXABAY" -> {
                if (pixabayKey.isBlank() || !id.matches("[0-9]{1,15}")) throw new ApiException("Invalid Pixabay image");
                yield pixabayImage(pixabayJson("detail:"+id,true,id).path("hits").path(0));
            }
            case "CLEVELAND" -> {
                if (!id.matches("[0-9]{1,15}")) throw new ApiException("Invalid Cleveland image");
                yield clevelandImage(getJson(cleveland.get().uri("/api/artworks/{id}",id).retrieve().body(String.class)).path("data"));
            }
            case "MET" -> {
                if (!id.matches("[0-9]{1,15}")) throw new ApiException("Invalid Met image");
                yield metImage(getJson(met.get().uri("/public/collection/v1/objects/{id}",id).retrieve().body(String.class)));
            }
            case "WIKIMEDIA" -> {
                if (!id.matches("[0-9]{1,15}")) throw new ApiException("Invalid Wikimedia file ID");
                JsonNode root = getJson(commons.get().uri(b -> b.path("/w/api.php")
                        .queryParam("action","query").queryParam("format","json")
                        .queryParam("pageids",id).queryParam("prop","imageinfo")
                        .queryParam("iiprop","url|extmetadata|mime|user").queryParam("iiurlwidth",550).build())
                        .retrieve().body(String.class));
                yield commonsImage(root.path("query").path("pages").path(id));
            }
            default -> throw new ApiException("Unsupported image provider");
        };
        if (image==null || !suppliedId.equals(image.id())) throw new ApiException("Image was unavailable or did not pass safety/licence checks");
        return "PIXABAY".equals(prefix) ? storage.store(image) : image;
    }

    private List<Image> searchPixabay(String query, int page, int size) {
        JsonNode root = pixabayJson("search:"+query+":"+page+":"+size,false,query+"|"+page+"|"+size);
        List<Image> result = new ArrayList<>();
        for (JsonNode hit: root.path("hits")) { Image i=pixabayImage(hit); if(i!=null)result.add(i); }
        return result;
    }
    private JsonNode pixabayJson(String cacheKey,boolean detail,String argument) {
        Cached cached=pixabayCache.get(cacheKey);
        if(cached!=null && cached.expires.isAfter(Instant.now()))return getJson(cached.body);
        String body;
        if(detail) body=pixabay.get().uri(b->b.path("/api/").queryParam("key",pixabayKey)
                .queryParam("id",argument).queryParam("safesearch",true).build()).retrieve().body(String.class);
        else {
            String[] a=argument.split("\\|",3);
            body=pixabay.get().uri(b->b.path("/api/").queryParam("key",pixabayKey)
                    .queryParam("q",a[0].substring(0,Math.min(100,a[0].length())))
                    .queryParam("page",a[1]).queryParam("per_page",Math.max(3,Integer.parseInt(a[2])))
                    .queryParam("image_type","all").queryParam("safesearch",true).build())
                    .retrieve().body(String.class);
        }
        if(pixabayCache.size()>400) pixabayCache.entrySet().removeIf(e->e.getValue().expires.isBefore(Instant.now()));
        pixabayCache.put(cacheKey,new Cached(body,Instant.now().plus(Duration.ofHours(24))));
        return getJson(body);
    }
    private Image pixabayImage(JsonNode v) {
        String id=value(v,"id"),title=value(v,"tags"),url=https(v,"webformatURL"),thumb=https(v,"previewURL");
        String page=https(v,"pageURL"),author=value(v,"user");
        if(id==null||!id.matches("[0-9]{1,15}")||url==null||page==null||unsafe(title))return null;
        return new Image("PIXABAY:"+id, or(title,"Pixabay image"),url,thumb,author,null,
                "Pixabay Content License",null,"https://pixabay.com/service/license-summary/",page,
                "Image by "+or(author,"Pixabay contributor")+" on Pixabay","PIXABAY");
    }
    private List<Image> searchCleveland(String query,int page,int size) {
        JsonNode root=getJson(cleveland.get().uri(b->b.path("/api/artworks/")
                .queryParam("q",query).queryParam("cc0").queryParam("has_image",1)
                .queryParam("skip",(page-1)*size).queryParam("limit",size).build())
                .retrieve().body(String.class));
        List<Image> result=new ArrayList<>();
        for(JsonNode artwork:root.path("data")){Image i=clevelandImage(artwork);if(i!=null)result.add(i);}
        return result;
    }
    private Image clevelandImage(JsonNode v) {
        String id=value(v,"id"),license=value(v,"share_license_status"),title=value(v,"title");
        if(id==null||!id.matches("[0-9]{1,15}")||!"CC0".equalsIgnoreCase(license)||unsafe(title)||unsafe(plain(value(v,"description")))||unsafe(plain(value(v,"tombstone"))))return null;
        JsonNode images=v.path("images");
        String web=https(images.path("web"),"url"),print=https(images.path("print"),"url");
        if(web==null)return null;
        String url=https(v,"url");
        if(url==null)url="https://www.clevelandart.org/art/"+id;
        String artist=value(v,"creators"); // creators is usually an array, handled below
        if(v.path("creators").isArray()&&v.path("creators").size()>0)artist=value(v.path("creators").path(0),"description");
        return new Image("CLEVELAND:"+id,or(title,"Cleveland artwork"),or(print,web),web,artist,null,
                "CC0",null,"https://creativecommons.org/publicdomain/zero/1.0/",url,
                "The Cleveland Museum of Art"+(artist==null?"":" · "+artist),"CLEVELAND");
    }
    private List<Image> searchMet(String query,int page,int size) {
        JsonNode response=getJson(met.get().uri(b->b.path("/public/collection/v1.1/search")
                .queryParam("q",query).queryParam("hasImages",true)
                .queryParam("offset",(page-1)*size).queryParam("limit",size).build())
                .retrieve().body(String.class));
        List<Image> result=new ArrayList<>();
        // API search returns IDs only. Revalidate each record for public-domain artwork.
        List<CompletableFuture<Image>> details = new ArrayList<>();
        for(JsonNode id: response.path("objectIDs")) {
            if(details.size() >= Math.min(size,10)) break;
            String objectId = id.asText();
            details.add(CompletableFuture.supplyAsync(() -> {
                try { return metImage(getJson(met.get().uri("/public/collection/v1/objects/{id}",objectId)
                            .retrieve().body(String.class))); }
                catch(RuntimeException e) { log.debug("Met object skipped: {}",e.getMessage()); return null; }
            }).completeOnTimeout(null,10,TimeUnit.SECONDS));
        }
        for(var detail:details) {Image image=detail.join();if(image!=null)result.add(image);}
        return result;
    }
    private Image metImage(JsonNode v) {
        String id=value(v,"objectID"),title=value(v,"title");
        if(id==null||!id.matches("[0-9]{1,15}")||!v.path("isPublicDomain").asBoolean(false)||unsafe(title)||unsafe(value(v,"objectName"))||unsafe(v.path("tags").toString()))return null;
        String full=https(v,"primaryImage"),thumb=https(v,"primaryImageSmall");
        if(full==null&&thumb==null)return null;
        String page=https(v,"objectURL");
        if(page==null)page="https://www.metmuseum.org/art/collection/search/"+id;
        return new Image("MET:"+id,or(title,"Met artwork"),or(thumb,full),thumb,value(v,"artistDisplayName"),null,
                "Public Domain",null,"https://www.metmuseum.org/about-the-met/policies-and-documents/open-access",page,
                "The Metropolitan Museum of Art","MET");
    }
    private List<Image> searchCommons(String query,int page,int size) {
        JsonNode root=getJson(commons.get().uri(b->b.path("/w/api.php")
                .queryParam("action","query").queryParam("format","json")
                .queryParam("generator","search").queryParam("gsrsearch",query)
                .queryParam("gsrnamespace",6).queryParam("gsrlimit",Math.min(size,18))
                .queryParam("gsroffset",(page-1)*Math.min(size,18))
                .queryParam("prop","imageinfo").queryParam("iiprop","url|extmetadata|mime|user")
                .queryParam("iiurlwidth",550).build()).retrieve().body(String.class));
        List<Image> result=new ArrayList<>();
        for(JsonNode v:root.path("query").path("pages")) {Image image=commonsImage(v);if(image!=null)result.add(image);}
        return result;
    }
    private Image commonsImage(JsonNode v) {
        String id=value(v,"pageid"),title=value(v,"title");
        if(id==null||!id.matches("[0-9]{1,15}")||unsafe(title))return null;
        JsonNode info=v.path("imageinfo").path(0),meta=info.path("extmetadata");
        String mime=value(info,"mime");
        if(!List.of("image/jpeg","image/png","image/webp").contains(mime))return null;
        String license=metaValue(meta,"LicenseShortName"),licenseUrl=metaValue(meta,"LicenseUrl");
        if(license==null || !Pattern.compile("(?i)^(?:CC0|CC BY(?:-SA)?(?: [0-9.]+)?|public domain|PD[- ].*)$").matcher(license.trim()).matches())return null;
        if(licenseUrl!=null&&!licenseUrl.startsWith("https://")) licenseUrl=null;
        String image=https(info,"url"),thumb=https(info,"thumburl");
        if(image==null||unsafe(metaValue(meta,"ImageDescription")))return null;
        String creator=plain(metaValue(meta,"Artist"));
        String page="https://commons.wikimedia.org/?curid="+id;
        return new Image("WIKIMEDIA:"+id,or(title,"Wikimedia image").replaceFirst("^File:",""),image,thumb,
                creator,null,license,null,licenseUrl,page,
                "Wikimedia Commons · "+or(creator,"contributor")+" · "+license,"WIKIMEDIA");
    }
    private static boolean unsafe(String value) {return value!=null && UNSAFE.matcher(value).find();}
    private static String metaValue(JsonNode v,String key){return value(v.path(key),"value");}
    private static String plain(String s){return s==null?null:s.replaceAll("<[^>]*>"," ").replaceAll("&[^;]{1,12};"," ").replaceAll("\\s+"," ").trim();}
    private static String or(String s,String fallback){return s==null||s.isBlank()?fallback:s;}
    private JsonNode getJson(String body) {
        if(body==null||body.isBlank())throw new ApiException("Image provider returned no data");
        return mapper.readTree(body);
    }
    private static String value(JsonNode node,String key){JsonNode v=node.path(key);return v.isMissingNode()||v.isNull()||v.asText().isBlank()?null:v.asText().trim();}
    private static String https(JsonNode v,String field) {
        String s=value(v,field);
        if(s==null)return null;
        try {URI u=URI.create(s);return "https".equalsIgnoreCase(u.getScheme())&&u.getHost()!=null&&u.getUserInfo()==null?s:null;}
        catch(IllegalArgumentException e){return null;}
    }
}
