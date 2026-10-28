package com.example.sana.Service.DiagramServices;

import com.example.sana.API.ApiConflictException;
import com.example.sana.DTO.GraphDefinitionDTO;
import com.example.sana.Model.ProjectEdge;
import com.example.sana.Model.ProjectNode;
import com.example.sana.Model.ProjectOutput;
import com.example.sana.Repository.ProjectEdgeRepository;
import com.example.sana.Repository.ProjectNodeRepository;
import com.example.sana.Service.IntegrationServices.AIJson;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

// Replaces a diagram's nodes and edges with an accepted full graph.
@Service
@RequiredArgsConstructor
public class GraphApplier {

    // Plain diagrams need no extra steps
    private static final GraphApplyHooks NO_HOOKS = new GraphApplyHooks() {
    };

    private final ProjectNodeRepository nodes;
    private final ProjectEdgeRepository edges;
    private final AIJson json;

    // Caller owns the output lock and transaction and has validated this entire graph.
    public Map<String, Integer> apply(ProjectOutput output, GraphDefinitionDTO graph) {
        return apply(output, graph, NO_HOOKS);
    }

    // Same as apply, with extra steps for diagram types that store more than nodes and edges.
    // Returns the saved ID of every graph key, so new elements can be found by their temporary keys.
    public Map<String, Integer> apply(ProjectOutput output, GraphDefinitionDTO graph, GraphApplyHooks hooks) {

        hooks.beforeApply(output, graph);

        Map<Integer, ProjectNode> oldNodes = output.getNodes().stream().collect(Collectors.toMap(ProjectNode::getId, node -> node));
        Map<Integer, ProjectEdge> oldEdges = output.getEdges().stream().collect(Collectors.toMap(ProjectEdge::getId, edge -> edge));

        // 1. Delete edges that are not in the new graph
        Set<Integer> retainedEdges = graph.getEdges().stream()
                .map(GraphDefinitionDTO.EdgeDTO::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        for (ProjectEdge edge : new ArrayList<>(output.getEdges())) {
            if (!retainedEdges.contains(edge.getId())) {
                hooks.beforeEdgeDelete(edge);
                detach(output, edge);
                edges.delete(edge);
            }
        }

        // 2. Create or update every node of the new graph
        Map<String, ProjectNode> byKey = new HashMap<>();
        Map<String, Integer> ids = new LinkedHashMap<>();
        Set<Integer> retainedNodes = new HashSet<>();

        for (GraphDefinitionDTO.NodeDTO dto : graph.getNodes()) {

            ProjectNode node = dto.getId() == null ? new ProjectNode() : oldNodes.get(dto.getId());
            if (node == null) {
                throw new ApiConflictException("A retained node is no longer present");
            }

            if (node.getId() == null) {
                node.setCreatedAt(LocalDate.now());
                node.setProjectOutput(output);
            }
            node.setType(hooks.storedNodeType(dto));
            node.setDataJson(json.write(hooks.storedNodeProperties(dto)));
            node.setPositionX(dto.getPositionX());
            node.setPositionY(dto.getPositionY());
            node.setUpdatedAt(LocalDate.now());
            if (node.getOutgoingEdges() == null) {
                node.setOutgoingEdges(new HashSet<>());
            }
            if (node.getIncomingEdges() == null) {
                node.setIncomingEdges(new HashSet<>());
            }

            nodes.save(node);
            output.getNodes().add(node);
            retainedNodes.add(node.getId());
            byKey.put(dto.getKey(), node);
            ids.put(dto.getKey(), node.getId());

            hooks.afterNodeSaved(node, dto);
        }

        // 3. Create or update every edge of the new graph
        for (GraphDefinitionDTO.EdgeDTO dto : graph.getEdges()) {

            ProjectEdge edge = dto.getId() == null ? new ProjectEdge() : oldEdges.get(dto.getId());
            if (edge == null) {
                throw new ApiConflictException("A retained edge is no longer present");
            }

            if (edge.getId() == null) {
                edge.setCreatedAt(LocalDate.now());
                edge.setProjectOutput(output);
            } else {
                detach(output, edge);
            }
            edge.setSourceNode(byKey.get(dto.getSourceKey()));
            edge.setTargetNode(byKey.get(dto.getTargetKey()));
            edge.setType(dto.getType());
            edge.setLabel(dto.getLabel());
            edge.setDataJson(json.write(dto.getProperties()));

            edges.save(edge);
            output.getEdges().add(edge);
            edge.getSourceNode().getOutgoingEdges().add(edge);
            edge.getTargetNode().getIncomingEdges().add(edge);
            ids.put(dto.getKey(), edge.getId());

            hooks.afterEdgeSaved(edge, dto);
        }

        hooks.afterElementsSaved(output, byKey, graph);

        // 4. Delete nodes that are not in the new graph
        for (ProjectNode node : new ArrayList<>(output.getNodes())) {
            if (!retainedNodes.contains(node.getId())) {
                hooks.beforeNodeDelete(node);
                output.getNodes().remove(node);
                nodes.delete(node);
            }
        }

        // 5. Save diagram properties and advance the revision
        Map<String, Object> properties = new LinkedHashMap<>(graph.getProperties());
        output.setTitle((String) properties.remove("title"));
        output.setPropertiesJson(json.write(properties));

        if (output.getGraphRevision() == Integer.MAX_VALUE) {
            throw new ApiConflictException("Graph revision limit reached");
        }
        output.setGraphRevision(output.getGraphRevision() + 1);
        output.setUpdatedAt(LocalDateTime.now());

        hooks.afterApply(output, graph);

        return ids;
    }

    private void detach(ProjectOutput output, ProjectEdge edge) {

        output.getEdges().remove(edge);

        if (edge.getSourceNode().getOutgoingEdges() != null) {
            edge.getSourceNode().getOutgoingEdges().remove(edge);
        }
        if (edge.getTargetNode().getIncomingEdges() != null) {
            edge.getTargetNode().getIncomingEdges().remove(edge);
        }
    }
}
