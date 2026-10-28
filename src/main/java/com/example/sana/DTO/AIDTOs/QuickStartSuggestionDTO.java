package com.example.sana.DTO.AIDTOs;

import lombok.AllArgsConstructor;
import lombok.Getter;

// The AI's suggestion for a quick start, checked against the supported diagram types.
// category is the frontend's domain (TECHNOLOGY, BUSINESS, EDUCATION, WRITING or GAMES);
// domain is the stored project domain (TECH, BUSINESS, EDUCATION or CREATIVITY).
@Getter
@AllArgsConstructor
public class QuickStartSuggestionDTO {
    private String category;
    private String domain;
    private String diagramType;
    private String projectName;
    private String reason;
}
