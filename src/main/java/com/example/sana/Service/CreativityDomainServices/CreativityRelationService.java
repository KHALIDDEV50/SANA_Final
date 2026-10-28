package com.example.sana.Service.CreativityDomainServices;

import com.example.sana.API.ApiException;
import com.example.sana.Service.DiagramServices.DiagramWriteGuard;
import com.example.sana.DTO.CreativityDomainDTOs.CreativityDeletePreviewDTO;
import com.example.sana.DTO.CreativityDomainDTOs.CreativityRelationCreateDTO;
import com.example.sana.DTO.CreativityDomainDTOs.CreativityRelationDTO;
import com.example.sana.Enums.CreativityDomainEnums.CreativityRelationType;
import com.example.sana.Model.ProjectEdge;
import com.example.sana.Model.ProjectNode;
import com.example.sana.Model.ProjectOutput;
import com.example.sana.Repository.ProjectEdgeRepository;
import com.example.sana.Repository.ProjectNodeRepository;
import com.example.sana.Repository.ProjectOutputRepository;
import com.example.sana.Service.ProjectEdgeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CreativityRelationService {

    private final ProjectOutputRepository projectOutputRepository;
    private final ProjectNodeRepository projectNodeRepository;
    private final ProjectEdgeRepository projectEdgeRepository;
    private final ProjectEdgeService projectEdgeService;


    // Get all relationships inside a specific ProjectOutput
    @Transactional(readOnly = true)
    public List<CreativityRelationDTO> getOutputRelations(Integer outputId) {

        ProjectOutput output = projectOutputRepository.findProjectOutputById(outputId);

        if (output == null) {
            throw new ApiException("Project output not found");
        }

        return buildResults(output, null);
    }


    // Get relationships connected to a specific ProjectNode
    // Includes both incoming and outgoing relationships
    @Transactional(readOnly = true)
    public List<CreativityRelationDTO> getNodeRelations(
            Integer outputId, Integer nodeId) {

        ProjectOutput output = projectOutputRepository.findProjectOutputById(outputId);

        if (output == null) {
            throw new ApiException("Project output not found");
        }

        ProjectNode node = projectNodeRepository.findProjectNodeById(nodeId);

        if (node == null) {
            throw new ApiException("Project node not found");
        }

        // Make sure the Node belongs to the requested output
        if (!node.getProjectOutput().getId().equals(outputId)) {
            throw new ApiException("Project node does not belong to this output");
        }

        return buildResults(output, nodeId);
    }


    // Convert ProjectEdges into results for the frontend
    // If nodeId is null, return all relationships in the output
    // Otherwise, return relationships connected to that Node
    private List<CreativityRelationDTO> buildResults(ProjectOutput output, Integer nodeId) {

        return output.getEdges().stream()
                .filter(edge ->
                        nodeId == null
                                || edge.getSourceNode().getId().equals(nodeId)
                                || edge.getTargetNode().getId().equals(nodeId)
                )

                .sorted(Comparator.comparing(ProjectEdge::getId))

                .map(edge -> new CreativityRelationDTO(
                        edge.getId(),
                        edge.getSourceNode().getId(),
                        edge.getTargetNode().getId(),
                        edge.getType(),
                        edge.getLabel()
                ))

                .toList();
    }



    // Get valid relationship types for two selected Nodes
    // Used by the frontend to populate the relationship dropdown
    @Transactional(readOnly = true)
    public List<CreativityRelationType> getAllowedRelationTypes(
            Integer outputId,
            Integer sourceNodeId,
            Integer targetNodeId) {

        // Validate that ProjectOutput exists
        if (projectOutputRepository.findProjectOutputById(outputId) == null) {
            throw new ApiException("Project output not found");
        }

        // Validate both Nodes and their ProjectOutput
        ProjectNode source = getValidNode(sourceNodeId, outputId);
        ProjectNode target = getValidNode(targetNodeId, outputId);

        // Return only relationship types compatible with these Nodes
        return Arrays.stream(CreativityRelationType.values())
                .filter(type -> type.isAllowed(
                        source.getType(), target.getType()))
                .toList();
    }


    // Create a validated creativity relationship
    // The actual Edge is saved using the team's ProjectEdgeService
    @Transactional
    public void addCreativityRelation(
            Integer outputId,
            Integer sourceNodeId,
            Integer targetNodeId,
            CreativityRelationCreateDTO dto) {

        // Validate ProjectOutput
        if (projectOutputRepository.findProjectOutputById(outputId) == null) {
            throw new ApiException("Project output not found");
        }

        // Validate both Nodes
        ProjectNode source = getValidNode(sourceNodeId, outputId);
        ProjectNode target = getValidNode(targetNodeId, outputId);

        CreativityRelationType type = dto.getType();

        if (type == null) {
            throw new ApiException("Relationship type is required");
        }

        // Check business rules for the selected relationship
        if (!type.isAllowed(source.getType(), target.getType())) {
            throw new ApiException("Relationship type is not allowed between these nodes");
        }

        // Use the custom label when provided
        // Otherwise use the default label from the Enum
        String label = dto.getLabel();

        if (label == null || label.isBlank()) {
            label = type.getDefaultLabel();
        }

        // Custom relationships must have a meaningful label
        if (type == CreativityRelationType.CUSTOM
                && (dto.getLabel() == null
                || dto.getLabel().isBlank())) {

            throw new ApiException("Custom relationship label is required");
        }

        if (label.length() > 50) {
            throw new ApiException("Relationship label cannot exceed 50 characters");
        }

        // Prepare ProjectEdge
        ProjectEdge edge = new ProjectEdge();
        edge.setType(type.name());
        edge.setLabel(label);

        // Reuse the existing ProjectEdgeService
        // It handles linking and saving the Edge
        projectEdgeService.addEdge(outputId, sourceNodeId, targetNodeId, edge);
    }


    // Validate that a Node exists and belongs to the selected output
    private ProjectNode getValidNode(Integer nodeId, Integer outputId) {

        ProjectNode node = projectNodeRepository.findProjectNodeById(nodeId);

        if (node == null) {
            throw new ApiException("Project node not found");
        }

        if (node.getProjectOutput() == null || !node.getProjectOutput().getId().equals(outputId)) {

            throw new ApiException("Project node does not belong to this output");
        }

        DiagramWriteGuard.rejectUnversionedWrite(node.getProjectOutput());
        return node;
    }



    // Update an existing relationship type and label
    // Validates the relationship against the existing Node types
    @Transactional
    public void updateCreativityRelation(Integer edgeId, CreativityRelationCreateDTO dto) {

        ProjectEdge edge = projectEdgeRepository.findProjectEdgeById(edgeId);

        if (edge == null) {
            throw new ApiException("Project edge not found");
        }

        CreativityRelationType type = dto.getType();

        if (type == null) {
            throw new ApiException("Relationship type is required");
        }

        // Validate the existing Source and Target
        DiagramWriteGuard.rejectUnversionedWrite(edge.getProjectOutput());
        String sourceType = edge.getSourceNode().getType();
        String targetType = edge.getTargetNode().getType();

        if (!type.isAllowed(sourceType, targetType)) {
            throw new ApiException("Relationship type is not allowed between these nodes");
        }

        // Use the provided label or the default Enum label
        String label = dto.getLabel();

        if (label == null || label.isBlank()) {
            if (type == CreativityRelationType.CUSTOM) {
                throw new ApiException("Custom relationship label is required");
            }

            label = type.getDefaultLabel();
        }

        if (label.length() > 50) {
            throw new ApiException("Relationship label cannot exceed 50 characters");
        }

        edge.setType(type.name());
        edge.setLabel(label);

        projectEdgeRepository.save(edge);
    }



    // Preview the impact of deleting a creativity Node
    // This method does NOT delete or modify any data
    @Transactional(readOnly = true)
    public CreativityDeletePreviewDTO previewNodeDeletion(Integer outputId, Integer nodeId) {

        // Check if ProjectOutput exists
        ProjectOutput output = projectOutputRepository.findProjectOutputById(outputId);

        if (output == null) {
            throw new ApiException("Project output not found");
        }

        // Check if ProjectNode exists
        ProjectNode node = projectNodeRepository.findProjectNodeById(nodeId);

        if (node == null) {
            throw new ApiException("Project node not found");
        }

        // Make sure the Node belongs to the selected output
        if (node.getProjectOutput() == null || !node.getProjectOutput().getId().equals(outputId)) {
            throw new ApiException("Project node does not belong to this output");
        }

        // Get incoming and outgoing ProjectEdges
        // Reuse the method we already implemented
        List<CreativityRelationDTO> relations = getNodeRelations(outputId, nodeId);

        // Collect structural relationships that may block deletion
        List<String> blockingReasons = new ArrayList<>();

        // A World may be referenced by Locations and WorldRules
        if (node.getWorld() != null) {

            int locationCount = node.getWorld().getLocations() == null ? 0 : node.getWorld().getLocations().size();

            int ruleCount = node.getWorld().getRules() == null ? 0 : node.getWorld().getRules().size();

            if (locationCount > 0) {
                blockingReasons.add("World has " + locationCount + " linked locations");
            }

            if (ruleCount > 0) {
                blockingReasons.add("World has " + ruleCount + " linked world rules");
            }
        }

        // A Location with children cannot be deleted
        // according to our current LocationService
        if (node.getLocation() != null) {

            int childCount = node.getLocation().getChildLocations() == null ? 0 : node.getLocation().getChildLocations().size();

            if (childCount > 0) {
                blockingReasons.add("Location has " + childCount + " child locations");
            }
        }

        // Deletion is potentially allowed when no known blockers exist
        boolean canDelete = blockingReasons.isEmpty();

        // Return preview information without deleting anything
        return new CreativityDeletePreviewDTO(node.getId(), node.getType(), relations.size(), relations, canDelete, blockingReasons);
    }


}
