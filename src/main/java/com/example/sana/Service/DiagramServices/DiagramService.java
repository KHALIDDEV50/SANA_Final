package com.example.sana.Service.DiagramServices;

import com.example.sana.API.ApiConflictException;
import com.example.sana.API.ApiException;
import com.example.sana.DTO.DiagramDTOs.DiagramEdgeRequestDTO;
import com.example.sana.DTO.DiagramDTOs.DiagramGraphResponseDTO;
import com.example.sana.DTO.DiagramDTOs.DiagramNodeRequestDTO;
import com.example.sana.DTO.DiagramDTOs.DiagramRequestDTO;
import com.example.sana.DTO.DiagramDTOs.DiagramTypeDTO;
import com.example.sana.DTO.GraphDefinitionDTO;
import com.example.sana.Enums.RepresentationType;
import com.example.sana.Model.Project;
import com.example.sana.Model.ProjectEdge;
import com.example.sana.Model.ProjectNode;
import com.example.sana.Model.ProjectOutput;
import com.example.sana.Repository.ProjectEdgeRepository;
import com.example.sana.Repository.ProjectNodeRepository;
import com.example.sana.Repository.ProjectOutputRepository;
import com.example.sana.Repository.ProjectRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

// Manual editing for every diagram type that has DiagramRules (Architecture, Mind Map, Business diagrams).
// Every write locks the output, checks expectedRevision, re-validates the whole graph and advances graphRevision.
// requiredType limits a call to one diagram type (used by the Architecture endpoints); null accepts any type.
@Service
@RequiredArgsConstructor
public class DiagramService {

    private final ProjectRepository projectRepository;
    private final ProjectOutputRepository projectOutputRepository;
    private final ProjectNodeRepository projectNodeRepository;
    private final ProjectEdgeRepository projectEdgeRepository;
    private final DiagramRulesRegistry diagramRulesRegistry;
    private final GraphMapper graphMapper;
    private final GraphValidator graphValidator;
    private final DiagramGraphService diagramGraphService;
    private final JsonMapper jsonMapper;

    // List every supported diagram type with its node and edge types
    public List<DiagramTypeDTO> getDiagramTypes() {

        List<DiagramTypeDTO> types = new ArrayList<>();

        for (DiagramRules rules : diagramRulesRegistry.getAllRules()) {
            types.add(new DiagramTypeDTO(rules.getDomain(), rules.getDiagramType(),
                    rules.getNodeTypes(), rules.getEdgeTypes(), rules.getRulesSummary()));
        }

        return types;
    }

    // Get all diagrams of one type across all projects
    @Transactional(readOnly = true)
    public List<ProjectOutput> getDiagramsByType(String diagramType) {

        return projectOutputRepository.findAll().stream()
                .filter(output -> output.getRepresentationType() == RepresentationType.GRAPH)
                .filter(output -> output.getOutputType().equals(diagramType))
                .toList();
    }

    // Get the diagrams of a project, optionally only one type
    @Transactional(readOnly = true)
    public List<ProjectOutput> getProjectDiagrams(Integer projectId, String diagramType) {

        if (projectRepository.findProjectById(projectId) == null) {
            throw new ApiException("Project not found");
        }

        return projectOutputRepository.findAllByProject_Id(projectId).stream()
                .filter(output -> output.getRepresentationType() == RepresentationType.GRAPH)
                .filter(output -> diagramRulesRegistry.findRulesByDiagramType(output.getOutputType()) != null)
                .filter(output -> diagramType == null || output.getOutputType().equals(diagramType))
                .toList();
    }

    // Get one diagram
    @Transactional(readOnly = true)
    public ProjectOutput getDiagram(Integer outputId, String requiredType) {

        ProjectOutput output = projectOutputRepository.findProjectOutputById(outputId);

        if (output == null) {
            throw new ApiException("Project output not found");
        }

        getRules(output, requiredType);
        return output;
    }

    // Get the full graph and its revision. Uses the writers' lock so both describe one snapshot.
    @Transactional
    public DiagramGraphResponseDTO getGraph(Integer outputId, String requiredType) {

        ProjectOutput output = lockDiagram(outputId, requiredType);

        return new DiagramGraphResponseDTO(outputId, output.getGraphRevision(), diagramGraphService.read(output));
    }

    // Create an empty diagram; the project's domain must support the diagram type
    @Transactional
    public void addDiagram(Integer projectId, DiagramRequestDTO dto) {

        Project project = projectRepository.findProjectById(projectId);

        if (project == null) {
            throw new ApiException("Project not found");
        }

        if (dto.getDiagramType() == null || dto.getDiagramType().isBlank()) {
            throw new ApiException("Diagram type is required");
        }

        // Fails when the type does not belong to the project's domain
        diagramRulesRegistry.getRules(project.getDomain(), dto.getDiagramType());

        ProjectOutput output = new ProjectOutput();
        output.setTitle(dto.getTitle());
        output.setOutputType(dto.getDiagramType());
        output.setRepresentationType(RepresentationType.GRAPH);
        output.setProject(project);
        output.setCreatedAt(LocalDateTime.now());
        output.setUpdatedAt(output.getCreatedAt());

        projectOutputRepository.save(output);
    }

    // Rename a diagram
    @Transactional
    public void updateDiagram(Integer outputId, DiagramRequestDTO dto, Integer expectedRevision, String requiredType) {

        // Renaming is allowed for every diagram type, including Creativity diagrams
        ProjectOutput output = lockDiagram(outputId, expectedRevision, requiredType);

        output.setTitle(dto.getTitle());

        advanceRevision(output);
    }

    // Delete a diagram with all its nodes and edges
    @Transactional
    public void deleteDiagram(Integer outputId, Integer expectedRevision, String requiredType) {

        ProjectOutput output = lockEditableDiagram(outputId, expectedRevision, requiredType);

        projectOutputRepository.delete(output);
    }

    // Get one node of a diagram
    @Transactional(readOnly = true)
    public ProjectNode getNode(Integer outputId, Integer nodeId, String requiredType) {

        getDiagram(outputId, requiredType);

        return getOutputNode(outputId, nodeId);
    }

    // Add a node
    @Transactional
    public void addNode(Integer outputId, DiagramNodeRequestDTO dto, Integer expectedRevision, String requiredType) {

        ProjectOutput output = lockEditableDiagram(outputId, expectedRevision, requiredType);
        DiagramRules rules = getRules(output, requiredType);

        ProjectNode node = new ProjectNode();
        node.setProjectOutput(output);
        node.setCreatedAt(LocalDate.now());
        node.setOutgoingEdges(new HashSet<>());
        node.setIncomingEdges(new HashSet<>());
        setNodeFields(node, dto, rules);

        projectNodeRepository.save(node);
        output.getNodes().add(node);

        validateWholeGraph(output, rules);
        advanceRevision(output);
    }

    // Update a node; its connected edges are re-checked with the whole graph
    @Transactional
    public void updateNode(Integer outputId, Integer nodeId, DiagramNodeRequestDTO dto, Integer expectedRevision, String requiredType) {

        ProjectOutput output = lockEditableDiagram(outputId, expectedRevision, requiredType);
        DiagramRules rules = getRules(output, requiredType);
        ProjectNode node = getOutputNode(outputId, nodeId);

        setNodeFields(node, dto, rules);
        projectNodeRepository.save(node);

        validateWholeGraph(output, rules);
        advanceRevision(output);
    }

    // Delete a node together with its connected edges
    @Transactional
    public void deleteNode(Integer outputId, Integer nodeId, Integer expectedRevision, String requiredType) {

        ProjectOutput output = lockEditableDiagram(outputId, expectedRevision, requiredType);
        ProjectNode node = getOutputNode(outputId, nodeId);

        // Remove connected edges from both sides of the in-memory graph before deleting,
        // otherwise cascading collections can keep the deleted entities
        Set<ProjectEdge> connectedEdges = new HashSet<>();
        if (node.getOutgoingEdges() != null) {
            connectedEdges.addAll(node.getOutgoingEdges());
        }
        if (node.getIncomingEdges() != null) {
            connectedEdges.addAll(node.getIncomingEdges());
        }

        for (ProjectEdge edge : connectedEdges) {
            detachEdge(output, edge);
            projectEdgeRepository.delete(edge);
        }

        output.getNodes().remove(node);
        projectNodeRepository.delete(node);

        advanceRevision(output);
    }

    // Get one edge of a diagram
    @Transactional(readOnly = true)
    public ProjectEdge getEdge(Integer outputId, Integer edgeId, String requiredType) {

        getDiagram(outputId, requiredType);

        return getOutputEdge(outputId, edgeId);
    }

    // Add an edge between two nodes of the same diagram
    @Transactional
    public void addEdge(Integer outputId, DiagramEdgeRequestDTO dto, Integer expectedRevision, String requiredType) {

        ProjectOutput output = lockEditableDiagram(outputId, expectedRevision, requiredType);
        DiagramRules rules = getRules(output, requiredType);
        ProjectNode source = getOutputNode(outputId, dto.getSourceNodeId());
        ProjectNode target = getOutputNode(outputId, dto.getTargetNodeId());

        ProjectEdge edge = new ProjectEdge();
        edge.setProjectOutput(output);
        edge.setCreatedAt(LocalDate.now());
        setEdgeFields(edge, dto, source, target, rules);

        projectEdgeRepository.save(edge);
        output.getEdges().add(edge);
        addEdgeToNodes(edge);

        validateWholeGraph(output, rules);
        advanceRevision(output);
    }

    // Update an edge, including its endpoints
    @Transactional
    public void updateEdge(Integer outputId, Integer edgeId, DiagramEdgeRequestDTO dto, Integer expectedRevision, String requiredType) {

        ProjectOutput output = lockEditableDiagram(outputId, expectedRevision, requiredType);
        DiagramRules rules = getRules(output, requiredType);
        ProjectEdge edge = getOutputEdge(outputId, edgeId);
        ProjectNode source = getOutputNode(outputId, dto.getSourceNodeId());
        ProjectNode target = getOutputNode(outputId, dto.getTargetNodeId());

        detachEdge(output, edge);
        setEdgeFields(edge, dto, source, target, rules);
        projectEdgeRepository.save(edge);
        output.getEdges().add(edge);
        addEdgeToNodes(edge);

        validateWholeGraph(output, rules);
        advanceRevision(output);
    }

    // Delete an edge
    @Transactional
    public void deleteEdge(Integer outputId, Integer edgeId, Integer expectedRevision, String requiredType) {

        ProjectOutput output = lockEditableDiagram(outputId, expectedRevision, requiredType);
        ProjectEdge edge = getOutputEdge(outputId, edgeId);

        detachEdge(output, edge);
        projectEdgeRepository.delete(edge);

        advanceRevision(output);
    }

    // Find the rules of a diagram and check it has the required type
    private DiagramRules getRules(ProjectOutput output, String requiredType) {

        if (requiredType != null && !requiredType.equals(output.getOutputType())) {
            throw new ApiException("Output is not a " + requiredType + " diagram");
        }

        DiagramRules rules = null;
        if (output.getRepresentationType() == RepresentationType.GRAPH && output.getProject() != null) {
            rules = diagramRulesRegistry.findRules(output.getProject().getDomain(), output.getOutputType());
        }

        if (rules == null) {
            throw new ApiException("Output is not an editable diagram");
        }

        return rules;
    }

    // Lock the diagram for reading the graph
    private ProjectOutput lockDiagram(Integer outputId, String requiredType) {

        ProjectOutput output = projectOutputRepository.findByIdForUpdate(outputId)
                .orElseThrow(() -> new ApiException("Project output not found"));

        getRules(output, requiredType);

        if (output.getGraphRevision() == null) {
            throw new ApiException("Graph revision migration is required");
        }

        return output;
    }

    // Lock the diagram for a write and check the client edited the latest revision
    private ProjectOutput lockDiagram(Integer outputId, Integer expectedRevision, String requiredType) {

        if (expectedRevision == null || expectedRevision < 0) {
            throw new ApiException("Expected revision must be a non-negative integer");
        }

        ProjectOutput output = lockDiagram(outputId, requiredType);

        if (!expectedRevision.equals(output.getGraphRevision())) {
            throw new ApiConflictException("Diagram changed. Reload the graph and retry with its current revision");
        }

        if (output.getGraphRevision() == Integer.MAX_VALUE) {
            throw new ApiConflictException("Graph revision limit reached");
        }

        return output;
    }

    // Lock a diagram for a node, edge or delete write.
    // Creativity diagrams keep profile, scene and choice rows, so only their own endpoints change them.
    private ProjectOutput lockEditableDiagram(Integer outputId, Integer expectedRevision, String requiredType) {

        ProjectOutput output = lockDiagram(outputId, expectedRevision, requiredType);

        if (diagramGraphService.isDomainEdited(output.getOutputType())) {
            throw new ApiException("Use the Creativity endpoints to change the content of this diagram");
        }

        return output;
    }

    // Apply the same whole-graph checks that AI proposals go through; a failure rolls back the change
    private void validateWholeGraph(ProjectOutput output, DiagramRules rules) {

        GraphDefinitionDTO graph = graphMapper.toGraphDTO(output);

        graphValidator.validate(graph, graph, output.getTitle(), rules);
    }

    private void advanceRevision(ProjectOutput output) {

        output.setGraphRevision(output.getGraphRevision() + 1);
        output.setUpdatedAt(LocalDateTime.now());
        // The locked output is managed; dirty checking commits its revision atomically.
    }

    private void setNodeFields(ProjectNode node, DiagramNodeRequestDTO dto, DiagramRules rules) {

        Map<String, Object> data = new LinkedHashMap<>();
        if (dto.getData() != null) {
            data.putAll(dto.getData());
        }
        data.put("label", dto.getLabel());

        rules.validateNode(dto.getType(), data, dto.getPositionX(), dto.getPositionY());

        node.setType(dto.getType());
        node.setDataJson(jsonMapper.writeValueAsString(data));
        node.setPositionX(dto.getPositionX());
        node.setPositionY(dto.getPositionY());
        node.setUpdatedAt(LocalDate.now());
    }

    private void setEdgeFields(ProjectEdge edge, DiagramEdgeRequestDTO dto, ProjectNode source, ProjectNode target, DiagramRules rules) {

        if (source.getId().equals(target.getId())) {
            throw new ApiException("Edges require two different nodes");
        }

        // The edge type is the label when the user gives none
        String label = dto.getLabel() == null || dto.getLabel().isBlank() ? dto.getType() : dto.getLabel();
        Map<String, Object> properties = dto.getData() == null ? Map.of() : dto.getData();

        rules.validateEdge(dto.getType(), label, source.getType(), target.getType(), properties);

        edge.setType(dto.getType());
        edge.setLabel(label);
        edge.setDataJson(dto.getData() == null ? null : jsonMapper.writeValueAsString(dto.getData()));
        edge.setSourceNode(source);
        edge.setTargetNode(target);
    }

    private void addEdgeToNodes(ProjectEdge edge) {

        if (edge.getSourceNode().getOutgoingEdges() == null) {
            edge.getSourceNode().setOutgoingEdges(new HashSet<>());
        }
        if (edge.getTargetNode().getIncomingEdges() == null) {
            edge.getTargetNode().setIncomingEdges(new HashSet<>());
        }

        edge.getSourceNode().getOutgoingEdges().add(edge);
        edge.getTargetNode().getIncomingEdges().add(edge);
    }

    private void detachEdge(ProjectOutput output, ProjectEdge edge) {

        output.getEdges().remove(edge);

        if (edge.getSourceNode() != null && edge.getSourceNode().getOutgoingEdges() != null) {
            edge.getSourceNode().getOutgoingEdges().remove(edge);
        }
        if (edge.getTargetNode() != null && edge.getTargetNode().getIncomingEdges() != null) {
            edge.getTargetNode().getIncomingEdges().remove(edge);
        }
    }

    private ProjectNode getOutputNode(Integer outputId, Integer nodeId) {

        ProjectNode node = projectNodeRepository.findProjectNodeById(nodeId);

        if (node == null) {
            throw new ApiException("Project node not found");
        }
        if (node.getProjectOutput() == null || !outputId.equals(node.getProjectOutput().getId())) {
            throw new ApiException("Project node does not belong to this diagram");
        }

        return node;
    }

    private ProjectEdge getOutputEdge(Integer outputId, Integer edgeId) {

        ProjectEdge edge = projectEdgeRepository.findProjectEdgeById(edgeId);

        if (edge == null) {
            throw new ApiException("Project edge not found");
        }
        if (edge.getProjectOutput() == null || !outputId.equals(edge.getProjectOutput().getId())) {
            throw new ApiException("Project edge does not belong to this diagram");
        }

        return edge;
    }
}
