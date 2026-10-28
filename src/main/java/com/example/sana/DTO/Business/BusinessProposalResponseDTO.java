package com.example.sana.DTO.Business;

import java.time.LocalDateTime;

public record BusinessProposalResponseDTO(
        Integer proposalId,
        Integer projectId,
        String title,
        String description,
        String status,
        LocalDateTime createdAt
) {
}