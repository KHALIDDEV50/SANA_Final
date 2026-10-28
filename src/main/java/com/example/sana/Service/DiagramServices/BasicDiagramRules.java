package com.example.sana.Service.DiagramServices;

import com.example.sana.API.ApiException;

import java.util.List;
import java.util.Map;

// Rules shared by most diagrams: known node/edge types, a node label, optional descriptions and valid positions.
// Diagrams with extra rules extend this class and override the methods they need.
public class BasicDiagramRules implements DiagramRules {

    private final String domain;
    private final String diagramType;
    private final List<String> nodeTypes;
    private final List<String> edgeTypes;
    private final String rulesSummary;

    public BasicDiagramRules(String domain, String diagramType, List<String> nodeTypes, List<String> edgeTypes, String rulesSummary) {
        this.domain = domain;
        this.diagramType = diagramType;
        this.nodeTypes = List.copyOf(nodeTypes);
        this.edgeTypes = List.copyOf(edgeTypes);
        this.rulesSummary = rulesSummary;
    }

    @Override
    public String getDomain() {
        return domain;
    }

    @Override
    public String getDiagramType() {
        return diagramType;
    }

    @Override
    public List<String> getNodeTypes() {
        return nodeTypes;
    }

    @Override
    public List<String> getEdgeTypes() {
        return edgeTypes;
    }

    @Override
    public String getRulesSummary() {
        return rulesSummary;
    }

    // Check one node: known type, a label, an optional description and a valid position
    @Override
    public void validateNode(String type, Map<String, Object> properties, Double positionX, Double positionY) {

        if (type == null || !nodeTypes.contains(type)) {
            throw new ApiException("Unsupported node type for " + diagramType + ". Allowed values: " + nodeTypes);
        }

        if (properties == null) {
            throw new ApiException("Node properties are required");
        }

        GraphRuleChecks.requireText(properties.get("label"), 255, "Node label");
        GraphRuleChecks.optionalText(properties, "description", 20000);
        GraphRuleChecks.requirePosition(positionX, positionY);
    }

    // Check one edge: known type, a short label and an optional description
    @Override
    public void validateEdge(String type, String label, String sourceType, String targetType, Map<String, Object> properties) {

        if (type == null || !edgeTypes.contains(type)) {
            throw new ApiException("Unsupported edge type for " + diagramType + ". Allowed values: " + edgeTypes);
        }

        GraphRuleChecks.requireText(label, 50, "Edge label");

        if (properties == null) {
            throw new ApiException("Edge properties are required");
        }

        GraphRuleChecks.optionalText(properties, "label", 50);
        GraphRuleChecks.optionalText(properties, "description", 20000);
    }
}
