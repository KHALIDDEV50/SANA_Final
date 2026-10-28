package com.example.sana.DTO.CreativityDomainDTOs;

import java.util.Map;

public record BranchingGamePreviewDTO(

        BranchingPlayDTO scene,

        // Variable values after the current choice
        Map<String, Integer> variables,

        // True if the scene has choices, but none are available
        boolean blocked
) {}
