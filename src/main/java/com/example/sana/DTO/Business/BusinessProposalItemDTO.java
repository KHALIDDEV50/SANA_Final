package com.example.sana.DTO.Business;

public record BusinessProposalItemDTO(
        Integer id,
        String operation,
        String targetType,
        Integer targetId,
        String beforeJson,
        String afterJson
) {
}