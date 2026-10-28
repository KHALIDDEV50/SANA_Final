package com.example.sana.DTO.AIDTOs;

import com.example.sana.DTO.GraphDefinitionDTO;
import com.example.sana.Enums.*;
import java.time.LocalDateTime;
import java.util.List;

public record AIProposalResponseDTO(Integer proposalId, String requestId, String title, String description,
        Integer outputId, Integer baseRevision, String reviewStatus, Integer appliedOutputId,
        GraphDefinitionDTO proposedGraph, List<Change> changes, LocalDateTime createdAt, LocalDateTime reviewedAt) {
    public record Change(ChangeOperation operation, ChangeTargetType targetType, Integer targetId, Object before, Object after) {}
}
