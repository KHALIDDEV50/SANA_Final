package com.example.sana.Controller;

import com.example.sana.DTO.AIDTOs.QuickStartRequestDTO;
import com.example.sana.DTO.AIDTOs.QuickStartSuggestionDTO;
import com.example.sana.Service.AIServices.QuickStartService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

// "Start with SANA AI": suggest a domain and diagram for an idea before any project exists.
// Any logged-in user may call it; it reads no project data.
@RestController
@RequestMapping("/api/v1/ai/quick-start")
@RequiredArgsConstructor
public class QuickStartController {

    private final QuickStartService quickStartService;

    // Ask the AI which domain and diagram type fit the idea
    @PostMapping("/classify")
    public ResponseEntity<QuickStartSuggestionDTO> classify(@Valid @RequestBody QuickStartRequestDTO request) {
        return ResponseEntity.status(200).body(quickStartService.classify(request));
    }
}
