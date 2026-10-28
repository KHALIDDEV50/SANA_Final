package com.example.sana.Controller.CreativityDomainController;

import com.example.sana.Service.OwnershipService;
import com.example.sana.Model.User;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import com.example.sana.API.ApiResponse;
import com.example.sana.DTO.CreativityDomainDTOs.WorldRuleDTO;
import com.example.sana.Service.CreativityDomainServices.WorldRuleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/world-rule")
@RequiredArgsConstructor
public class WorldRuleController {

    private final WorldRuleService worldRuleService;
    private final OwnershipService ownershipService;


    // Get all WorldRules in the system
    @GetMapping("/get")
    public ResponseEntity<?> getAllWorldRules() {
        return ResponseEntity.status(200).body(worldRuleService.getAllWorldRules());
    }


    // Get a specific WorldRule by its ID
    @GetMapping("/get/{id}")
    public ResponseEntity<?> getWorldRuleById(@AuthenticationPrincipal User user, @PathVariable Integer id) {
        ownershipService.checkWorldRule(user, id);

        return ResponseEntity.status(200).body(worldRuleService.getWorldRuleById(id));
    }


    // Add an independent WorldRule without a World
    // Automatically creates and links a ProjectNode
    // outputId = the ProjectOutput where the rule will be created
    @PostMapping("/add/{outputId}")
    public ResponseEntity<?> addWorldRule(@AuthenticationPrincipal User user, @PathVariable Integer outputId, @RequestBody @Valid WorldRuleDTO dto) {
        ownershipService.checkOutput(user, outputId);

        worldRuleService.addWorldRule(outputId, null, dto);
        return ResponseEntity.status(200).body(new ApiResponse("World rule added successfully"));
    }


    // Add a WorldRule to an existing World
    // The backend verifies that World belongs to the ProjectOutput
    // outputId = ProjectOutput ID
    // worldId = existing World ID
    @PostMapping("/add/{outputId}/world/{worldId}")
    public ResponseEntity<?> addWorldRuleToWorld(@AuthenticationPrincipal User user, @PathVariable Integer outputId, @PathVariable Integer worldId, @RequestBody @Valid WorldRuleDTO dto) {
        ownershipService.checkOutput(user, outputId);

        worldRuleService.addWorldRule(outputId, worldId, dto);
        return ResponseEntity.status(200).body(new ApiResponse("World rule added to world successfully"));
    }


    // Update WorldRule details and its ProjectNode position
    // Does NOT change the associated World
    @PutMapping("/update/{id}")
    public ResponseEntity<?> updateWorldRule(@AuthenticationPrincipal User user, @PathVariable Integer id, @RequestBody @Valid WorldRuleDTO dto) {
        ownershipService.checkWorldRule(user, id);

        worldRuleService.updateWorldRule(id, dto);
        return ResponseEntity.status(200).body(new ApiResponse("World rule updated successfully"));
    }


    // Assign an existing WorldRule to a World or change its World
    // ruleId = existing WorldRule ID
    // worldId = new World ID
    // Both must belong to the same ProjectOutput
    @PutMapping("/update/{ruleId}/world/{worldId}")
    public ResponseEntity<?> updateWorldRuleWorld(@AuthenticationPrincipal User user, @PathVariable Integer ruleId, @PathVariable Integer worldId) {
        ownershipService.checkWorldRule(user, ruleId);

        worldRuleService.updateWorldRuleWorld(ruleId, worldId);
        return ResponseEntity.status(200).body(new ApiResponse("World rule world updated successfully"));
    }


    // Delete a WorldRule and its associated ProjectNode
    // Connected ProjectEdges may also be deleted by cascade
    @DeleteMapping("/delete/{id}")
    public ResponseEntity<?> deleteWorldRule(@AuthenticationPrincipal User user, @PathVariable Integer id) {
        ownershipService.checkWorldRule(user, id);

        worldRuleService.deleteWorldRule(id);
        return ResponseEntity.status(200).body(new ApiResponse("World rule deleted successfully"));
    }
}

