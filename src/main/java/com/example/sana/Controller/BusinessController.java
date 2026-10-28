package com.example.sana.Controller;

import com.example.sana.Service.OwnershipService;
import com.example.sana.Model.User;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import com.example.sana.DTO.Business.BusinessToolRequestDTO;
import com.example.sana.DTO.AIDTOs.AIRunResponseDTO;
import com.example.sana.Service.Business.BusinessService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/business")
@RequiredArgsConstructor
public class BusinessController {
    private final BusinessService businessService;
    private final OwnershipService ownershipService;
    @PostMapping("/tools")
    public ResponseEntity<AIRunResponseDTO> executeTool(@AuthenticationPrincipal User user, @Valid @RequestBody BusinessToolRequestDTO request) {
        ownershipService.checkProject(user, request.projectId());

        return ResponseEntity.accepted().body(businessService.executeTool(request));
    }
}
