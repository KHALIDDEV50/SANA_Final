package com.example.sana.DTO.AIDTOs;

import java.util.Map;
public record AIProposalAcceptanceDTO(Integer proposalId, String status, Integer outputId, Integer graphRevision, Map<String, Integer> elementIds) {}
