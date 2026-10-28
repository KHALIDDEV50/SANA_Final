package com.example.sana.Service.CreativityDomainServices;

import com.example.sana.API.ApiException;
import com.example.sana.Service.DiagramServices.DiagramWriteGuard;
import com.example.sana.DTO.CreativityDomainDTOs.*;
import com.example.sana.Enums.CreativityDomainEnums.BranchingNodeType;
import com.example.sana.Model.CreativityDomainModels.*;
import com.example.sana.Model.ProjectEdge;
import com.example.sana.Model.ProjectNode;
import com.example.sana.Model.ProjectOutput;
import com.example.sana.Repository.CreativityDomainRepository.*;
import com.example.sana.Repository.ProjectEdgeRepository;
import com.example.sana.Repository.ProjectNodeRepository;
import com.example.sana.Repository.ProjectOutputRepository;
import com.example.sana.Service.ProjectEdgeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;

@Service
@RequiredArgsConstructor
public class BranchingNarrativeService {

    private final BranchingNodeRepository branchingNodeRepository;
    private final BranchingChoiceRepository branchingChoiceRepository;

    private final ProjectOutputRepository projectOutputRepository;
    private final ProjectNodeRepository projectNodeRepository;
    private final ProjectEdgeRepository projectEdgeRepository;
    private final ProjectEdgeService projectEdgeService;

    private final CharacterProfileRepository characterProfileRepository;
    private final StoryEventRepository storyEventRepository;
    private final BranchingVariableRepository branchingVariableRepository;


    // Get all branching scenes inside the selected ProjectOutput
    @Transactional(readOnly = true)
    public List<BranchingMapDTO.NodeItem> getAllNodes(Integer outputId) {

        getValidOutput(outputId);

        return branchingNodeRepository
                .findByProjectNode_ProjectOutput_IdOrderByIdAsc(outputId)
                .stream()
                .map(this::toNodeItem)
                .toList();
    }


    // Get one branching scene with its visual position
    @Transactional(readOnly = true)
    public BranchingMapDTO.NodeItem getNodeById(
            Integer outputId, Integer nodeId) {

        BranchingNode node = getValidNode(outputId, nodeId);

        return toNodeItem(node);
    }



    // Includes scenes, positions and player choices
    // Get the branching canvas using lightweight database projections.
    // This avoids Hibernate's large eager-fetch joins.
    @Transactional(readOnly = true)
    public BranchingMapDTO getBranchingMap(Integer outputId) {

        // Verify that the selected ProjectOutput exists.
        if (!projectOutputRepository.existsById(outputId)) {
            throw new ApiException("Project output not found");
        }

        // Convert scalar scene rows directly into frontend DTOs.
        List<BranchingMapDTO.NodeItem> nodes = branchingNodeRepository.findMapNodes(outputId).stream().map(row -> new BranchingMapDTO.NodeItem(
                        (Integer) row[0],
                        (Integer) row[1],
                        (String) row[2],
                        (String) row[3],
                        (BranchingNodeType) row[4],
                        (Double) row[5],
                        (Double) row[6])).toList();

        // Convert scalar choice rows without fetching ProjectEdge objects.
        List<BranchingMapDTO.ChoiceItem> choices = branchingChoiceRepository.findMapChoices(outputId).stream().map(row -> new BranchingMapDTO.ChoiceItem(
                        (Integer) row[0],
                        (Integer) row[1],
                        (Integer) row[2],
                        (Integer) row[3],
                        (String) row[4],
                        (Integer) row[5])).toList();

        return new BranchingMapDTO(nodes, choices);
    }



    // Create a ProjectNode and BranchingNode together
    @Transactional
    public void addNode(Integer outputId, BranchingNodeDTO dto) {

        ProjectOutput output = getValidOutput(outputId);

        // Create the visual ProjectNode
        ProjectNode projectNode = new ProjectNode();

        projectNode.setType("BRANCHING");
        projectNode.setPositionX(dto.getPositionX());
        projectNode.setPositionY(dto.getPositionY());
        projectNode.setProjectOutput(output);
        projectNode.setCreatedAt(LocalDate.now());
        projectNode.setUpdatedAt(LocalDate.now());

        projectNodeRepository.save(projectNode);

        // Create the branching scene
        BranchingNode branchingNode = new BranchingNode();

        branchingNode.setTitle(dto.getTitle());
        branchingNode.setContent(dto.getContent());
        branchingNode.setType(dto.getType());
        branchingNode.setProjectNode(projectNode);
        branchingNode.setCreatedAt(LocalDate.now());
        branchingNode.setUpdatedAt(LocalDate.now());

        branchingNodeRepository.save(branchingNode);
    }


    // Update an existing branching scene and its canvas position
    @Transactional
    public void updateNode(
            Integer outputId, Integer nodeId, BranchingNodeDTO dto) {

        BranchingNode node = getValidNode(outputId, nodeId);
        ProjectNode projectNode = node.getProjectNode();

        // Ending scenes cannot have outgoing choices
        if (dto.getType() == BranchingNodeType.ENDING
                && !branchingChoiceRepository
                .findByProjectEdge_SourceNode_Id(
                        projectNode.getId()).isEmpty()) {

            throw new ApiException(
                    "Remove outgoing choices before setting an ending");
        }

        // Update branching scene details
        node.setTitle(dto.getTitle());
        node.setContent(dto.getContent());
        node.setType(dto.getType());
        node.setUpdatedAt(LocalDate.now());

        // Update position on the visual canvas
        projectNode.setPositionX(dto.getPositionX());
        projectNode.setPositionY(dto.getPositionY());
        projectNode.setUpdatedAt(LocalDate.now());

        branchingNodeRepository.save(node);
        projectNodeRepository.save(projectNode);
    }


    // Delete a branching scene and its visual ProjectNode
    // Prevent deletion when any ProjectEdge is connected
    @Transactional
    public void deleteNode(Integer outputId, Integer nodeId) {

        BranchingNode node = getValidNode(outputId, nodeId);
        ProjectNode projectNode = node.getProjectNode();

        // Prevent deleting a scene with connected ProjectEdges
        boolean hasOutgoingEdges =
                projectNode.getOutgoingEdges() != null
                        && !projectNode.getOutgoingEdges().isEmpty();

        boolean hasIncomingEdges =
                projectNode.getIncomingEdges() != null
                        && !projectNode.getIncomingEdges().isEmpty();

        if (hasOutgoingEdges || hasIncomingEdges) {
            throw new ApiException(
                    "Remove connected edges before deleting this scene");
        }

        // Delete the scene first because it references ProjectNode
        branchingNodeRepository.delete(node);
        branchingNodeRepository.flush();

        // Delete the visual ProjectNode
        projectNodeRepository.delete(projectNode);
    }


    // Create a player choice between two branching scenes
    // ProjectEdge stores the direction of the choice
    @Transactional
    public void addChoice(
            Integer outputId,
            Integer sourceNodeId,
            Integer targetNodeId,
            BranchingChoiceDTO dto) {

        BranchingNode source = getValidNode(outputId, sourceNodeId);
        BranchingNode target = getValidNode(outputId, targetNodeId);

        if (source.getType() == BranchingNodeType.ENDING) {
            throw new ApiException(
                    "Ending scenes cannot have outgoing choices");
        }

        // Create the visual connection between both ProjectNodes
        ProjectEdge edge = new ProjectEdge();

        edge.setType("BRANCH_CHOICE");
        edge.setLabel("Choice");

        // Reuse the team's existing edge creation service
        projectEdgeService.addEdge(
                outputId,
                source.getProjectNode().getId(),
                target.getProjectNode().getId(),
                edge
        );

        // Store choice-specific information separately
        BranchingChoice choice = new BranchingChoice();

        choice.setText(dto.getText());
        choice.setDisplayOrder(dto.getDisplayOrder());
        choice.setProjectEdge(edge);

        branchingChoiceRepository.save(choice);
    }


    // Update a player choice and its destination
    // The source scene remains the same
    @Transactional
    public void updateChoice(
            Integer outputId,
            Integer choiceId,
            Integer targetNodeId,
            BranchingChoiceDTO dto) {

        BranchingChoice choice = getValidChoice(outputId, choiceId);
        BranchingNode target = getValidNode(outputId, targetNodeId);

        ProjectEdge edge = choice.getProjectEdge();

        // Update the target of the existing ProjectEdge
        edge.setTargetNode(target.getProjectNode());

        // Update choice details
        choice.setText(dto.getText());
        choice.setDisplayOrder(dto.getDisplayOrder());

        projectEdgeRepository.save(edge);
        branchingChoiceRepository.save(choice);
    }


    // Delete a player choice and its associated ProjectEdge
    // Does not delete either branching scene
    @Transactional
    public void deleteChoice(Integer outputId, Integer choiceId) {

        BranchingChoice choice = getValidChoice(outputId, choiceId);
        ProjectEdge edge = choice.getProjectEdge();

        // Delete the choice before its referenced ProjectEdge
        branchingChoiceRepository.delete(choice);
        branchingChoiceRepository.flush();

        projectEdgeRepository.delete(edge);
    }


    // Convert a BranchingNode into a frontend-friendly DTO
    private BranchingMapDTO.NodeItem toNodeItem(BranchingNode node) {

        ProjectNode projectNode = node.getProjectNode();

        return new BranchingMapDTO.NodeItem(
                node.getId(),
                projectNode.getId(),
                node.getTitle(),
                node.getContent(),
                node.getType(),
                projectNode.getPositionX(),
                projectNode.getPositionY()
        );
    }


    // Convert a BranchingChoice into a frontend-friendly DTO
    private BranchingMapDTO.ChoiceItem toChoiceItem(
            BranchingChoice choice) {

        ProjectEdge edge = choice.getProjectEdge();

        BranchingNode source = getBranchingNodeByProjectNode(
                edge.getSourceNode().getId());

        BranchingNode target = getBranchingNodeByProjectNode(
                edge.getTargetNode().getId());

        return new BranchingMapDTO.ChoiceItem(
                choice.getId(),
                edge.getId(),
                source.getId(),
                target.getId(),
                choice.getText(),
                choice.getDisplayOrder()
        );
    }


    // Find ProjectOutput or throw an exception
    private ProjectOutput getValidOutput(Integer outputId) {

        ProjectOutput output =
                projectOutputRepository.findProjectOutputById(outputId);

        if (output == null) {
            throw new ApiException("Project output not found");
        }

        DiagramWriteGuard.rejectUnversionedWrite(output);
        return output;
    }


    // Find a branching scene within the selected ProjectOutput
    private BranchingNode getValidNode(Integer outputId, Integer nodeId) {

        BranchingNode node =
                branchingNodeRepository
                        .findByIdAndProjectNode_ProjectOutput_Id(
                                nodeId, outputId);

        if (node == null) {
            throw new ApiException(
                    "Branching node not found in this output");
        }

        DiagramWriteGuard.rejectUnversionedWrite(node.getProjectNode().getProjectOutput());
        return node;
    }


    // Find a branching scene using its visual ProjectNode ID
    private BranchingNode getBranchingNodeByProjectNode(
            Integer projectNodeId) {

        BranchingNode node =
                branchingNodeRepository.findByProjectNode_Id(
                        projectNodeId);

        if (node == null) {
            throw new ApiException(
                    "Branching scene not found for ProjectNode");
        }

        DiagramWriteGuard.rejectUnversionedWrite(node.getProjectNode().getProjectOutput());
        return node;
    }


    // Find a choice within the selected ProjectOutput
    private BranchingChoice getValidChoice(
            Integer outputId, Integer choiceId) {

        BranchingChoice choice =
                branchingChoiceRepository
                        .findByIdAndProjectEdge_ProjectOutput_Id(
                                choiceId, outputId);

        if (choice == null) {
            throw new ApiException(
                    "Branching choice not found in this output");
        }

        DiagramWriteGuard.rejectUnversionedWrite(choice.getProjectEdge().getProjectOutput());
        return choice;
    }



    // Update the position of one branching scene
    // Does not modify the scene's title, content or choices
    @Transactional
    public void updateNodePosition(
            Integer outputId,
            Integer nodeId,
            BranchingPositionDTO dto) {

        // Check that the scene belongs to this ProjectOutput
        BranchingNode node = getValidNode(outputId, nodeId);

        // Validate the new coordinates
        validatePosition(dto);

        // Get the visual ProjectNode
        ProjectNode projectNode = node.getProjectNode();

        // Update only the coordinates
        projectNode.setPositionX(dto.getPositionX());
        projectNode.setPositionY(dto.getPositionY());
        projectNode.setUpdatedAt(LocalDate.now());

        projectNodeRepository.save(projectNode);
    }



    // Update multiple branching scene positions in one transaction
    // Node IDs and positions must be provided in the same order
    @Transactional
    public void updateMultipleNodePositions(
            Integer outputId,
            List<Integer> nodeIds,
            List<BranchingPositionDTO> positions) {

        // Check that both lists contain the same number of items
        if (nodeIds == null || nodeIds.isEmpty()
                || positions == null
                || nodeIds.size() != positions.size()) {

            throw new ApiException(
                    "Node IDs and positions must have the same non-zero size");
        }

        // Prevent updating the same scene more than once in one request
        if (nodeIds.contains(null)
                || new HashSet<>(nodeIds).size() != nodeIds.size()) {

            throw new ApiException(
                    "Node IDs must be unique and cannot be null");
        }

        List<ProjectNode> projectNodes = new ArrayList<>();

        // Validate every scene and position before saving changes
        for (int i = 0; i < nodeIds.size(); i++) {

            BranchingNode node = getValidNode(outputId, nodeIds.get(i));
            BranchingPositionDTO position = positions.get(i);

            validatePosition(position);

            ProjectNode projectNode = node.getProjectNode();

            projectNode.setPositionX(position.getPositionX());
            projectNode.setPositionY(position.getPositionY());
            projectNode.setUpdatedAt(LocalDate.now());

            projectNodes.add(projectNode);
        }

        // Save all updated positions together
        projectNodeRepository.saveAll(projectNodes);
    }


    // Helper method

    // Validate coordinates before saving them
    // Negative coordinates are allowed on an infinite canvas
    private void validatePosition(BranchingPositionDTO dto) {

        if (dto == null
                || dto.getPositionX() == null
                || dto.getPositionY() == null
                || !Double.isFinite(dto.getPositionX())
                || !Double.isFinite(dto.getPositionY())) {

            throw new ApiException("Invalid canvas position");
        }
    }




    // Link an existing CharacterProfile as the speaker
    // Both elements must belong to the same Project
    @Transactional
    public void linkSpeaker(
            Integer outputId,
            Integer nodeId,
            Integer characterId) {

        BranchingNode node = getValidNode(outputId, nodeId);

        CharacterProfile character =
                characterProfileRepository.findCharacterProfileById(characterId);

        if (character == null) {
            throw new ApiException("Character profile not found");
        }

        // A character may belong to another output of the same project
        validateSameProject(
                node.getProjectNode(),
                character.getProjectNode()
        );

        node.setSpeaker(character);
        branchingNodeRepository.save(node);
    }


    // Remove the speaker reference without deleting the character
    @Transactional
    public void unlinkSpeaker(Integer outputId, Integer nodeId) {

        BranchingNode node = getValidNode(outputId, nodeId);

        node.setSpeaker(null);
        branchingNodeRepository.save(node);
    }


    // Link an existing StoryEvent to the branching scene
    @Transactional
    public void linkStoryEvent(
            Integer outputId,
            Integer nodeId,
            Integer eventId) {

        BranchingNode node = getValidNode(outputId, nodeId);

        StoryEvent event =
                storyEventRepository.findStoryEventById(eventId);

        if (event == null) {
            throw new ApiException("Story event not found");
        }

        validateSameProject(
                node.getProjectNode(),
                event.getProjectNode()
        );

        node.setRelatedStoryEvent(event);
        branchingNodeRepository.save(node);
    }


    // Remove the event reference without deleting the StoryEvent
    @Transactional
    public void unlinkStoryEvent(Integer outputId, Integer nodeId) {

        BranchingNode node = getValidNode(outputId, nodeId);

        node.setRelatedStoryEvent(null);
        branchingNodeRepository.save(node);
    }


    // Get the CharacterProfile and StoryEvent linked to a scene
    @Transactional(readOnly = true)
    public BranchingReferenceDTO getSceneReferences(
            Integer outputId, Integer nodeId) {

        BranchingNode node = getValidNode(outputId, nodeId);

        CharacterProfile speaker = node.getSpeaker();
        StoryEvent event = node.getRelatedStoryEvent();

        return new BranchingReferenceDTO(
                node.getId(),
                speaker == null ? null : speaker.getId(),
                speaker == null ? null : speaker.getName(),
                event == null ? null : event.getId(),
                event == null ? null : event.getTitle()
        );
    }


    // Prevent linking creativity elements from another Project
    private void validateSameProject(
            ProjectNode branchingProjectNode,
            ProjectNode referenceProjectNode) {

        if (branchingProjectNode == null
                || referenceProjectNode == null
                || branchingProjectNode.getProjectOutput() == null
                || referenceProjectNode.getProjectOutput() == null
                || branchingProjectNode.getProjectOutput().getProject() == null
                || referenceProjectNode.getProjectOutput().getProject() == null) {

            throw new ApiException("Invalid project reference");
        }

        Integer branchingProjectId = branchingProjectNode
                .getProjectOutput().getProject().getId();

        Integer referenceProjectId = referenceProjectNode
                .getProjectOutput().getProject().getId();

        if (!branchingProjectId.equals(referenceProjectId)) {
            throw new ApiException(
                    "Referenced element must belong to the same project");
        }
    }





    // Get all game variables defined in a ProjectOutput
    @Transactional(readOnly = true)
    public List<BranchingVariable> getAllVariables(Integer outputId) {

        getValidOutput(outputId);

        return branchingVariableRepository
                .findByProjectOutput_IdOrderByIdAsc(outputId);
    }


    // Create a new game variable
    @Transactional
    public void addVariable(Integer outputId, BranchingVariableDTO dto) {

        ProjectOutput output = getValidOutput(outputId);

        if (branchingVariableRepository
                .existsByProjectOutput_IdAndName(outputId, dto.getName())) {
            throw new ApiException("Variable name already exists");
        }

        BranchingVariable variable = new BranchingVariable();

        variable.setName(dto.getName());
        variable.setInitialValue(dto.getInitialValue());
        variable.setProjectOutput(output);

        branchingVariableRepository.save(variable);
    }


    // Update a variable's name or initial value
    @Transactional
    public void updateVariable(
            Integer outputId, Integer variableId,
            BranchingVariableDTO dto) {

        BranchingVariable variable =
                getValidVariable(outputId, variableId);

        // Prevent duplicate names when renaming a variable
        if (!variable.getName().equals(dto.getName())
                && branchingVariableRepository
                .existsByProjectOutput_IdAndName(
                        outputId, dto.getName())) {

            throw new ApiException("Variable name already exists");
        }

        variable.setName(dto.getName());
        variable.setInitialValue(dto.getInitialValue());

        branchingVariableRepository.save(variable);
    }


    // Delete a variable only if no choice references it
    @Transactional
    public void deleteVariable(Integer outputId, Integer variableId) {

        BranchingVariable variable =
                getValidVariable(outputId, variableId);

        if (branchingChoiceRepository
                .existsByConditionVariable_IdOrEffectVariable_Id(
                        variableId, variableId)) {

            throw new ApiException(
                    "Remove the variable from all choices before deleting it");
        }

        branchingVariableRepository.delete(variable);
    }


    // Validate a variable belongs to the selected ProjectOutput
    private BranchingVariable getValidVariable(
            Integer outputId, Integer variableId) {

        BranchingVariable variable =
                branchingVariableRepository
                        .findByIdAndProjectOutput_Id(variableId, outputId);

        if (variable == null) {
            throw new ApiException(
                    "Game variable not found in this output");
        }

        DiagramWriteGuard.rejectUnversionedWrite(variable.getProjectOutput());
        return variable;
    }





    // Set or replace the condition of a player choice
    @Transactional
    public void setChoiceCondition(
            Integer outputId,
            Integer choiceId,
            Integer variableId,
            BranchingConditionDTO dto) {

        BranchingChoice choice = getValidChoice(outputId, choiceId);
        BranchingVariable variable =
                getValidVariable(outputId, variableId);

        choice.setConditionVariable(variable);
        choice.setConditionOperator(dto.getOperator());
        choice.setConditionValue(dto.getValue());

        branchingChoiceRepository.save(choice);
    }


    // Remove a condition so the choice is always available
    @Transactional
    public void removeChoiceCondition(
            Integer outputId, Integer choiceId) {

        BranchingChoice choice = getValidChoice(outputId, choiceId);

        choice.setConditionVariable(null);
        choice.setConditionOperator(null);
        choice.setConditionValue(null);

        branchingChoiceRepository.save(choice);
    }


    // Set or replace the effect executed after a player choice
    @Transactional
    public void setChoiceEffect(
            Integer outputId,
            Integer choiceId,
            Integer variableId,
            BranchingEffectDTO dto) {

        BranchingChoice choice = getValidChoice(outputId, choiceId);
        BranchingVariable variable =
                getValidVariable(outputId, variableId);

        choice.setEffectVariable(variable);
        choice.setEffectOperator(dto.getOperator());
        choice.setEffectValue(dto.getValue());

        branchingChoiceRepository.save(choice);
    }


    // Remove a choice's effect without removing the choice
    @Transactional
    public void removeChoiceEffect(
            Integer outputId, Integer choiceId) {

        BranchingChoice choice = getValidChoice(outputId, choiceId);

        choice.setEffectVariable(null);
        choice.setEffectOperator(null);
        choice.setEffectValue(null);

        branchingChoiceRepository.save(choice);
    }




    // Start a new preview using the initial values of game variables
    // No game state is saved to the database
    @Transactional(readOnly = true)
    public BranchingGamePreviewDTO startPreview(
            Integer outputId, Integer nodeId) {

        getValidNode(outputId, nodeId);

        Map<String, Integer> variables = new LinkedHashMap<>();

        // Initialize game variables for a new play session
        branchingVariableRepository
                .findByProjectOutput_IdOrderByIdAsc(outputId)
                .forEach(variable ->
                        variables.put(
                                variable.getName(),
                                variable.getInitialValue()
                        )
                );

        return buildGamePreview(outputId, nodeId, variables);
    }


    // Return a scene with only its currently available choices
    private BranchingGamePreviewDTO buildGamePreview(
            Integer outputId,
            Integer nodeId,
            Map<String, Integer> variables) {

        BranchingNode node = getValidNode(outputId, nodeId);

        boolean ending = node.getType() == BranchingNodeType.ENDING;

        List<BranchingChoice> allChoices = ending
                ? List.of()
                : branchingChoiceRepository
                .findByProjectEdge_SourceNode_IdOrderByDisplayOrderAscIdAsc(
                        node.getProjectNode().getId());

        // Filter choices based on the current game variables
        List<BranchingPlayDTO.ChoiceOption> availableChoices =
                allChoices.stream()
                        .filter(choice -> conditionMatches(choice, variables))
                        .map(choice -> {

                            ProjectNode targetProjectNode =
                                    choice.getProjectEdge().getTargetNode();

                            BranchingNode target =
                                    getBranchingNodeByProjectNode(
                                            targetProjectNode.getId());

                            return new BranchingPlayDTO.ChoiceOption(
                                    choice.getId(),
                                    choice.getText(),
                                    target.getId(),
                                    targetProjectNode.getId()
                            );
                        })
                        .toList();

        // A dead end has no outgoing choices at all
        boolean deadEnd = !ending && allChoices.isEmpty();

        // A blocked scene has choices, but none meet their conditions
        boolean blocked = !ending
                && !allChoices.isEmpty()
                && availableChoices.isEmpty();

        BranchingPlayDTO scene = new BranchingPlayDTO(
                node.getId(),
                node.getProjectNode().getId(),
                node.getTitle(),
                node.getContent(),
                node.getType(),
                ending,
                deadEnd,
                availableChoices
        );

        return new BranchingGamePreviewDTO(
                scene,
                new LinkedHashMap<>(variables),
                blocked
        );
    }

    // Check whether a player choice is available
    private boolean conditionMatches(
            BranchingChoice choice,
            Map<String, Integer> variables) {

        // No condition means the choice is always available
        if (choice.getConditionVariable() == null) {
            return true;
        }

        if (choice.getConditionOperator() == null
                || choice.getConditionValue() == null) {
            throw new ApiException("Invalid choice condition");
        }

        String variableName =
                choice.getConditionVariable().getName();

        Integer currentValue = variables.get(variableName);

        if (currentValue == null) {
            throw new ApiException(
                    "Missing game variable: " + variableName);
        }

        int expectedValue = choice.getConditionValue();

        return switch (choice.getConditionOperator()) {
            case EQ -> currentValue == expectedValue;
            case NE -> currentValue != expectedValue;
            case GT -> currentValue > expectedValue;
            case GTE -> currentValue >= expectedValue;
            case LT -> currentValue < expectedValue;
            case LTE -> currentValue <= expectedValue;
        };
    }


    // Apply a choice effect to the preview state only
    // Does not modify the saved BranchingVariable.initialValue
    private void applyChoiceEffect(
            BranchingChoice choice,
            Map<String, Integer> variables) {

        // Choices without effects do not change the game state
        if (choice.getEffectVariable() == null) {
            return;
        }

        if (choice.getEffectOperator() == null
                || choice.getEffectValue() == null) {
            throw new ApiException("Invalid choice effect");
        }

        String variableName = choice.getEffectVariable().getName();

        Integer currentValue = variables.get(variableName);

        if (currentValue == null) {
            throw new ApiException(
                    "Missing game variable: " + variableName);
        }

        int effectValue = choice.getEffectValue();
        int newValue;

        try {
            newValue = switch (choice.getEffectOperator()) {

                // Set the variable to a specific value
                case SET -> effectValue;

                // Increase or decrease the current value
                case ADD -> Math.addExact(currentValue, effectValue);
            };

        } catch (ArithmeticException e) {
            throw new ApiException("Game variable value is out of range");
        }

        variables.put(variableName, newValue);
    }



    // Follow a choice using the current game state
    // Validate its condition, apply its effect and open the next scene
    @Transactional(readOnly = true)
    public BranchingGamePreviewDTO playChoice(
            Integer outputId,
            Integer currentNodeId,
            Integer choiceId,
            BranchingPlayStateDTO dto) {

        BranchingNode current = getValidNode(outputId, currentNodeId);
        BranchingChoice choice = getValidChoice(outputId, choiceId);

        if (current.getType() == BranchingNodeType.ENDING) {
            throw new ApiException(
                    "Cannot continue after reaching an ending");
        }

        // Make sure the choice starts from the current scene
        if (!choice.getProjectEdge().getSourceNode()
                .getId().equals(current.getProjectNode().getId())) {

            throw new ApiException(
                    "Choice does not belong to the current scene");
        }

        // Validate that the frontend sent all expected game variables
        Map<String, Integer> variables =
                validateGameState(outputId, dto);

        // The chosen option must satisfy its condition
        if (!conditionMatches(choice, variables)) {
            throw new ApiException(
                    "Choice condition is not satisfied");
        }

        // Apply the effect to this preview session only
        applyChoiceEffect(choice, variables);

        // Find the next scene through the existing ProjectEdge
        Integer targetProjectNodeId =
                choice.getProjectEdge().getTargetNode().getId();

        BranchingNode next =
                getBranchingNodeByProjectNode(targetProjectNodeId);

        return buildGamePreview(outputId, next.getId(), variables);
    }


    // Validate the received game state
    // Prevent missing, unknown or null variable values
    private Map<String, Integer> validateGameState(
            Integer outputId, BranchingPlayStateDTO dto) {

        if (dto == null || dto.getVariables() == null) {
            throw new ApiException("Game state is required");
        }

        Map<String, Integer> expected = new LinkedHashMap<>();

        branchingVariableRepository
                .findByProjectOutput_IdOrderByIdAsc(outputId)
                .forEach(variable ->
                        expected.put(
                                variable.getName(),
                                variable.getInitialValue()
                        )
                );

        Map<String, Integer> received = dto.getVariables();

        if (!received.keySet().equals(expected.keySet())
                || received.values().stream().anyMatch(value -> value == null)) {

            throw new ApiException(
                    "Game state does not match the project variables");
        }

        return new LinkedHashMap<>(received);
    }


    // Get the saved settings of a player choice
    // Includes condition, effect and connected scenes
    @Transactional(readOnly = true)
    public BranchingChoiceSettingsDTO getChoiceSettings(
            Integer outputId, Integer choiceId) {

        // Validate that the choice belongs to this ProjectOutput
        BranchingChoice choice = getValidChoice(outputId, choiceId);

        ProjectEdge edge = choice.getProjectEdge();

        // Find the branching scenes linked by this choice
        BranchingNode source = getBranchingNodeByProjectNode(
                edge.getSourceNode().getId());

        BranchingNode target = getBranchingNodeByProjectNode(
                edge.getTargetNode().getId());

        // Get the optional condition and effect variables
        BranchingVariable conditionVariable =
                choice.getConditionVariable();

        BranchingVariable effectVariable =
                choice.getEffectVariable();

        // Return all saved settings without modifying any data
        return new BranchingChoiceSettingsDTO(
                choice.getId(),
                source.getId(),
                target.getId(),
                choice.getText(),
                choice.getDisplayOrder(),

                conditionVariable == null
                        ? null : conditionVariable.getId(),
                conditionVariable == null
                        ? null : conditionVariable.getName(),
                choice.getConditionOperator(),
                choice.getConditionValue(),

                effectVariable == null
                        ? null : effectVariable.getId(),
                effectVariable == null
                        ? null : effectVariable.getName(),
                choice.getEffectOperator(),
                choice.getEffectValue()
        );
    }









}
