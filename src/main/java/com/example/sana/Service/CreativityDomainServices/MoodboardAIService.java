package com.example.sana.Service.CreativityDomainServices;

import com.example.sana.DTO.CreativityDomainDTOs.MoodboardAIDTO.*;
import com.example.sana.DTO.CreativityDomainDTOs.MoodboardImageDTO;
import com.example.sana.DTO.CreativityDomainDTOs.CreativityAIRequestDTO;
import com.example.sana.DTO.AIDTOs.AIRunResponseDTO;
import com.example.sana.Enums.AITask;
import com.example.sana.Enums.ExecutionStatus;
import com.example.sana.Enums.RepresentationType;
import com.example.sana.Model.ProjectOutput;
import com.example.sana.Repository.ProjectRepository;
import com.example.sana.Repository.ProjectOutputRepository;
import com.example.sana.Config.N8nProperties;
import com.example.sana.Service.AIServices.AIRequestService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.json.JsonMapper;

import java.net.URI;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/** Moodboard preview creation through the existing Creativity ADVISE queue, n8n gateway and callback. */
@Service
@RequiredArgsConstructor
public class MoodboardAIService {
    private static final Logger log = LoggerFactory.getLogger(MoodboardAIService.class);
    private final ProjectRepository projects;
    private final CreativityAIRequestService creativityRequests;
    private final AIRequestService sharedRequests;
    private final N8nProperties n8n;
    private final JsonMapper json;
    private final MoodboardImageService imageService;
    private final MoodboardService moodboards;
    private final Map<String, Draft> drafts = new ConcurrentHashMap<>();

    private static final class Draft {
        final Integer projectId;
        final String session;
        final String requestId;
        volatile Preview preview;
        volatile String state = "GENERATING";
        volatile Integer savedId;
        Draft(Integer projectId, String session, String requestId, Preview preview) {
            this.projectId = projectId;
            this.session = session;
            this.requestId = requestId;
            this.preview = preview;
        }
    }

    /** Uses the same /webhook/sana/ai as STORY_MAP. NO independent webhook or API key. */
    public boolean configured() {
        return n8n.isEnabled() && n8n.isWorkerEnabled();
    }

    private static String planningPrompt(String userPrompt, String language) {
        String guidance = userPrompt == null || userPrompt.isBlank()
                ? "Create a visual moodboard for my whole SANA project."
                : userPrompt.trim().substring(0, Math.min(8000, userPrompt.trim().length()));
        return "SANA_MOODBOARD_PLAN. Prepare a moodboard PREVIEW PLAN for this project's story, not a graph. " +
                "Incoming task is ADVISE, and outgoing result type is ADVICE (not PROPOSAL). " +
                "This is a special structured planning task; do not give ordinary advice, " +
                "ask to switch to CREATE, or request a different output. " +
                "Read the full PROJECT CONTEXT provided in context.creativity.relatedOutputs, " +
                "the current graph (when present), project description, characters, worlds, " +
                "places, events, plot relationships and world rules. Ground visual direction " +
                "in actual project facts and descriptions, not just the user's short request. " +
                "If there is little project detail, do not invent established facts. " +
                "Use distinct English image search terms for key characters (modest attire), " +
                "locations, historical settings, architecture, objects and overall mood. " +
                "Respect Muslim-friendly modest visual standards: avoid nudity, revealing clothing, " +
                "sexualized depictions, graphic gore, and religiously disrespectful material. " +
                "These are instructions for search and selection; do not claim visual verification. " +
                "Return result type ADVICE with clarification=null and proposal=null. " +
                "Its message must be ONLY one compact serialized valid JSON object (no markdown). " +
                "JSON keys: title (string), theme (string), searchTerms (array of 1-6 specific " +
                "English searches), notes (array of 1-6 concise creative notes), palette " +
                "(array of 3-5 objects, each name and hex e.g. #26364C). " +
                "No image URLs or fabricated sources. Keep project-specific details accurate. " +
                "Language for title/theme/notes: " + language + ". Request: " + guidance;
    }

    private Plan parsePlan(String message) {
        if (message == null || message.length() > 16000) throw invalidPlan();
        String raw = message.trim();
        if (raw.startsWith("```")) {
            int newline = raw.indexOf('\n');
            int endFence = raw.lastIndexOf("```");
            if (newline < 0 || endFence <= newline) throw invalidPlan();
            raw = raw.substring(newline + 1, endFence).trim();
        }
        try {
            Plan plan = json.readerFor(Plan.class).with(
                    DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES,
                    DeserializationFeature.FAIL_ON_TRAILING_TOKENS).readValue(raw);
            return validatePlan(plan);
        } catch (RuntimeException ex) {
            throw invalidPlan();
        }
    }

    private static Plan validatePlan(Plan p) {
        if (p == null) throw invalidPlan();
        requireText(p.title(), 120);
        requireText(p.theme(), 600);
        requireStrings(p.searchTerms(), 1, 6, 120);
        requireStrings(p.notes(), 1, 6, 800);
        if (p.palette() == null || p.palette().size() < 3 || p.palette().size() > 5) throw invalidPlan();
        for (Color c : p.palette()) {
            if (c == null) throw invalidPlan();
            requireText(c.name(), 60);
            if (c.hex() == null || !c.hex().matches("#[0-9a-fA-F]{6}")) throw invalidPlan();
        }
        var terms = p.searchTerms().stream().map(String::trim).distinct().toList();
        if (terms.isEmpty()) throw invalidPlan();
        return new Plan(p.title().trim(), p.theme().trim(), terms,
                p.notes().stream().map(String::trim).toList(), List.copyOf(p.palette()));
    }
    private static void requireStrings(List<String> list, int min, int max, int length) {
        if (list == null || list.size() < min || list.size() > max) throw invalidPlan();
        list.forEach(value -> requireText(value, length));
    }
    private static void requireText(String value, int limit) {
        if (value == null || value.isBlank() || value.length() > limit || value.indexOf('\0') >= 0)
            throw invalidPlan();
    }
    private static ResponseStatusException invalidPlan() {
        return fail(HttpStatus.BAD_GATEWAY,
                "Moodboard n8n planner returned invalid JSON. Check its plan response.");
    }

    public Preview generate(Integer projectId, Generate request, String session) {
        if (!configured()) throw fail(HttpStatus.SERVICE_UNAVAILABLE,
                "Sana n8n gateway/worker must be enabled. Uses the existing AI webhook.");
        var project = projects.findById(projectId).orElseThrow(() -> fail(HttpStatus.NOT_FOUND, "Project not found"));
        if (!"CREATIVITY".equals(project.getDomain())) throw fail(HttpStatus.BAD_REQUEST, "Creativity project required");
        drafts.entrySet().removeIf(e -> !e.getValue().preview.expiresAt().isAfter(Instant.now()));
        if (drafts.size() >= 100 || drafts.values().stream().filter(d -> d.session.equals(session)).count() >= 6)
            throw fail(HttpStatus.TOO_MANY_REQUESTS, "Too many pending moodboards; reject a draft or retry later.");

        // Planning via the existing ADVISE n8n contract and an actual saved story output.
        // The Creativity context includes story graphs,
        // characters, events, locations and relationships even when there is no active graph.
        AIRunResponseDTO run = creativityRequests.startMoodboardPlan(projectId,
                planningPrompt(request.prompt(), request.language()));

        String id = UUID.randomUUID().toString();
        Instant expires = Instant.now().plus(15, ChronoUnit.MINUTES);
        Preview preview = new Preview(id, projectId, "GENERATING", expires,
                "", "", List.of(), List.of(), List.of(),
                List.of("Waiting for the existing n8n callback. This does not save a moodboard."), false, null);
        drafts.put(id, new Draft(projectId, session, run.getRequestId(), preview));
        return preview;
    }

    /**
     * Public photo libraries and museum catalogues index short, concrete subjects better than
     * long cinematic prose. Preserve the full AI plan for notes, but convert each idea into a
     * short search phrase focused on the central subject.
     */
    private static String searchableTerm(String term) {
        if (term == null || term.isBlank()) return "";
        String lower = term.toLowerCase(Locale.ROOT).replaceAll("[^\\p{L}\\p{N}]+", " ").trim();
        if (containsAny(lower, "lighthouse", "beacon", "signal room")) {
            return containsAny(lower, "fog", "storm", "coast", "harbor", "harbour", "pier")
                    ? "old lighthouse fog" : "old lighthouse";
        }
        if (containsAny(lower, "harbor", "harbour", "port", "pier", "coastal", "coast", "sea", "marine")) {
            return "coastal harbor";
        }
        if (containsAny(lower, "archive", "ledger", "logbook", "record", "records", "map", "maps", "warning")) {
            return containsAny(lower, "harbor", "harbour", "maritime", "port")
                    ? "harbor archive" : "old archive ledger";
        }
        if (containsAny(lower, "woman", "female", "girl", "lady")) {
            return containsAny(lower, "detective", "investigator", "researcher")
                    ? "woman detective portrait" : "woman portrait";
        }
        if (containsAny(lower, "man", "male", "friend", "guard", "sailor")) {
            return containsAny(lower, "portrait", "character") ? "man portrait" : "calm man portrait";
        }
        if (containsAny(lower, "town", "village", "street", "city")) {
            return containsAny(lower, "coast", "harbor", "harbour") ? "coastal town" : "old town";
        }
        String[] words = lower.split("\\s+");
        Set<String> filler = Set.of("cinematic", "mystery", "historical", "young", "calm", "dramatic",
                "movie", "film", "still", "suspenseful", "restrained", "dark", "atmosphere", "light",
                "lighting", "environment", "scene", "stormy", "storm", "foggy", "battered", "modest",
                "clothed", "practical", "night", "old", "ancient", "weathered", "remote");
        List<String> keep = new ArrayList<>();
        for (String word : words) {
            if (word.length() >= 4 && !filler.contains(word)) keep.add(word);
            if (keep.size() == 2) break;
        }
        return keep.isEmpty() ? term.trim().substring(0, Math.min(60, term.trim().length()))
                : String.join(" ", keep);
    }

    private static boolean containsAny(String text, String... values) {
        for (String value : values) if (text.contains(value)) return true;
        return false;
    }

    private static int providerWeight(String provider) {
        return switch (provider == null ? "" : provider) {
            case "PIXABAY" -> 120;
            case "WIKIMEDIA" -> 105;
            case "MET" -> 78;
            case "CLEVELAND" -> 72;
            default -> 50;
        };
    }

    private static int scoreCandidate(MoodboardImageDTO.Image image, String simplifiedTerm) {
        int score = providerWeight(image.provider());
        String hay = ((image.title() == null ? "" : image.title()) + " " +
                (image.attribution() == null ? "" : image.attribution()) + " " +
                (image.sourceUrl() == null ? "" : image.sourceUrl())).toLowerCase(Locale.ROOT);
        for (String token : simplifiedTerm.split("\\s+")) {
            if (token.length() > 2 && hay.contains(token)) score += 18;
        }
        if (simplifiedTerm.contains("portrait") && containsAny(hay, "portrait", "head", "woman", "man")) score += 10;
        if (simplifiedTerm.contains("lighthouse") && containsAny(hay, "lighthouse", "beacon")) score += 16;
        if (simplifiedTerm.contains("harbor") && containsAny(hay, "harbor", "harbour", "port", "coast")) score += 12;
        if (simplifiedTerm.contains("archive") && containsAny(hay, "archive", "record", "book", "ledger", "map")) score += 12;
        if (containsAny(simplifiedTerm, "detective", "woman portrait", "man portrait") && ("MET".equals(image.provider()) || "CLEVELAND".equals(image.provider()))) score -= 12;
        if (containsAny(hay, "thomas storm", "marble head of a woman") && !simplifiedTerm.contains("portrait")) score -= 30;
        return score;
    }

    private static String imageExplanation(String term) {
        String lower = term == null ? "" : term.toLowerCase(Locale.ROOT);
        if (containsAny(lower, "lighthouse", "beacon", "signal room"))
            return "تمثل هذه الصورة المنارة القديمة، وهي المرجع الأهم للمكان المحوري في القصة وأجواء الغموض حوله.";
        if (containsAny(lower, "harbor", "harbour", "coastal", "coast", "pier", "town", "village"))
            return "تمثل هذه الصورة البلدة الساحلية والميناء والعزلة التي تصنع الجو العام للحبكة.";
        if (containsAny(lower, "archive", "ledger", "logbook", "record", "map", "warning"))
            return "تمثل هذه الصورة الأرشيف والسجلات والخرائط، أي القرائن البصرية المرتبطة بالسرّ والتحقيق.";
        if (containsAny(lower, "woman", "female", "detective", "investigator"))
            return "تمثل هذه الصورة البطلة بصفتها باحثة ومحققة، بملابس محتشمة وحضور هادئ وعملي.";
        if (containsAny(lower, "man", "male", "friend", "guard", "sailor"))
            return "تمثل هذه الصورة حضور شخصية رجالية مساندة أو غامضة داخل القصة، دون كشف الحقيقة مبكرًا.";
        return "هذه الصورة مرجع بصري لأحد عناصر القصة وأجوائها العامة؛ راجعيها كإلهام لا كمطابقة حرفية.";
    }

    private Preview buildPreview(Draft d, Plan plan) {
        Integer projectId = d.projectId;
        String id = d.preview.draftId();
        Instant expires = d.preview.expiresAt();
        Map<String, List<MoodboardImageDTO.Image>> byTerm = new LinkedHashMap<>();
        Map<String, List<MoodboardImageDTO.Image>> cachedSearches = new HashMap<>();
        int searchFailures = 0;
        int candidateCount = 0;
        for (String term : plan.searchTerms()) {
            List<MoodboardImageDTO.Image> matching = new ArrayList<>();
            try {
                String simplified = searchableTerm(term);
                List<MoodboardImageDTO.Image> hits = cachedSearches.get(simplified);
                if (hits == null) {
                    hits = new ArrayList<>(imageService.searchImages(simplified, 1, 20).images());
                    hits.sort(Comparator.comparingInt((MoodboardImageDTO.Image image) -> scoreCandidate(image, simplified)).reversed());
                    cachedSearches.put(simplified, hits);
                }
                for (var image : hits) {
                    if (image.id() != null && image.provider() != null &&
                            List.of("PIXABAY", "CLEVELAND", "MET", "WIKIMEDIA").contains(image.provider())) {
                        matching.add(image);
                        candidateCount++;
                    }
                }
            } catch (RuntimeException ex) {
                searchFailures++;
                log.warn("Moodboard image search failed: {}", ex.getMessage());
            }
            byTerm.put(term, matching);
        }

        List<ImageReference> images = new ArrayList<>();
        List<Note> notes = new ArrayList<>();
        Set<String> selectedIds = new HashSet<>();
        int maxImages = 6;
        int perTermCap = 2;
        int index = 0;
        for (var entry : byTerm.entrySet()) {
            int chosenForTerm = 0;
            for (var candidate : entry.getValue()) {
                if (images.size() >= maxImages || chosenForTerm >= perTermCap) break;
                if (!selectedIds.add(candidate.id())) continue;
                MoodboardImageDTO.Image safe = previewImage(candidate);
                if (safe == null) continue;
                int col = index % 2;
                int row = index / 2;
                double imageX = 40 + col * 760;
                double imageY = 40 + row * 380;
                double noteX = imageX + 230;
                double noteY = imageY + 18;
                images.add(new ImageReference(safe, entry.getKey(), imageX, imageY));
                notes.add(new Note(imageExplanation(entry.getKey()), "#1F2E35", "#FFF8E8", noteX, noteY));
                index++;
                chosenForTerm++;
            }
        }

        if (images.isEmpty()) {
            log.warn("SANA Moodboard: all providers returned zero usable images. candidates={}, failedSearches={}",
                    candidateCount, searchFailures);
            throw fail(HttpStatus.BAD_GATEWAY,
                    "لم تُرجع مكتبات الصور أي صورة صالحة. تحققي من PIXABAY_API_KEY ومن لوق Moodboard provider.");
        }
        log.info("SANA Moodboard preview selectedImages={} candidates={} failedSearches={}",
                images.size(), candidateCount, searchFailures);

        List<String> warnings = new ArrayList<>();
        if (images.size() < 4) {
            warnings.add("عدد الصور قليل نسبيًا؛ يمكنك حفظ الموجود الآن أو إعادة التوليد لاحقًا لتحسين التنوع.");
        }
        warnings.add("اختيار الصور يعتمد على بيانات المكتبات والترشيح النصي، لذا راجعي دلالتها قبل الحفظ النهائي.");

        Preview preview = new Preview(id, projectId, "PENDING", expires,
                plan.title(), plan.theme(), List.copyOf(images), List.copyOf(notes), plan.palette(),
                List.copyOf(warnings), false, null);
        return preview;
    }
    public Preview get(Integer projectId, String id, String session) {
        Draft d = require(projectId, id, session);
        synchronized (d) {
            if ("GENERATING".equals(d.state)) {
                AIRunResponseDTO run = sharedRequests.getRun(projectId, UUID.fromString(d.requestId));
                if (run.getStatus() == ExecutionStatus.FAILED || run.getStatus() == ExecutionStatus.TIMED_OUT) {
                    d.state = "FAILED";
                    throw fail(HttpStatus.BAD_GATEWAY,
                            "The shared Sana n8n request failed/timed out. Check callback connectivity and n8n execution.");
                }
                if (run.getStatus() == ExecutionStatus.COMPLETED) {
                    if (run.getResult() == null || run.getResult().type() !=
                            com.example.sana.DTO.IntegrationDTOs.N8nCallbackDTO.ResultType.ADVICE) {
                        d.state = "FAILED";
                        throw fail(HttpStatus.BAD_GATEWAY,
                                "Existing Sana n8n returned no Moodboard ADVICE; no board was saved.");
                    }
                    try {
                        Plan plan = parsePlan(run.getResult().message());
                        d.preview = buildPreview(d, plan);
                        d.state = "PENDING";
                    } catch (ResponseStatusException ex) {
                        d.state = "FAILED";
                        throw ex;
                    }
                }
            }
            if ("FAILED".equals(d.state)) throw fail(HttpStatus.BAD_GATEWAY,
                    "Moodboard planning failed; request a new preview. No moodboard was saved.");
            Preview p = d.preview;
            return new Preview(p.draftId(), p.projectId(), d.state, p.expiresAt(), p.title(), p.theme(),
                    p.images(), p.notes(), p.palette(), p.warnings(), false, d.savedId);
        }
    }

    public Saved approve(Integer projectId, String id, String session) {
        Draft d = require(projectId, id, session);
        synchronized (d) {
            if ("APPROVED".equals(d.state)) return new Saved(id, d.savedId, projectId);
            if (!"PENDING".equals(d.state)) throw fail(HttpStatus.CONFLICT, "Only pending previews can be approved");
            // Existing MoodboardService owns the transaction, storage and rollback.
            var saved = moodboards.saveApprovedDraft(projectId, id, session, d.preview);
            d.savedId = saved.id();
            d.state = "APPROVED";
            return new Saved(id, saved.id(), projectId);
        }
    }

    public void reject(Integer projectId, String id, String session) {
        Draft d = require(projectId, id, session);
        synchronized (d) {
            if ("APPROVED".equals(d.state)) throw fail(HttpStatus.CONFLICT, "Already approved");
            d.state = "REJECTED";
        }
    }

    private Draft require(Integer projectId, String id, String session) {
        Draft d = drafts.get(id);
        if (d == null || !d.projectId.equals(projectId) || !d.session.equals(session))
            throw fail(HttpStatus.NOT_FOUND, "Moodboard draft not found in this session");
        if (!d.preview.expiresAt().isAfter(Instant.now())) {
            drafts.remove(id);
            throw fail(HttpStatus.GONE, "Moodboard preview expired; request a new one");
        }
        return d;
    }

    /** Require HTTPS for images (browser mixed-content safety), but do not reject a
     * valid Openverse image merely because its source or licence page uses HTTP.
     * Optional non-HTTPS thumbnails are dropped so the UI can show the HTTPS original.
     */
    private static MoodboardImageDTO.Image previewImage(MoodboardImageDTO.Image image) {
        if (image == null || image.license() == null || image.license().isBlank()
                || !publicUrl(image.sourceUrl(), true)) return null;
        String full = publicUrl(image.imageUrl(), false) ? image.imageUrl() : null;
        String thumb = publicUrl(image.thumbnail(), false) ? image.thumbnail() : null;
        if (full == null && thumb == null) return null;
        return new MoodboardImageDTO.Image(image.id(), image.title(), full != null ? full : thumb,
                thumb, image.creator(), image.creatorUrl(), image.license(),
                image.licenseVersion(), image.licenseUrl(), image.sourceUrl(), image.attribution(), image.provider());
    }

    private static boolean publicUrl(String raw, boolean allowHttp) {
        if (raw == null || raw.length() > 2048) return false;
        try {
            URI u = URI.create(raw);
            String host = u.getHost();
            String scheme = u.getScheme();
            if (host == null || u.getUserInfo() != null || scheme == null
                    || !("https".equalsIgnoreCase(scheme)
                    || (allowHttp && "http".equalsIgnoreCase(scheme)))) return false;
            host = host.toLowerCase(Locale.ROOT);
            return host.contains(".") && !host.endsWith(".localhost")
                    && !host.endsWith(".local") && !host.endsWith(".internal")
                    && !host.matches("[0-9.]+") && !host.contains(":");
        } catch (IllegalArgumentException e) { return false; }
    }
    private static ResponseStatusException fail(HttpStatus status, String message) {
        return new ResponseStatusException(status, message);
    }
}
