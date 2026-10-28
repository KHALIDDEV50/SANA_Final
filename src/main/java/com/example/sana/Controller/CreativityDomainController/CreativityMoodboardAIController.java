package com.example.sana.Controller.CreativityDomainController;

import com.example.sana.DTO.CreativityDomainDTOs.MoodboardAIDTO.*;
import com.example.sana.Service.CreativityDomainServices.MoodboardAIService;
import com.example.sana.Service.OwnershipService;
import com.example.sana.Model.User;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.util.Map;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/projects/{projectId}/creativity/moodboard-ai")
public class CreativityMoodboardAIController {
    private final MoodboardAIService service;
    private final OwnershipService ownershipService;

    @GetMapping("/capabilities")
    public Map<String, Object> capabilities(@AuthenticationPrincipal User user, @PathVariable Integer projectId) {
        ownershipService.checkProject(user, projectId);
        return Map.of("configured", service.configured(), "imagesVisuallyAnalyzed", false);
    }

    @PostMapping("/drafts")
    public Preview generate(@AuthenticationPrincipal User user, @PathVariable Integer projectId, @Valid @RequestBody Generate body, HttpServletRequest request) {
        ownershipService.checkProject(user, projectId);
        return service.generate(projectId, body, session(request));
    }
    @GetMapping("/drafts/{draftId}")
    public Preview get(@AuthenticationPrincipal User user, @PathVariable Integer projectId, @PathVariable UUID draftId, HttpServletRequest request) {
        ownershipService.checkProject(user, projectId);
        return service.get(projectId, draftId.toString(), session(request));
    }
    @PostMapping("/drafts/{draftId}/approve")
    public Saved approve(@AuthenticationPrincipal User user, @PathVariable Integer projectId, @PathVariable UUID draftId,
                         @Valid @RequestBody Decision body, HttpServletRequest request) {
        ownershipService.checkProject(user, projectId);
        return service.approve(projectId, draftId.toString(), session(request));
    }
    @PostMapping("/drafts/{draftId}/reject")
    public ResponseEntity<Void> reject(@AuthenticationPrincipal User user, @PathVariable Integer projectId, @PathVariable UUID draftId,
                                      @Valid @RequestBody Decision body, HttpServletRequest request) {
        ownershipService.checkProject(user, projectId);
        service.reject(projectId, draftId.toString(), session(request)); return ResponseEntity.noContent().build();
    }
    private String session(HttpServletRequest request) {
        // A non-simple header prevents cross-origin form submissions; no permissive CORS is added.
        if (!"1".equals(request.getHeader("X-Sana-Moodboard")))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Moodboard request header is required");
        try {
            var hash = java.security.MessageDigest.getInstance("SHA-256").digest(
                    request.getSession(true).getId().getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return java.util.HexFormat.of().formatHex(hash);
        } catch (java.security.NoSuchAlgorithmException e) { throw new IllegalStateException(e); }
    }
}
