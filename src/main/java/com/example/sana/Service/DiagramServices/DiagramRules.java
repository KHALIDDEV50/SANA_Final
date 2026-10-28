package com.example.sana.Service.DiagramServices;

import com.example.sana.DTO.GraphDefinitionDTO;

import java.util.List;
import java.util.Map;

// Rules for one diagram type that users and the AI can create and edit.
// Every domain adds one implementation per diagram type, and DiagramRulesRegistry finds it.
public interface DiagramRules {

    // Project domain this diagram belongs to, for example TECH
    String getDomain();

    // Output type stored on ProjectOutput, for example ARCHITECTURE
    String getDiagramType();

    // Node types allowed in this diagram
    List<String> getNodeTypes();

    // Edge types allowed in this diagram
    List<String> getEdgeTypes();

    // Short rules text sent to the n8n agent inside the request context
    String getRulesSummary();

    // Throws ApiException when the node is not valid for this diagram
    void validateNode(String type, Map<String, Object> properties, Double positionX, Double positionY);

    // Throws ApiException when the edge is not valid for this diagram
    void validateEdge(String type, String label, String sourceType, String targetType, Map<String, Object> properties);

    // Throws ApiException when the whole graph breaks a rule, for example "only one central topic"
    default void validateGraph(GraphDefinitionDTO graph) {
    }
}
