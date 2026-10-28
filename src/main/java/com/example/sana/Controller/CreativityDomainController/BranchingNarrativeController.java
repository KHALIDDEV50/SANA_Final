package com.example.sana.Controller.CreativityDomainController;

import com.example.sana.Service.OwnershipService;
import com.example.sana.Model.User;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import com.example.sana.API.ApiResponse;
import com.example.sana.DTO.CreativityDomainDTOs.*;
import com.example.sana.Service.CreativityDomainServices.BranchingNarrativeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/branching")
@RequiredArgsConstructor
public class BranchingNarrativeController {

    private final BranchingNarrativeService branchingNarrativeService;
    private final OwnershipService ownershipService;


    // Get all branching scenes inside a ProjectOutput
    @GetMapping("/get/{outputId}/nodes")
    public ResponseEntity<?> getAllNodes(@AuthenticationPrincipal User user, @PathVariable Integer outputId) {
        ownershipService.checkOutput(user, outputId);

        return ResponseEntity.status(200).body(branchingNarrativeService.getAllNodes(outputId));
    }


    // Get one branching scene by ID
    @GetMapping("/get/{outputId}/nodes/{nodeId}")
    public ResponseEntity<?> getNodeById(@AuthenticationPrincipal User user, 
            @PathVariable Integer outputId,
            @PathVariable Integer nodeId) {
        ownershipService.checkOutput(user, outputId);

        return ResponseEntity.status(200).body(branchingNarrativeService.getNodeById(outputId, nodeId));
    }


    // Create a branching scene
    // outputId = ProjectOutput ID
    // Type can be DIALOGUE, DECISION, EVENT or ENDING
    @PostMapping("/add/{outputId}/nodes")
    public ResponseEntity<?> addNode(@AuthenticationPrincipal User user, 
            @PathVariable Integer outputId,
            @RequestBody @Valid BranchingNodeDTO dto) {
        ownershipService.checkOutput(user, outputId);

        branchingNarrativeService.addNode(outputId, dto);

        return ResponseEntity.status(200).body(new ApiResponse("Branching node added successfully"));
    }


    // Update an existing branching scene
    @PutMapping("/update/{outputId}/nodes/{nodeId}")
    public ResponseEntity<?> updateNode(@AuthenticationPrincipal User user, 
            @PathVariable Integer outputId,
            @PathVariable Integer nodeId,
            @RequestBody @Valid BranchingNodeDTO dto) {
        ownershipService.checkOutput(user, outputId);

        branchingNarrativeService.updateNode(outputId, nodeId, dto);

        return ResponseEntity.status(200).body(new ApiResponse("Branching node updated successfully"));
    }


    // Delete a branching scene
    // Scenes with incoming or outgoing choices cannot be deleted
    @DeleteMapping("/delete/{outputId}/nodes/{nodeId}")
    public ResponseEntity<?> deleteNode(@AuthenticationPrincipal User user, 
            @PathVariable Integer outputId,
            @PathVariable Integer nodeId) {
        ownershipService.checkOutput(user, outputId);

        branchingNarrativeService.deleteNode(outputId, nodeId);

        return ResponseEntity.status(200).body(new ApiResponse("Branching node deleted successfully"));
    }


    // Connect two scenes using a player choice
    // All IDs are passed through the Path
    @PostMapping("/add/{outputId}/choices/{sourceNodeId}/{targetNodeId}")
    public ResponseEntity<?> addChoice(@AuthenticationPrincipal User user, 
            @PathVariable Integer outputId,
            @PathVariable Integer sourceNodeId,
            @PathVariable Integer targetNodeId,
            @RequestBody @Valid BranchingChoiceDTO dto) {
        ownershipService.checkOutput(user, outputId);

        branchingNarrativeService.addChoice(outputId, sourceNodeId, targetNodeId, dto);

        return ResponseEntity.status(200).body(new ApiResponse("Branching choice added successfully"));
    }


    // Update an existing choice and its destination
    @PutMapping("/update/{outputId}/choices/{choiceId}/to/{targetNodeId}")
    public ResponseEntity<?> updateChoice(@AuthenticationPrincipal User user, 
            @PathVariable Integer outputId,
            @PathVariable Integer choiceId,
            @PathVariable Integer targetNodeId,
            @RequestBody @Valid BranchingChoiceDTO dto) {
        ownershipService.checkOutput(user, outputId);

        branchingNarrativeService.updateChoice(outputId, choiceId, targetNodeId, dto);

        return ResponseEntity.status(200).body(new ApiResponse("Branching choice updated successfully"));
    }


    // Delete a choice without deleting its scenes
    @DeleteMapping("/delete/{outputId}/choices/{choiceId}")
    public ResponseEntity<?> deleteChoice(@AuthenticationPrincipal User user, 
            @PathVariable Integer outputId,
            @PathVariable Integer choiceId) {
        ownershipService.checkOutput(user, outputId);

        branchingNarrativeService.deleteChoice(outputId, choiceId);

        return ResponseEntity.status(200).body(new ApiResponse("Branching choice deleted successfully"));
    }



    // Start a new preview using the game's initial variables
    @GetMapping("/play/{outputId}/start/{nodeId}")
    public ResponseEntity<?> startPreview(@AuthenticationPrincipal User user, 
            @PathVariable Integer outputId,
            @PathVariable Integer nodeId) {
        ownershipService.checkOutput(user, outputId);

        return ResponseEntity.status(200).body(branchingNarrativeService.startPreview(outputId, nodeId));
    }


    // Follow a player choice using the current game state
    // POST is used because the state is provided in the request body
    @PostMapping("/play/{outputId}/{nodeId}/choose/{choiceId}")
    public ResponseEntity<?> playChoice(@AuthenticationPrincipal User user, 
            @PathVariable Integer outputId,
            @PathVariable Integer nodeId,
            @PathVariable Integer choiceId,
            @RequestBody @Valid BranchingPlayStateDTO dto) {
        ownershipService.checkOutput(user, outputId);

        return ResponseEntity.status(200).body(branchingNarrativeService.playChoice(outputId, nodeId, choiceId, dto));
    }




    // Get the complete Branching Narrative visual map
    // Returns scenes with positions and their connected choices
    // Used by the frontend to render the branching canvas
    @GetMapping("/get/{outputId}/map")
    public ResponseEntity<?> getBranchingMap(@AuthenticationPrincipal User user, @PathVariable Integer outputId) {
        ownershipService.checkOutput(user, outputId);

        return ResponseEntity.status(200).body(branchingNarrativeService.getBranchingMap(outputId));
    }



    // Update the canvas position of one branching scene
    // outputId = ProjectOutput ID
    // nodeId = BranchingNode ID
    @PutMapping("/update/{outputId}/nodes/{nodeId}/position")
    public ResponseEntity<?> updateNodePosition(@AuthenticationPrincipal User user, 
            @PathVariable Integer outputId,
            @PathVariable Integer nodeId,
            @RequestBody @Valid BranchingPositionDTO dto) {
        ownershipService.checkOutput(user, outputId);

        branchingNarrativeService.updateNodePosition(outputId, nodeId, dto);

        return ResponseEntity.status(200).body(new ApiResponse("Branching node position updated successfully"));
    }


    // Update positions of multiple branching scenes
    // nodeIds is a comma-separated list of BranchingNode IDs
    // Positions must follow the same order as nodeIds
    @PutMapping("/update/{outputId}/layout/nodes/{nodeIds}")
    public ResponseEntity<?> updateMultipleNodePositions(@AuthenticationPrincipal User user, 
            @PathVariable Integer outputId,
            @PathVariable List<Integer> nodeIds,
            @RequestBody @Valid List<@Valid BranchingPositionDTO> positions) {
        ownershipService.checkOutput(user, outputId);

        branchingNarrativeService.updateMultipleNodePositions(outputId, nodeIds, positions);

        return ResponseEntity.status(200).body(new ApiResponse("Branching layout updated successfully"));
    }



    // Link an existing character to a branching scene
    // All IDs are provided in the Path
    @PutMapping("/link/{outputId}/nodes/{nodeId}/speaker/{characterId}")
    public ResponseEntity<?> linkSpeaker(@AuthenticationPrincipal User user, 
            @PathVariable Integer outputId,
            @PathVariable Integer nodeId,
            @PathVariable Integer characterId) {
        ownershipService.checkOutput(user, outputId);

        branchingNarrativeService.linkSpeaker(outputId, nodeId, characterId);

        return ResponseEntity.status(200).body(new ApiResponse("Speaker linked successfully"));
    }


    // Remove the speaker without deleting the CharacterProfile
    @DeleteMapping("/unlink/{outputId}/nodes/{nodeId}/speaker")
    public ResponseEntity<?> unlinkSpeaker(@AuthenticationPrincipal User user, 
            @PathVariable Integer outputId,
            @PathVariable Integer nodeId) {
        ownershipService.checkOutput(user, outputId);

        branchingNarrativeService.unlinkSpeaker(outputId, nodeId);

        return ResponseEntity.status(200).body(new ApiResponse("Speaker unlinked successfully"));
    }


    // Link an existing StoryEvent to a branching scene
    @PutMapping("/link/{outputId}/nodes/{nodeId}/event/{eventId}")
    public ResponseEntity<?> linkStoryEvent(@AuthenticationPrincipal User user, 
            @PathVariable Integer outputId,
            @PathVariable Integer nodeId,
            @PathVariable Integer eventId) {
        ownershipService.checkOutput(user, outputId);

        branchingNarrativeService.linkStoryEvent(outputId, nodeId, eventId);

        return ResponseEntity.status(200).body(new ApiResponse("Story event linked successfully"));
    }


    // Remove the event reference without deleting the StoryEvent
    @DeleteMapping("/unlink/{outputId}/nodes/{nodeId}/event")
    public ResponseEntity<?> unlinkStoryEvent(@AuthenticationPrincipal User user, 
            @PathVariable Integer outputId,
            @PathVariable Integer nodeId) {
        ownershipService.checkOutput(user, outputId);

        branchingNarrativeService.unlinkStoryEvent(outputId, nodeId);

        return ResponseEntity.status(200).body(new ApiResponse("Story event unlinked successfully"));
    }


    // Get the character and event connected to a branching scene
    @GetMapping("/get/{outputId}/nodes/{nodeId}/references")
    public ResponseEntity<?> getSceneReferences(@AuthenticationPrincipal User user, 
            @PathVariable Integer outputId,
            @PathVariable Integer nodeId) {
        ownershipService.checkOutput(user, outputId);

        return ResponseEntity.status(200).body(branchingNarrativeService.getSceneReferences(outputId, nodeId));
    }



    // Get all game variables inside a ProjectOutput
    @GetMapping("/get/{outputId}/variables")
    public ResponseEntity<?> getAllVariables(@AuthenticationPrincipal User user, 
            @PathVariable Integer outputId) {
        ownershipService.checkOutput(user, outputId);

        return ResponseEntity.status(200).body(branchingNarrativeService.getAllVariables(outputId));
    }


    // Create a game variable
    @PostMapping("/add/{outputId}/variables")
    public ResponseEntity<?> addVariable(@AuthenticationPrincipal User user, 
            @PathVariable Integer outputId,
            @RequestBody @Valid BranchingVariableDTO dto) {
        ownershipService.checkOutput(user, outputId);

        branchingNarrativeService.addVariable(outputId, dto);

        return ResponseEntity.status(200).body(new ApiResponse("Game variable added successfully"));
    }


    // Update a game variable
    @PutMapping("/update/{outputId}/variables/{variableId}")
    public ResponseEntity<?> updateVariable(@AuthenticationPrincipal User user, 
            @PathVariable Integer outputId,
            @PathVariable Integer variableId,
            @RequestBody @Valid BranchingVariableDTO dto) {
        ownershipService.checkOutput(user, outputId);

        branchingNarrativeService.updateVariable(outputId, variableId, dto);

        return ResponseEntity.status(200).body(new ApiResponse("Game variable updated successfully"));
    }


    // Delete a variable if it is not used by any choice
    @DeleteMapping("/delete/{outputId}/variables/{variableId}")
    public ResponseEntity<?> deleteVariable(@AuthenticationPrincipal User user, 
            @PathVariable Integer outputId,
            @PathVariable Integer variableId) {
        ownershipService.checkOutput(user, outputId);

        branchingNarrativeService.deleteVariable(outputId, variableId);

        return ResponseEntity.status(200).body(new ApiResponse("Game variable deleted successfully"));
    }


    // Set a condition for a player choice
    // variableId = BranchingVariable ID
    @PutMapping("/set/{outputId}/choices/{choiceId}/condition/{variableId}")
    public ResponseEntity<?> setChoiceCondition(@AuthenticationPrincipal User user, 
            @PathVariable Integer outputId,
            @PathVariable Integer choiceId,
            @PathVariable Integer variableId,
            @RequestBody @Valid BranchingConditionDTO dto) {
        ownershipService.checkOutput(user, outputId);

        branchingNarrativeService.setChoiceCondition(outputId, choiceId, variableId, dto);

        return ResponseEntity.status(200).body(new ApiResponse("Choice condition updated successfully"));
    }


    // Remove a condition from a player choice
    @DeleteMapping("/remove/{outputId}/choices/{choiceId}/condition")
    public ResponseEntity<?> removeChoiceCondition(@AuthenticationPrincipal User user, 
            @PathVariable Integer outputId,
            @PathVariable Integer choiceId) {
        ownershipService.checkOutput(user, outputId);

        branchingNarrativeService.removeChoiceCondition(outputId, choiceId);

        return ResponseEntity.status(200).body(new ApiResponse("Choice condition removed successfully"));
    }


    // Set an effect for a player choice
    @PutMapping("/set/{outputId}/choices/{choiceId}/effect/{variableId}")
    public ResponseEntity<?> setChoiceEffect(@AuthenticationPrincipal User user, 
            @PathVariable Integer outputId,
            @PathVariable Integer choiceId,
            @PathVariable Integer variableId,
            @RequestBody @Valid BranchingEffectDTO dto) {
        ownershipService.checkOutput(user, outputId);

        branchingNarrativeService.setChoiceEffect(outputId, choiceId, variableId, dto);

        return ResponseEntity.status(200).body(new ApiResponse("Choice effect updated successfully"));
    }


    // Remove an effect from a player choice
    @DeleteMapping("/remove/{outputId}/choices/{choiceId}/effect")
    public ResponseEntity<?> removeChoiceEffect(@AuthenticationPrincipal User user, 
            @PathVariable Integer outputId,
            @PathVariable Integer choiceId) {
        ownershipService.checkOutput(user, outputId);

        branchingNarrativeService.removeChoiceEffect(outputId, choiceId);

        return ResponseEntity.status(200).body(new ApiResponse("Choice effect removed successfully"));
    }

    // Get all saved settings for a specific player choice
    // Used when the writer opens the choice editor
    @GetMapping("/get/{outputId}/choices/{choiceId}/settings")
    public ResponseEntity<?> getChoiceSettings(@AuthenticationPrincipal User user, 
            @PathVariable Integer outputId,
            @PathVariable Integer choiceId) {
        ownershipService.checkOutput(user, outputId);

        return ResponseEntity.status(200).body(branchingNarrativeService.getChoiceSettings(outputId, choiceId));
    }







}
