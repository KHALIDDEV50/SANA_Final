package com.example.sana.DTO.AIDTOs;

import com.example.sana.Enums.ExecutionStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import com.example.sana.DTO.IntegrationDTOs.N8nCallbackDTO;
import com.example.sana.Enums.AIDispatchStatus;

import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
public class AIRunResponseDTO {
    private Integer interactionId;
    private String requestId;
    private Integer projectId;
    private Integer outputId;
    private Integer baseRevision;
    private String task;
    private ExecutionStatus status;
    private String modelName;
    private LocalDateTime createdAt;
    // Safe request metadata lets the owner resume a clarification after reloading.
    // Never include webhook URLs, transport keys, or callback credentials here.
    private String prompt;
    private String diagramType;
    private String businessToolType;
    private Object businessInput;
    private String statusUrl;
    private String executionId;
    private AIDispatchStatus dispatchStatus;
    private LocalDateTime acceptedAt;
    private LocalDateTime completedAt;
    private LocalDateTime deadlineAt;
    private Integer proposalId;
    private String proposalReviewStatus;
    private N8nCallbackDTO.Result result;
    private N8nCallbackDTO.Failure error;

    public AIRunResponseDTO(Integer interactionId, String requestId, Integer projectId, Integer outputId,
                            Integer baseRevision, String task, ExecutionStatus status, String modelName, LocalDateTime createdAt) {
        this.interactionId = interactionId; this.requestId = requestId; this.projectId = projectId; this.outputId = outputId;
        this.baseRevision = baseRevision; this.task = task; this.status = status; this.modelName = modelName; this.createdAt = createdAt;
    }

}
