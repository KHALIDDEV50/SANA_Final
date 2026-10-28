package com.example.sana.Controller.CreativityDomainController;

import com.example.sana.Service.OwnershipService;
import com.example.sana.Model.User;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

import com.example.sana.API.ApiResponse;
import com.example.sana.DTO.CreativityDomainDTOs.WorldDTO;
import com.example.sana.Service.CreativityDomainServices.WorldService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/world")
@RequiredArgsConstructor
public class WorldController {

    private final WorldService worldService;
    private final OwnershipService ownershipService;

    @GetMapping("/get")
    public ResponseEntity<?> getAllWorlds() {
        return ResponseEntity.status(200).body(worldService.getAllWorlds());
    }

    @GetMapping("/get/{id}")
    public ResponseEntity<?> getWorldById(@AuthenticationPrincipal User user, @PathVariable Integer id) {
        ownershipService.checkWorld(user, id);

        return ResponseEntity.status(200).body(worldService.getWorldById(id));
    }

    @PostMapping("/add/{outputId}")
    public ResponseEntity<?> addWorld(@AuthenticationPrincipal User user, @PathVariable Integer outputId, @RequestBody @Valid WorldDTO dto) {
        ownershipService.checkOutput(user, outputId);

        worldService.addWorld(outputId, dto);
        return ResponseEntity.status(200).body(new ApiResponse("World added successfully"));
    }

    @PutMapping("/update/{id}")
    public ResponseEntity<?> updateWorld(@AuthenticationPrincipal User user, @PathVariable Integer id, @RequestBody @Valid WorldDTO dto) {
        ownershipService.checkWorld(user, id);

        worldService.updateWorld(id, dto);
        return ResponseEntity.status(200).body(new ApiResponse("World updated successfully"));
    }

    @DeleteMapping("/delete/{id}")
    public ResponseEntity<?> deleteWorld(@AuthenticationPrincipal User user, @PathVariable Integer id) {
        ownershipService.checkWorld(user, id);

        worldService.deleteWorld(id);
        return ResponseEntity.status(200).body(new ApiResponse("World deleted successfully"));
    }
}

