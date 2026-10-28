package com.example.sana.DTO.CreativityDomainDTOs;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.Map;

@Getter
@Setter
public class BranchingPlayStateDTO {

    // Current game state sent by the frontend
    // Example: {"hasRoyalKey": 1, "reputation": 10}
    @NotNull
    private Map<String, Integer> variables;
}
