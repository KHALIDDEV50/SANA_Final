package com.example.sana.Service.DiagramServices;

import com.example.sana.API.ApiException;
import com.example.sana.DTO.GraphDefinitionDTO;
import com.example.sana.Service.IntegrationServices.AIJson;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.*;

@Service
@RequiredArgsConstructor
public class GraphValidator {
    private final AIJson json;

    // Check a full proposed graph against the diagram rules and the request snapshot (baseline is null for CREATE)
    public GraphDefinitionDTO validate(GraphDefinitionDTO candidate, GraphDefinitionDTO baseline, String defaultTitle, DiagramRules rules) {
        if (candidate == null || candidate.getNodes() == null || candidate.getEdges() == null || candidate.getProperties() == null)
            throw new ApiException("A complete proposedGraph is required");
        if (candidate.getNodes().size() > 200 || candidate.getEdges().size() > 400) throw new ApiException("Graph exceeds 200 nodes or 400 edges");
        candidate.setProperties(new LinkedHashMap<>(candidate.getProperties()));
        if (!candidate.getProperties().containsKey("title")) candidate.getProperties().put("title", defaultTitle);
        GraphRuleChecks.requireText(candidate.getProperties().get("title"), 255, "Diagram title");
        properties(candidate.getProperties(), 0);
        Map<Integer, String> oldNodes = new HashMap<>(); Map<Integer, String> oldEdges = new HashMap<>();
        Set<String> oldKeys = new HashSet<>();
        if (baseline != null) {
            baseline.getNodes().forEach(node -> {oldNodes.put(node.getId(), node.getKey()); oldKeys.add(node.getKey());});
            baseline.getEdges().forEach(edge -> {oldEdges.put(edge.getId(), edge.getKey()); oldKeys.add(edge.getKey());});
        }
        Set<String> keys = new HashSet<>(); Set<Integer> nodeIds = new HashSet<>(); Set<Integer> edgeIds = new HashSet<>();
        Map<String, GraphDefinitionDTO.NodeDTO> nodes = new HashMap<>();
        for (var node : candidate.getNodes()) {
            if (node == null) throw new ApiException("Null graph node");
            identity(node.getKey(), node.getId(), oldNodes, oldKeys, keys, nodeIds);
            // Name the element in the error so a rejected AI proposal or manual edit is easy to trace
            try {
                rules.validateNode(node.getType(), node.getProperties(), node.getPositionX(), node.getPositionY());
            } catch (ApiException e) {
                throw new ApiException(node.getKey() + ": " + e.getMessage());
            }
            properties(node.getProperties(), 0); nodes.put(node.getKey(), node);
        }
        for (var edge : candidate.getEdges()) {
            if (edge == null) throw new ApiException("Null graph edge");
            identity(edge.getKey(), edge.getId(), oldEdges, oldKeys, keys, edgeIds);
            var source = nodes.get(edge.getSourceKey()); var target = nodes.get(edge.getTargetKey());
            if (source == null || target == null || source == target) throw new ApiException("Edge endpoints must reference two distinct nodes in proposedGraph");
            if (edge.getProperties() == null) throw new ApiException("Edge properties are required");
            if (edge.getLabel() == null) edge.setLabel(edge.getProperties().get("label") instanceof String label ? label : edge.getType());
            try {
                rules.validateEdge(edge.getType(), edge.getLabel(), source.getType(), target.getType(), edge.getProperties());
            } catch (ApiException e) {
                throw new ApiException(edge.getKey() + ": " + e.getMessage());
            }
            properties(edge.getProperties(), 0);
        }
        // Rules that look at the whole graph, for example "only one central topic"
        rules.validateGraph(candidate);

        if (json.write(candidate).getBytes(StandardCharsets.UTF_8).length > 1048576) throw new ApiException("Graph exceeds 1 MiB");
        return candidate;
    }

    private void identity(String key, Integer id, Map<Integer, String> persisted, Set<String> oldKeys, Set<String> keys, Set<Integer> ids) {
        if (key == null || !key.matches("[A-Za-z][A-Za-z0-9:_-]{0,99}") || !keys.add(key)) throw new ApiException("Graph keys must be unique valid identifiers");
        if (id != null) {
            if (id <= 0 || !ids.add(id) || !key.equals(persisted.get(id))) throw new ApiException("Existing graph IDs and keys must match the request snapshot");
        } else if (oldKeys.contains(key) || key.matches("(node|edge):[0-9]+")) {
            throw new ApiException("New graph elements require unique temporary keys and null IDs");
        }
    }

    private void properties(Object value, int depth) {
        if (depth > 20) throw new ApiException("Graph properties exceed maximum nesting depth");
        if (value instanceof Map<?, ?> map) {
            if (map.size() > 200) throw new ApiException("Too many graph properties");
            for (var entry : map.entrySet()) {
                if (!(entry.getKey() instanceof String key) || key.isBlank() || key.length() > 100) throw new ApiException("Invalid graph property name");
                properties(entry.getValue(), depth + 1);
            }
        } else if (value instanceof Collection<?> list) {
            if (list.size() > 500) throw new ApiException("Graph property array is too large");
            list.forEach(item -> properties(item, depth + 1));
        } else if (value instanceof String text) {
            if (text.length() > 20000) throw new ApiException("Graph property text is too long");
        } else if (value instanceof Double number && !Double.isFinite(number)
                || value instanceof Float floatNumber && !Float.isFinite(floatNumber)) {
            throw new ApiException("Graph properties must contain finite numbers");
        } else if (value != null && !(value instanceof Number || value instanceof Boolean)) throw new ApiException("Graph properties must be JSON values");
    }
}
