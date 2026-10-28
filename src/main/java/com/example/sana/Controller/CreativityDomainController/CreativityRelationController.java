package com.example.sana.Controller.CreativityDomainController;

import com.example.sana.Service.OwnershipService;
import com.example.sana.Model.User;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import com.example.sana.API.ApiResponse;
import com.example.sana.DTO.CreativityDomainDTOs.CreativityRelationCreateDTO;
import com.example.sana.Service.CreativityDomainServices.CreativityRelationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/creativity/relations")
@RequiredArgsConstructor
public class CreativityRelationController {

    private final CreativityRelationService creativityRelationService;
    private final OwnershipService ownershipService;


    // Get all relationships in a ProjectOutput
    // Used when displaying the complete visual map
    @GetMapping("/get/{outputId}")
    public ResponseEntity<?> getOutputRelations(@AuthenticationPrincipal User user, 
            @PathVariable Integer outputId) {
        ownershipService.checkOutput(user, outputId);

        return ResponseEntity.status(200).body(creativityRelationService.getOutputRelations(outputId));
    }


    // Get all relationships connected to a specific Node
    // Used when the user selects a Character, World, Location, etc.
    // Includes both incoming and outgoing edges
    @GetMapping("/get/{outputId}/node/{nodeId}")
    public ResponseEntity<?> getNodeRelations(@AuthenticationPrincipal User user, 
            @PathVariable Integer outputId,
            @PathVariable Integer nodeId) {
        ownershipService.checkOutput(user, outputId);

        return ResponseEntity.status(200).body(creativityRelationService.getNodeRelations(outputId, nodeId));
    }


    // Get available relationship types between two Nodes
    // Used when the user selects two elements on the canvas
    // All IDs are passed through the Path
    @GetMapping("/get/types/{outputId}/{sourceNodeId}/{targetNodeId}")
    public ResponseEntity<?> getAllowedRelationTypes(@AuthenticationPrincipal User user, 
            @PathVariable Integer outputId,
            @PathVariable Integer sourceNodeId,
            @PathVariable Integer targetNodeId) {
        ownershipService.checkOutput(user, outputId);

        return ResponseEntity.status(200).body(creativityRelationService.getAllowedRelationTypes(outputId, sourceNodeId, targetNodeId));
    }


    // Create a relationship between two creativity Nodes
    // Checks whether the selected relationship type is valid
    // Uses the existing ProjectEdgeService to save the Edge
    @PostMapping("/add/{outputId}/{sourceNodeId}/{targetNodeId}")
    public ResponseEntity<?> addCreativityRelation(@AuthenticationPrincipal User user, 
            @PathVariable Integer outputId,
            @PathVariable Integer sourceNodeId,
            @PathVariable Integer targetNodeId,
            @RequestBody @Valid CreativityRelationCreateDTO dto) {
        ownershipService.checkOutput(user, outputId);

        creativityRelationService.addCreativityRelation(
                outputId, sourceNodeId, targetNodeId, dto);

        return ResponseEntity.status(200).body(new ApiResponse("Creativity relation added successfully"));
    }


    // Preview the impact of deleting a creativity Node
    // Shows affected relationships and possible deletion blockers
    // Does NOT delete the Node or its connected elements
    // outputId = ProjectOutput ID
    // nodeId = ProjectNode ID

    @GetMapping("/delete/preview/{outputId}/node/{nodeId}")
    public ResponseEntity<?> previewNodeDeletion(@AuthenticationPrincipal User user, 
            @PathVariable Integer outputId,
            @PathVariable Integer nodeId) {
        ownershipService.checkOutput(user, outputId);

        return ResponseEntity.status(200).body(creativityRelationService.previewNodeDeletion(outputId, nodeId));
    }




}
