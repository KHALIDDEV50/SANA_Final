package com.example.sana.Controller.CreativityDomainController;

import com.example.sana.DTO.AIDTOs.AIRunResponseDTO;
import com.example.sana.DTO.CreativityDomainDTOs.CreativityAIRequestDTO;
import com.example.sana.Service.CreativityDomainServices.CreativityAIRequestService;
import com.example.sana.Service.OwnershipService;
import com.example.sana.Model.User;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/** Dedicated Creativity entrypoint. Other domains keep their existing /ai/requests route. */
@RestController
@RequestMapping("/api/projects/{projectId}/creativity/ai")
@RequiredArgsConstructor
public class CreativityAIRequestController {
    private final CreativityAIRequestService creativityAIRequestService;
    private final OwnershipService ownershipService;

    @PostMapping("/requests")
    public ResponseEntity<AIRunResponseDTO> start(
            @AuthenticationPrincipal User user, @PathVariable Integer projectId,
            @Valid @RequestBody CreativityAIRequestDTO request) {
        ownershipService.checkProject(user, projectId);
        return ResponseEntity.status(202).body(creativityAIRequestService.start(projectId, request));
    }
}
