package com.example.sana.Controller.IntegrationControllers;

import com.example.sana.DTO.IntegrationDTOs.N8nCallbackDTO;
import com.example.sana.Service.IntegrationServices.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/integrations/n8n/ai")
@RequiredArgsConstructor
public class N8nCallbackController {
    private final N8nCallbackService callbacks;
    private final AIJson json;

    @PostMapping(value = "/callback", consumes = "application/json")
    public ResponseEntity<?> callback(@RequestBody String body) {
        // Authentication and byte limits run in the dedicated security filter before body parsing.
        boolean accepted = callbacks.receive(json.read(body, N8nCallbackDTO.class));
        return accepted ? ResponseEntity.noContent().build() : ResponseEntity.status(409)
                .body(new com.example.sana.API.ApiResponse("Late callback rejected; request has timed out"));
    }
}
