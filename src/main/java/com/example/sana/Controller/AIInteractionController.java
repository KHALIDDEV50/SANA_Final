package com.example.sana.Controller;

import com.example.sana.API.ApiResponse;
import com.example.sana.Model.AIInteraction;
import com.example.sana.Service.AIInteractionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/ai-interaction")
@RequiredArgsConstructor
public class AIInteractionController {

    private final AIInteractionService aiInteractionService;

    @GetMapping("/get")
    public ResponseEntity<?> getAllAIInteractions() {
        return ResponseEntity.status(200).body(aiInteractionService.getAllAIInteractions());
    }

    @GetMapping("/get/{aiInteractionId}")
    public ResponseEntity<?> getAIInteractionById(@PathVariable Integer aiInteractionId) {
        return ResponseEntity.status(200).body(aiInteractionService.getAIInteractionById(aiInteractionId));
    }

    @PostMapping("/add/{userId}/{projectId}")
    public ResponseEntity<?> addAIInteraction(@PathVariable Integer userId, @PathVariable Integer projectId,
                                             @Valid @RequestBody AIInteraction aiInteraction) {
        aiInteractionService.addAIInteraction(userId, projectId, aiInteraction);
        return ResponseEntity.status(200).body(new ApiResponse("AI interaction added successfully"));
    }

    @PutMapping("/update/{aiInteractionId}")
    public ResponseEntity<?> updateAIInteraction(@PathVariable Integer aiInteractionId,
                                                @Valid @RequestBody AIInteraction aiInteraction) {
        aiInteractionService.updateAIInteraction(aiInteractionId, aiInteraction);
        return ResponseEntity.status(200).body(new ApiResponse("AI interaction updated successfully"));
    }

    @DeleteMapping("/delete/{aiInteractionId}")
    public ResponseEntity<?> deleteAIInteraction(@PathVariable Integer aiInteractionId) {
        aiInteractionService.deleteAIInteraction(aiInteractionId);
        return ResponseEntity.status(200).body(new ApiResponse("AI interaction deleted successfully"));
    }
}
