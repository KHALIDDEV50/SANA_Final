package com.example.sana.Service.DiagramServices;

import com.example.sana.API.ApiConflictException;
import com.example.sana.DTO.GraphDefinitionDTO;
import com.example.sana.Model.ProjectEdge;
import com.example.sana.Model.ProjectNode;
import com.example.sana.Model.ProjectOutput;

import java.util.Map;

// Optional steps a diagram type can add while GraphApplier saves an accepted graph.
// Plain diagrams use the defaults; Creativity uses them to keep its entity rows in sync.
public interface GraphApplyHooks {

    // Runs first, before any node or edge changes
    default void beforeApply(ProjectOutput output, GraphDefinitionDTO graph) {
    }

    // The type saved on ProjectNode for a graph node
    default String storedNodeType(GraphDefinitionDTO.NodeDTO node) {
        return node.getType();
    }

    // The properties saved in ProjectNode.dataJson
    default Map<String, Object> storedNodeProperties(GraphDefinitionDTO.NodeDTO node) {
        return node.getProperties();
    }

    // Runs after a node of the new graph is saved
    default void afterNodeSaved(ProjectNode node, GraphDefinitionDTO.NodeDTO dto) {
    }

    // Runs after an edge of the new graph is saved
    default void afterEdgeSaved(ProjectEdge edge, GraphDefinitionDTO.EdgeDTO dto) {
    }

    // Runs when every node and edge of the new graph is saved, before removed nodes are deleted
    default void afterElementsSaved(ProjectOutput output, Map<String, ProjectNode> nodesByKey, GraphDefinitionDTO graph) {
    }

    // Runs before an edge that is not in the new graph is deleted
    default void beforeEdgeDelete(ProjectEdge edge) {
    }

    // Runs before a node that is not in the new graph is deleted
    default void beforeNodeDelete(ProjectNode node) {

        if (node.getCharacterProfile() != null || node.getWorld() != null || node.getLocation() != null
                || node.getWorldRule() != null || node.getStoryEvent() != null) {
            throw new ApiConflictException("Diagram contains domain-specific data requiring manual cleanup");
        }
    }

    // Runs last, after the output's revision is advanced
    default void afterApply(ProjectOutput output, GraphDefinitionDTO graph) {
    }
}
