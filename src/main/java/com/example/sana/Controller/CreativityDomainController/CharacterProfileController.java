package com.example.sana.Controller.CreativityDomainController;

import com.example.sana.Service.OwnershipService;
import com.example.sana.Model.User;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import com.example.sana.API.ApiResponse;
import com.example.sana.DTO.CreativityDomainDTOs.CharacterProfileDTO;
import com.example.sana.Service.CreativityDomainServices.CharacterProfileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/character-profile")
@RequiredArgsConstructor
public class CharacterProfileController {

    private final CharacterProfileService characterProfileService;
    private final OwnershipService ownershipService;

    @GetMapping("/get")
    public ResponseEntity<?> getAllCharacterProfiles() {
        return ResponseEntity.status(200).body(characterProfileService.getAllCharacterProfiles());
    }

    @GetMapping("/get/by/{id}")
    public ResponseEntity<?> getCharacterProfileById(@AuthenticationPrincipal User user, @PathVariable Integer id) {
        ownershipService.checkCharacterProfile(user, id);

        return ResponseEntity.status(200).body(characterProfileService.getCharacterProfileById(id));
    }


    @PostMapping("/add/{outputId}")
    public ResponseEntity<?> addCharacterProfile(@AuthenticationPrincipal User user, @PathVariable Integer outputId, @RequestBody @Valid CharacterProfileDTO dto) {
        ownershipService.checkOutput(user, outputId);

        characterProfileService.addCharacterProfile(outputId, dto);
        return ResponseEntity.status(200).body(new ApiResponse("Character profile added successfully"));
    }



    @PutMapping("/update/{id}")
    public ResponseEntity<?> updateCharacterProfile(@AuthenticationPrincipal User user, @PathVariable Integer id, @RequestBody @Valid CharacterProfileDTO dto) {
        ownershipService.checkCharacterProfile(user, id);

        characterProfileService.updateCharacterProfile(id, dto);
        return ResponseEntity.status(200).body(new ApiResponse("Character profile updated successfully"));
    }


    @DeleteMapping("/delete/{id}")
    public ResponseEntity<?> deleteCharacterProfile(@AuthenticationPrincipal User user, @PathVariable Integer id) {
        ownershipService.checkCharacterProfile(user, id);

        characterProfileService.deleteCharacterProfile(id);
        return ResponseEntity.status(200).body(new ApiResponse("Character profile deleted successfully"));
    }
}


