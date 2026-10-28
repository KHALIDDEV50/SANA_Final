package com.example.sana.DTO.CreativityDomainDTOs;

import java.util.List;

public record CharacterBalanceReportDTO(

        Integer outputId,
        Integer totalCharacters,
        Integer analyzedCharacters,
        Double averagePoints,

        // Overall notes about the roster
        List<String> notes,

        // Balance results for each character with complete stats
        List<CharacterResult> characters

) {

    public record CharacterResult(
            Integer characterId,
            String name,
            Integer totalPoints,
            Double differenceFromAverage,
            List<String> warnings
    ) {}
}
