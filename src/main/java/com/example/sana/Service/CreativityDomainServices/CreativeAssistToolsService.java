package com.example.sana.Service.CreativityDomainServices;

import com.example.sana.API.ApiException;
import com.example.sana.DTO.AIDTOs.AIRunResponseDTO;
import com.example.sana.DTO.CreativityDomainDTOs.CharacterBalanceReportDTO;
import com.example.sana.DTO.CreativityDomainDTOs.CreativityAIRequestDTO;
import com.example.sana.DTO.CreativityDomainDTOs.OpenLibraryResponseDTO;
import com.example.sana.Enums.AITask;
import com.example.sana.Enums.ExecutionStatus;
import com.example.sana.Model.CreativityDomainModels.PlayableCharacter;
import com.example.sana.Model.ProjectOutput;
import com.example.sana.Repository.CreativityDomainRepository.PlayableCharacterRepository;
import com.example.sana.Repository.ProjectOutputRepository;
import com.example.sana.Service.AIServices.AIRequestService;
import com.example.sana.Service.DiagramServices.DiagramWriteGuard;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/** Project-aware AI assistance. Uses the existing Creativity ADVISE workflow, never changes graphs. */
@Service
@RequiredArgsConstructor
public class CreativeAssistToolsService {
    private static final Logger log = LoggerFactory.getLogger(CreativeAssistToolsService.class);
    private final CreativityAIRequestService creativityAI;
    private final AIRequestService ai;
    private final ProjectOutputRepository outputs;
    private final PlayableCharacterRepository fighters;
    private final CharacterBalanceService balance;
    private final OpenLibraryService library;
    private final JsonMapper json;
    private final Map<String,Draft> drafts = new ConcurrentHashMap<>();

    public record Stats(int attack, int defense, int speed, int rangeStat, int stamina, int special) {}
    public record BalanceSuggestion(Integer characterId, String name, Stats before, Stats after, String reason) {}
    public record BookSuggestion(OpenLibraryResponseDTO.BookInfo book, String reason) {}
    public record Preview(String draftId, String status, String kind, String summary,
                          List<BalanceSuggestion> changes, List<BookSuggestion> books,
                          String error, boolean applied) {}

    private static class Draft {
        String id, kind, stage, requestId, summary="", error;
        Integer projectId, userId, outputId;
        Instant expires;
        boolean applied;
        List<BalanceSuggestion> changes=List.of();
        List<BookSuggestion> books=List.of();
        Map<Integer,Stats> before=Map.of();
        List<OpenLibraryResponseDTO.BookInfo> candidates=List.of();
        Draft(String kind,Integer projectId,Integer userId,Integer outputId){
            this.id=UUID.randomUUID().toString();this.kind=kind;this.stage="GENERATING";
            this.projectId=projectId;this.userId=userId;this.outputId=outputId;
            this.expires=Instant.now().plus(25,ChronoUnit.MINUTES);
        }
    }

    /** No artificial graph or shared contract changes: select an existing story map for ADVISE. */
    private ProjectOutput story(Integer projectId) {
        return outputs.findAllByProject_Id(projectId).stream()
                .filter(o -> "STORY_MAP".equals(o.getOutputType()) && o.getGraphRevision()!=null)
                .max(Comparator.comparing(ProjectOutput::getId))
                .orElseThrow(()->new ApiException("احفظي خريطة القصة أولًا حتى تفهم سنا المشروع."));
    }
    private AIRunResponseDTO run(Integer projectId,ProjectOutput output,String prompt){
        CreativityAIRequestDTO req=new CreativityAIRequestDTO();
        req.setTask(AITask.ADVISE);
        req.setDiagramType("STORY_MAP");
        req.setOutputId(output.getId());
        req.setBaseRevision(output.getGraphRevision());
        req.setPrompt(prompt.length()>9700?prompt.substring(0,9700):prompt);
        req.setSelectedElementIds(List.of());
        req.setConversationHistory(List.of());
        return creativityAI.start(projectId,req);
    }
    private void tidy(){ drafts.entrySet().removeIf(e->e.getValue().expires.isBefore(Instant.now())); }
    private static Stats stats(PlayableCharacter c){
        if(c.getAttack()==null||c.getDefense()==null||c.getSpeed()==null||c.getRangeStat()==null||c.getStamina()==null||c.getSpecial()==null)return null;
        return new Stats(c.getAttack(),c.getDefense(),c.getSpeed(),c.getRangeStat(),c.getStamina(),c.getSpecial());
    }
    private static Stats readStats(JsonNode node){
        if(node==null || !node.isObject())throw new ApiException("AI returned invalid character stats");
        int[] a=new int[6];String[] keys={"attack","defense","speed","rangeStat","stamina","special"};
        for(int i=0;i<6;i++){
            JsonNode v=node.path(keys[i]);
            if(!v.isIntegralNumber() || v.asInt()<0 || v.asInt()>10)throw new ApiException("AI stats must be integers from 0 to 10");
            a[i]=v.asInt();
        }
        return new Stats(a[0],a[1],a[2],a[3],a[4],a[5]);
    }
    private static String text(JsonNode n,String key,int max){
        String v=n.path(key).asText("").trim();
        if(v.isBlank()||v.length()>max)throw new ApiException("AI response is missing "+key);
        return v;
    }
    public Preview startBalance(Integer projectId,Integer userId,Integer requestedOutputId,String focus){
        tidy();
        ProjectOutput output;
        if(requestedOutputId==null){output=story(projectId);}
        else {
            output=outputs.findProjectOutputById(requestedOutputId);
            if(output==null || output.getProject()==null || !projectId.equals(output.getProject().getId())
                    || !"STORY_MAP".equals(output.getOutputType()) || output.getGraphRevision()==null)
                throw new ApiException("خريطة القصة المختارة غير موجودة في هذا المشروع.");
        }
        List<PlayableCharacter> players=fighters.findByProjectOutput_IdOrderByIdAsc(output.getId());
        Map<Integer,Stats> before=new LinkedHashMap<>();
        StringBuilder facts=new StringBuilder();
        for(PlayableCharacter p:players){
            Stats s=stats(p);if(s==null)continue;
            before.put(p.getId(),s);
            facts.append("\n").append("id=").append(p.getId()).append(" name=").append(p.getName())
                .append(" role=").append(p.getRole()).append(" weapon=").append(p.getWeaponName())
                .append(" strengths=").append(p.getStrengths()).append(" weaknesses=").append(p.getWeaknesses())
                .append(" stats=").append(s);
        }
        if(before.isEmpty())throw new ApiException("أضيفي إحصائيات الشخصية الست أولًا.");
        CharacterBalanceReportDTO report=balance.analyzeBalance(output.getId());
        String prompt="SANA_BALANCE_PLAN. Suggest changes to PLAYABLE CHARACTER STATS only, not a graph. " +
            "Read project context using get_creativity_context, preserve roles, character identities, weapons and story. " +
            "This is ADVISE and MUST return ADVICE with message a serialized JSON object: " +
            "{\"summary\":\"Arabic explanation\",\"recommendations\":[{\"characterId\":integer,\"reason\":\"Arabic explanation\",\"after\":{\"attack\":0,\"defense\":0,\"speed\":0,\"rangeStat\":0,\"stamina\":0,\"special\":0}}]}. " +
            "Use only listed real character IDs. Each stat must be an integer 0..10. Max 6 recommendations; do not force equality of all characters. " +
            "Explain each recommendation. Do not save anything. Return no extra outer fields, proposal=null and clarification=null. " +
            "If no changes necessary return empty recommendations. Balance report: "+report+". Characters: "+facts+
            ". User focus: "+(focus==null?"General gameplay balance":focus.substring(0,Math.min(300,focus.length())));
        AIRunResponseDTO request=run(projectId,output,prompt);
        Draft d=new Draft("BALANCE",projectId,userId,output.getId());d.before=Map.copyOf(before);d.requestId=request.getRequestId();drafts.put(d.id,d);
        return view(d);
    }
    public Preview startBooks(Integer projectId,Integer userId,String focus){
        tidy();ProjectOutput output=story(projectId);
        String prompt="SANA_BOOKS_SEARCH. This is ADVISE. Read get_creativity_context first and ground searches in actual " +
            "project story, character conflicts, setting, events, genre and world. Return type ADVICE with serialized JSON message " +
            "{\"searchTerms\":[\"short English genre or subject\",\"another subject\"]}. " +
            "Produce 1 to 3 distinct realistic terms, each 2-5 words, suitable for Open Library book search. " +
            "Do not invent book titles, authors, links, or claim any books have been searched. No graph proposal. " +
            "User focus: "+(focus==null?"Recommend real books for this project":focus.substring(0,Math.min(350,focus.length())));
        AIRunResponseDTO r=run(projectId,output,prompt);
        Draft d=new Draft("BOOKS",projectId,userId,output.getId());d.stage="SEARCH_PLANNING";d.requestId=r.getRequestId();drafts.put(d.id,d);
        return view(d);
    }
    private Draft require(Integer projectId,Integer userId,String draftId){
        Draft d=drafts.get(draftId);
        if(d==null||!d.projectId.equals(projectId)||!d.userId.equals(userId)||d.expires.isBefore(Instant.now()))
            throw new ApiException("AI draft unavailable or expired; request another preview");
        return d;
    }
    private Preview view(Draft d){return new Preview(d.id,d.stage,d.kind,d.summary,d.changes,d.books,d.error,d.applied);}
    public Preview poll(Integer projectId,Integer userId,String draftId){
        Draft d=require(projectId,userId,draftId);
        synchronized(d){
            if(List.of("PENDING","FAILED","APPLIED").contains(d.stage))return view(d);
            AIRunResponseDTO r=ai.getRun(projectId,UUID.fromString(d.requestId));
            if(r.getStatus()==ExecutionStatus.FAILED || r.getStatus()==ExecutionStatus.TIMED_OUT){
                d.stage="FAILED";d.error="تعذر إكمال طلب سنا. تأكدي من اتصال n8n.";return view(d);
            }
            if(r.getStatus()!=ExecutionStatus.COMPLETED)return view(d);
            try{
                if(r.getResult()==null || r.getResult().type()!=com.example.sana.DTO.IntegrationDTOs.N8nCallbackDTO.ResultType.ADVICE)
                    throw new ApiException("Sana did not return structured ADVICE");
                JsonNode result=json.readTree(r.getResult().message());
                if("BALANCE".equals(d.kind))completeBalance(d,result);
                else if("SEARCH_PLANNING".equals(d.stage))completeBookSearch(d,result);
                else completeBookSelection(d,result);
            }catch(Exception e){d.stage="FAILED";d.error="تعذر قراءة ترشيحات سنا: "+e.getMessage();}
            return view(d);
        }
    }
    private void completeBalance(Draft d,JsonNode obj){
        d.summary=text(obj,"summary",1200);
        JsonNode changes=obj.path("recommendations");if(!changes.isArray()||changes.size()>6)throw new ApiException("Invalid balance recommendations");
        List<BalanceSuggestion> proposals=new ArrayList<>();Set<Integer> ids=new HashSet<>();
        for(JsonNode c:changes){
            JsonNode idNode=c.path("characterId");if(!idNode.isIntegralNumber())throw new ApiException("Invalid character ID");
            int id=idNode.asInt();
            if(!ids.add(id)||!d.before.containsKey(id))throw new ApiException("Unknown character ID");
            PlayableCharacter current=fighters.findByIdAndProjectOutput_Id(id,d.outputId);
            if(current==null)throw new ApiException("Character was deleted");
            Stats after=readStats(c.path("after"));
            if(after.equals(d.before.get(id)))continue;
            proposals.add(new BalanceSuggestion(id,current.getName(),d.before.get(id),after,text(c,"reason",800)));
        }
        d.changes=List.copyOf(proposals);d.stage="PENDING";
    }
    /**
     * The first AI response contains SEARCH TOPICS, not verified book titles.
     * Keep the existing OpenLibraryService as the only authority for real books.
     * Try the precise phrases first, then genuinely related shorter subjects.
     * Never silently turn HTTP errors into "there are no books".
     */
    private void completeBookSearch(Draft d, JsonNode obj) {
        JsonNode terms = obj.path("searchTerms");
        if (!terms.isArray() || terms.isEmpty() || terms.size() > 3)
            throw new ApiException("Invalid book search terms");

        LinkedHashSet<String> precise = new LinkedHashSet<>();
        for (JsonNode term : terms) {
            if (!term.isTextual()) continue;
            String query = term.asText().trim();
            if (!query.isBlank() && query.length() <= 80) precise.add(query);
        }
        if (precise.isEmpty()) throw new ApiException("No usable book search terms");

        Map<String, OpenLibraryResponseDTO.BookInfo> unique = new LinkedHashMap<>();
        int upstreamFailures = 0;
        String lastProblem = "";
        for (String query : precise) {
            try {
                addVerifiedBooks(unique, query);
            } catch (RuntimeException ex) {
                upstreamFailures++;
                lastProblem = ex.getMessage() == null ? ex.getClass().getSimpleName() : ex.getMessage();
                log.warn("SANA books: Open Library lookup failed for query '{}': {}", query, lastProblem);
                // If two calls fail, don't keep waiting on the same unavailable API.
                if (upstreamFailures >= 2 && unique.isEmpty()) break;
            }
        }

        if (unique.size() < 4 && upstreamFailures < 2) {
            for (String query : broaderBookQueries(precise)) {
                if (precise.stream().anyMatch(original -> original.equalsIgnoreCase(query))) continue;
                try {
                    addVerifiedBooks(unique, query);
                    if (unique.size() >= 12) break;
                } catch (RuntimeException ex) {
                    upstreamFailures++;
                    lastProblem = ex.getMessage() == null ? ex.getClass().getSimpleName() : ex.getMessage();
                    log.warn("SANA books: fallback Open Library lookup failed for '{}': {}", query, lastProblem);
                    if (upstreamFailures >= 2 && unique.isEmpty()) break;
                }
            }
        }

        if (unique.isEmpty()) {
            if (upstreamFailures > 0) {
                log.warn("SANA books: provider returned no usable books after {} failed request(s). Last error: {}",
                        upstreamFailures, lastProblem);
                throw new ApiException("تعذر الاتصال بمكتبة Open Library أو قراءة نتائجها. جرّبي البحث اليدوي وتحققي من سجل Spring Boot.");
            }
            throw new ApiException("لم تظهر كتب لهذه المواضيع حتى بعد توسيع البحث؛ جرّبي وصفًا أو نوعًا أدبيًا مختلفًا.");
        }

        log.info("SANA books: {} verified candidates; {} upstream request failure(s)", unique.size(), upstreamFailures);
        d.candidates = List.copyOf(unique.values().stream().limit(16).toList());
        StringBuilder list = new StringBuilder();
        for (var book : d.candidates) {
            list.append("\n").append(book.id()).append(" | ")
                    .append(book.title()).append(" | ")
                    .append(book.authors() == null ? "" : String.join(", ", book.authors()))
                    .append(" | ").append(book.publishedDate());
        }
        ProjectOutput source = outputs.findProjectOutputById(d.outputId);
        if (source == null) throw new ApiException("Story output no longer exists");
        String prompt = "SANA_BOOKS_SELECT. Read get_creativity_context for the story. ADVISE task. Select 3 to 6 " +
                "genuinely relevant books ONLY from the verified Open Library candidate list below. " +
                "Return type ADVICE with message ONLY JSON {\"summary\":\"Arabic explanation\",\"recommendations\":[" +
                "{\"id\":\"exact OL...W identifier from supplied list\",\"reason\":\"Arabic explanation of relevance to story, without copying content\"}]}. " +
                "Never invent titles, IDs, authors or URLs. If fewer than 3 are relevant, return fewer. " +
                "No graph proposal, clarification=null, proposal=null. Verified candidates:" + list;
        AIRunResponseDTO next = run(d.projectId, source, prompt);
        d.requestId = next.getRequestId();
        d.stage = "SELECTING";
    }

    /** Retain only actual Open Library works with usable metadata; no fabricated placeholders. */
    private void addVerifiedBooks(Map<String, OpenLibraryResponseDTO.BookInfo> unique, String query) {
        var found = library.searchBooks(query, 10, 0);
        if (found == null || found.books() == null) return;
        log.info("SANA books: query '{}' returned {} valid book(s)", query, found.books().size());
        for (var book : found.books()) {
            if (book == null || book.id() == null || !book.id().matches("OL[0-9]+W")
                    || book.title() == null || book.title().isBlank()
                    || book.previewLink() == null || book.previewLink().isBlank()) continue;
            unique.putIfAbsent(book.id(), book);
        }
    }

    /** Fallback subjects are derived ONLY from the AI's actual topics. */
    private static List<String> broaderBookQueries(Collection<String> precise) {
        LinkedHashSet<String> genres = new LinkedHashSet<>();
        LinkedHashSet<String> otherSubjects = new LinkedHashSet<>();
        Set<String> genreWords = Set.of("mystery", "thriller", "horror", "fantasy", "detective",
                "romance", "adventure", "crime", "fiction", "historical", "psychological", "suspense");
        Set<String> fillers = Set.of("about", "with", "story", "stories", "books", "book", "novel",
                "town", "small", "old", "and", "the", "a", "of", "in", "for", "hidden");
        for (String phrase : precise) {
            String words = phrase.toLowerCase(Locale.ROOT).replaceAll("[^\\p{L}\\p{N}]+", " ").trim();
            for (String token : words.split("\\s+")) {
                if (genreWords.contains(token)) genres.add(token);
                else if (token.length() >= 5 && !fillers.contains(token)) otherSubjects.add(token);
            }
        }
        LinkedHashSet<String> queries = new LinkedHashSet<>();
        queries.addAll(genres);
        queries.addAll(otherSubjects);
        return queries.stream().limit(3).toList();
    }
    private void completeBookSelection(Draft d,JsonNode obj){
        d.summary=text(obj,"summary",1200);
        JsonNode rec=obj.path("recommendations");if(!rec.isArray()||rec.size()>6)throw new ApiException("Invalid book recommendations");
        Map<String,OpenLibraryResponseDTO.BookInfo> verified=new HashMap<>();
        d.candidates.forEach(book->verified.put(book.id(),book));
        List<BookSuggestion> selected=new ArrayList<>();Set<String> unique=new HashSet<>();
        for(JsonNode b:rec){
            String id=text(b,"id",40);if(!unique.add(id))continue;
            var book=verified.get(id);if(book==null)throw new ApiException("Book is not among Open Library results");
            selected.add(new BookSuggestion(book,text(b,"reason",600)));
        }
        d.books=List.copyOf(selected);d.stage="PENDING";
    }
    /** Approve all validated suggestions atomically; compare original stats to detect concurrent edits. */
    @Transactional
    public Preview approveBalance(Integer projectId,Integer userId,String draftId){
        Draft d=require(projectId,userId,draftId);
        synchronized(d){
            if(!"BALANCE".equals(d.kind)||!"PENDING".equals(d.stage)||d.applied)throw new ApiException("Not an approvable balance draft");
            ProjectOutput output=outputs.findProjectOutputById(d.outputId);
            if(output==null||!output.getProject().getId().equals(projectId))throw new ApiException("Output not found");
            DiagramWriteGuard.rejectUnversionedWrite(output);
            List<PlayableCharacter> touched=new ArrayList<>();
            for(BalanceSuggestion s:d.changes){
                PlayableCharacter c=fighters.findByIdAndProjectOutput_Id(s.characterId(),d.outputId);
                if(c==null || !Objects.equals(stats(c),s.before()))
                    throw new ApiException("تغيّرت إحصائيات إحدى الشخصيات. أعيدي طلب المقارنة قبل التطبيق.");
                touched.add(c);
            }
            for(int i=0;i<d.changes.size();i++){
                Stats s=d.changes.get(i).after();PlayableCharacter c=touched.get(i);
                c.setAttack(s.attack());c.setDefense(s.defense());c.setSpeed(s.speed());
                c.setRangeStat(s.rangeStat());c.setStamina(s.stamina());c.setSpecial(s.special());
                c.setUpdatedAt(java.time.LocalDate.now());
            }
            fighters.saveAllAndFlush(touched);d.applied=true;d.stage="APPLIED";return view(d);
        }
    }
}
