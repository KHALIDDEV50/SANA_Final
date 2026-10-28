package com.example.sana.DTO.DiagramDTOs;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

// Describes one supported diagram type so the frontend can build its palette
@Getter
@AllArgsConstructor
public class DiagramTypeDTO {
    private String domain;
    private String diagramType;
    private List<String> nodeTypes;
    private List<String> edgeTypes;
    private String rulesSummary;
}
