package com.example.sana.Service.DiagramServices;

import com.example.sana.API.ApiException;
import com.example.sana.DTO.GraphDefinitionDTO;
import com.example.sana.Enums.RepresentationType;
import com.example.sana.Model.ProjectEdge;
import com.example.sana.Model.ProjectNode;
import com.example.sana.Model.ProjectOutput;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.DeserializationFeature;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Set;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class GraphMapper {

    private final JsonMapper jsonMapper;

    // Convert any GRAPH output of any domain into the shared graph format.
    // The caller loads the graph inside its read transaction. This mapper performs no writes.
    public GraphDefinitionDTO toGraphDTO(ProjectOutput output) {

        if (output == null || output.getRepresentationType() != RepresentationType.GRAPH) {
            throw new ApiException("Output must be a graph diagram");
        }

        requireId(output.getId(), "Diagram");
        if (output.getTitle() == null || output.getTitle().isBlank() || output.getTitle().length() > 255) {
            throw new ApiException("Diagram title is invalid");
        }
        if (output.getNodes() == null || output.getEdges() == null) {
            throw new ApiException("Graph collections must be loaded");
        }

        var nodes = new ArrayList<GraphDefinitionDTO.NodeDTO>();
        Set<Integer> nodeIds = new HashSet<>();
        for (ProjectNode node : output.getNodes()) {
            if (node == null) throw new ApiException("Graph contains a missing node");
            requireId(node.getId(), "Node");
            requireOutput(node.getProjectOutput(), output.getId());
            requireText(node.getType(), "Node type", 50);
            if (!nodeIds.add(node.getId())) throw new ApiException("Graph contains duplicate node IDs");
            if (node.getPositionX() == null || node.getPositionY() == null
                    || !Double.isFinite(node.getPositionX()) || !Double.isFinite(node.getPositionY())) {
                throw new ApiException("Node positions must be finite numbers");
            }
            nodes.add(new GraphDefinitionDTO.NodeDTO("node:" + node.getId(), node.getId(), node.getType(),
                    readProperties(node.getDataJson()), node.getPositionX(), node.getPositionY()));
        }

        var edges = new ArrayList<GraphDefinitionDTO.EdgeDTO>();
        Set<Integer> edgeIds = new HashSet<>();
        for (ProjectEdge edge : output.getEdges()) {
            if (edge == null) throw new ApiException("Graph contains a missing edge");
            requireId(edge.getId(), "Edge");
            requireOutput(edge.getProjectOutput(), output.getId());
            requireText(edge.getType(), "Edge type", 50);
            requireText(edge.getLabel(), "Edge label", 50);
            if (!edgeIds.add(edge.getId())) throw new ApiException("Graph contains duplicate edge IDs");
            requireEndpoint(edge.getSourceNode(), nodeIds, output.getId());
            requireEndpoint(edge.getTargetNode(), nodeIds, output.getId());
            edges.add(new GraphDefinitionDTO.EdgeDTO("edge:" + edge.getId(), edge.getId(), edge.getType(),
                    edge.getLabel(), "node:" + edge.getSourceNode().getId(), "node:" + edge.getTargetNode().getId(),
                    readProperties(edge.getDataJson())));
        }

        // Stable ordering keeps snapshots and later computed differences predictable.
        nodes.sort(Comparator.comparing(GraphDefinitionDTO.NodeDTO::getId));
        edges.sort(Comparator.comparing(GraphDefinitionDTO.EdgeDTO::getId));
        Map<String, Object> properties = readProperties(output.getPropertiesJson());
        properties.put("title", output.getTitle());
        return new GraphDefinitionDTO(nodes, edges, properties);
    }

    private void requireEndpoint(ProjectNode node, Set<Integer> nodeIds, Integer outputId) {
        if (node == null || !nodeIds.contains(node.getId())) {
            throw new ApiException("Edge endpoint must reference a node in the diagram");
        }
        requireOutput(node.getProjectOutput(), outputId);
    }

    private void requireOutput(ProjectOutput actual, Integer expectedId) {
        if (actual == null || !expectedId.equals(actual.getId())) {
            throw new ApiException("Graph element belongs to another output");
        }
    }

    private void requireId(Integer id, String element) {
        if (id == null || id <= 0) throw new ApiException(element + " must have a saved positive ID");
    }

    private void requireText(String value, String field, int maxLength) {
        if (value == null || value.isBlank() || value.length() > maxLength) {
            throw new ApiException(field + " is invalid");
        }
    }

    private Map<String, Object> readProperties(String dataJson) {
        // Nullable database metadata is represented as an empty JSON object.
        if (dataJson == null) return new LinkedHashMap<>();
        try {
            Map<String, Object> data = jsonMapper.readerFor(Map.class)
                    .with(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS, DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
                    .readValue(dataJson);
            if (data == null) {
                throw new ApiException("Graph properties must be a JSON object");
            }
            return data;
        } catch (JacksonException e) {
            throw new ApiException("Graph properties contain invalid JSON");
        }
    }
}
