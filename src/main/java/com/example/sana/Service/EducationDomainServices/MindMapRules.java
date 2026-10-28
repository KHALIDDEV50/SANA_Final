package com.example.sana.Service.EducationDomainServices;

import com.example.sana.API.ApiException;
import com.example.sana.DTO.GraphDefinitionDTO;
import com.example.sana.Enums.EducationDomainEnums.EducationOutputType;
import com.example.sana.Service.DiagramServices.BasicDiagramRules;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

// Rules for the EDUCATION Mind Map: one central topic with a tree of topics, subtopics and notes.
@Component
public class MindMapRules extends BasicDiagramRules {

    public MindMapRules() {
        super("EDUCATION",
                EducationOutputType.MIND_MAP.name(),
                List.of("CENTRAL_TOPIC", "TOPIC", "SUBTOPIC", "NOTE"),
                List.of("BRANCH", "RELATED"),
                "A mind map has one CENTRAL_TOPIC. TOPIC nodes branch from the central topic, SUBTOPIC nodes branch from topics or subtopics, "
                        + "and NOTE nodes hold short explanations or examples. BRANCH edges go from parent to child, and every node has at most one parent. "
                        + "RELATED edges link any two ideas that are connected. Node properties.label is required; properties.description is optional. "
                        + "New IDs must be null.");
    }

    // Nothing can branch into the central topic because it is the root
    @Override
    public void validateEdge(String type, String label, String sourceType, String targetType, Map<String, Object> properties) {

        super.validateEdge(type, label, sourceType, targetType, properties);

        if ("BRANCH".equals(type) && "CENTRAL_TOPIC".equals(targetType)) {
            throw new ApiException("The central topic cannot be the child of a BRANCH edge");
        }
    }

    // Keep the map a tree: one central topic and at most one parent per node
    @Override
    public void validateGraph(GraphDefinitionDTO graph) {

        long centralTopics = graph.getNodes().stream()
                .filter(node -> "CENTRAL_TOPIC".equals(node.getType()))
                .count();

        if (centralTopics > 1) {
            throw new ApiException("A mind map can have only one CENTRAL_TOPIC");
        }

        Set<String> childrenWithParent = new HashSet<>();

        for (GraphDefinitionDTO.EdgeDTO edge : graph.getEdges()) {
            if ("BRANCH".equals(edge.getType()) && !childrenWithParent.add(edge.getTargetKey())) {
                throw new ApiException("Each mind map node can have only one parent BRANCH");
            }
        }
    }
}
