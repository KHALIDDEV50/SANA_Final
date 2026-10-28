package com.example.sana.Service.DiagramServices;

import com.example.sana.DTO.GraphDefinitionDTO;
import com.example.sana.Model.Project;
import com.example.sana.Model.ProjectOutput;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

// Reads and saves full graphs for every diagram type.
// Plain diagrams use GraphMapper and GraphApplier; Creativity diagrams use their DiagramGraphAdapter.
@Service
@RequiredArgsConstructor
public class DiagramGraphService {

    private final List<DiagramGraphAdapter> adapters;
    private final DiagramRulesRegistry diagramRulesRegistry;
    private final GraphMapper graphMapper;
    private final GraphApplier graphApplier;

    // Every diagram type is either protected by DiagramWriteGuard or edited through its own adapter, never both
    @PostConstruct
    public void checkDiagramTypes() {

        for (DiagramRules rules : diagramRulesRegistry.getAllRules()) {

            boolean guarded = DiagramWriteGuard.MANAGED_DIAGRAM_TYPES.contains(rules.getDiagramType());
            boolean adapted = findAdapter(rules.getDiagramType()) != null;

            if (guarded == adapted) {
                throw new IllegalStateException(rules.getDiagramType()
                        + " must be either listed in DiagramWriteGuard.MANAGED_DIAGRAM_TYPES or have a DiagramGraphAdapter");
            }
        }
    }

    // True when the diagram keeps domain data and is edited through its domain endpoints
    public boolean isDomainEdited(String diagramType) {
        return findAdapter(diagramType) != null;
    }

    // Read the full graph of a diagram
    public GraphDefinitionDTO read(ProjectOutput output) {

        DiagramGraphAdapter adapter = findAdapter(output.getOutputType());

        if (adapter != null) {
            return adapter.read(output);
        }

        return graphMapper.toGraphDTO(output);
    }

    // Save an accepted, validated graph; the caller holds the output lock
    public Map<String, Integer> apply(ProjectOutput output, GraphDefinitionDTO graph) {

        DiagramGraphAdapter adapter = findAdapter(output.getOutputType());

        if (adapter != null) {
            return adapter.apply(output, graph);
        }

        return graphApplier.apply(output, graph);
    }

    // Extra facts the AI agent needs for this diagram type
    public Map<String, Object> extraContext(Project project, String diagramType) {

        DiagramGraphAdapter adapter = findAdapter(diagramType);

        if (adapter != null) {
            return adapter.extraContext(project);
        }

        return Map.of();
    }

    private DiagramGraphAdapter findAdapter(String diagramType) {

        for (DiagramGraphAdapter adapter : adapters) {
            if (adapter.getDiagramType().equals(diagramType)) {
                return adapter;
            }
        }

        return null;
    }
}
