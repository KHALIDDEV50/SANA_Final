package com.example.sana.DTO.AIDTOs;

import com.example.sana.DTO.GraphDefinitionDTO;
import com.example.sana.Enums.AITask;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

// Stored by Spring Boot now; the future n8n dispatcher will send this exact snapshot.
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AIWorkflowRequestDTO {
    private Integer schemaVersion;
    private String requestId;
    private String domain;
    private AITask task;
    private Integer projectId;
    private Integer outputId;
    private String diagramType;
    private Integer baseRevision;
    private String prompt;
    private List<String> selectedElementIds;
    private Map<String, Object> context;
    private List<Object> conversationHistory;
    private GraphDefinitionDTO currentGraph;
}
