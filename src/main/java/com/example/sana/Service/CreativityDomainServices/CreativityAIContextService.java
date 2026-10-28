package com.example.sana.Service.CreativityDomainServices;

import com.example.sana.DTO.CreativityDomainDTOs.MoodboardDTO;
import com.example.sana.DTO.CreativityDomainDTOs.PlayableCharacterResponseDTO;
import com.example.sana.DTO.GraphDefinitionDTO;
import com.example.sana.Model.Project;
import com.example.sana.Model.ProjectOutput;
import com.example.sana.Repository.ProjectOutputRepository;
import com.example.sana.Service.DiagramServices.DiagramGraphService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Read-only, bounded, Creativity-only context for n8n. The selected graph remains authoritative.
 * Other outputs are references only: suggestions must NEVER edit them as part of a single-output proposal.
 */
@Service
@RequiredArgsConstructor
public class CreativityAIContextService {
    private final ProjectOutputRepository outputRepository;
    private final DiagramGraphService graphService;
    private final MoodboardService moodboardService;
    private final CharacterBalanceService balanceService;

    @Transactional(readOnly = true)
    public Map<String, Object> build(Project project, Integer activeOutputId, List<String> selectedElementIds) {
        Map<String, Object> context = new LinkedHashMap<>();
        context.put("scope", "PROJECT_REFERENCES_READ_ONLY");
        context.put("referenceInstruction", "Other outputs, game stats and moodboards are references, not editable target graphs. Only currentGraph may be proposed for change. Images are metadata, not visually analyzed.");
        context.put("selectedElementIds", selectedElementIds == null ? List.of() : selectedElementIds);
        List<Map<String, Object>> references = new ArrayList<>();
        List<Map<String, Object>> playable = new ArrayList<>();
        List<ProjectOutput> outputs = outputRepository.findAllByProject_Id(project.getId()).stream()
                .sorted(Comparator.comparing(ProjectOutput::getId)).toList();
        int omittedOutputs = 0;
        for (ProjectOutput output : outputs) {
            if (!"STORY_MAP".equals(output.getOutputType()) && !"BRANCHING_STORY".equals(output.getOutputType())) continue;
            if ("STORY_MAP".equals(output.getOutputType()) && playable.size() < 40) {
                // CharacterBalanceService already uses bounded, project-output-scoped repository queries.
                for (PlayableCharacterResponseDTO p : balanceService.getAllCharacters(output.getId())) {
                    if (playable.size() == 40) break;
                    Map<String, Object> stats = new LinkedHashMap<>();
                    stats.put("outputId", output.getId()); stats.put("id", p.id());
                    stats.put("name", p.name()); stats.put("role", p.role());
                    stats.put("storyCharacterId", p.characterProfileId());
                    stats.put("attack", p.attack()); stats.put("defense", p.defense());
                    stats.put("speed", p.speed()); stats.put("range", p.rangeStat());
                    stats.put("stamina", p.stamina()); stats.put("special", p.special());
                    playable.add(stats);
                }
            }
            if (output.getId().equals(activeOutputId)) continue;
            if (references.size() >= 8) { omittedOutputs++; continue; }
            GraphDefinitionDTO graph = graphService.read(output);
            Map<String, Object> reference = new LinkedHashMap<>();
            reference.put("outputId", output.getId());
            reference.put("title", output.getTitle());
            reference.put("diagramType", output.getOutputType());
            reference.put("nodes", graph.getNodes().stream().limit(55).map(n -> {
                Map<String,Object> node = new LinkedHashMap<>();
                node.put("id", n.getId()); node.put("key", n.getKey());
                node.put("type", n.getType()); node.put("properties", n.getProperties());
                return node;
            }).toList());
            reference.put("edges", graph.getEdges().stream().limit(75).map(e -> {
                Map<String,Object> edge = new LinkedHashMap<>();
                edge.put("type", e.getType()); edge.put("label", e.getLabel());
                edge.put("sourceKey", e.getSourceKey()); edge.put("targetKey", e.getTargetKey());
                edge.put("properties", e.getProperties());
                return edge;
            }).toList());
            reference.put("graphProperties", graph.getProperties());
            reference.put("truncated", graph.getNodes().size() > 55 || graph.getEdges().size() > 75);
            references.add(reference);
        }
        context.put("relatedOutputs", references);
        context.put("omittedRelatedOutputCount", omittedOutputs);
        context.put("playableCharacters", playable);
        context.putAll(moodboardReferences(project.getId()));
        return context;
    }
    /** Saved moodboard notes and image references: never editable graph targets or visually inspected images. */
    @Transactional(readOnly = true)
    public Map<String, Object> moodboardReferences(Integer projectId) {
        var boards = moodboardService.getProjectBoards(projectId);
        List<Map<String, Object>> entries = new ArrayList<>();
        for (var summary : boards.stream().limit(6).toList()) {
            MoodboardDTO.Board board = moodboardService.getBoard(summary.id());
            List<Map<String, Object>> items = new ArrayList<>();
            for (var item : board.items().stream().limit(35).toList()) {
                Map<String, Object> ref = new LinkedHashMap<>();
                ref.put("id", item.id());
                ref.put("type", item.type());
                if (item.label() != null) {
                    ref.put("text", shorten(item.label().text(), 1000));
                    ref.put("color", shorten(item.label().color(), 7));
                    ref.put("backgroundColor", shorten(item.label().backgroundColor(), 7));
                }
                if (item.image() != null) {
                    var image = item.image();
                    ref.put("provider", image.provider() == null ? "OPENVERSE" : image.provider());
                    ref.put("imageId", shorten(image.id(), 160));
                    ref.put("imageTitle", shorten(image.title(), 300));
                    ref.put("creator", shorten(image.creator(), 200));
                    ref.put("imageUrl", shorten(image.imageUrl(), 2048));
                    ref.put("sourceUrl", shorten(image.sourceUrl(), 2048));
                    ref.put("license", shorten(image.license(), 80));
                    ref.put("licenseVersion", shorten(image.licenseVersion(), 30));
                    ref.put("licenseUrl", shorten(image.licenseUrl(), 2048));
                    ref.put("attribution", shorten(image.attribution(), 600));
                }
                items.add(ref);
            }
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("outputId", board.id());
            entry.put("title", board.title());
            entry.put("items", items);
            entry.put("truncated", board.items().size() > 35);
            entries.add(entry);
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("moodboards", entries);
        result.put("omittedMoodboardCount", Math.max(0, boards.size() - 6));
        result.put("moodboardReferenceInstruction", "Saved moodboards are read-only references. Image metadata is not visual image analysis.");
        return result;
    }

    private static String shorten(String s, int max) {
        return s == null ? "" : s.substring(0, Math.min(s.length(), max));
    }

}
