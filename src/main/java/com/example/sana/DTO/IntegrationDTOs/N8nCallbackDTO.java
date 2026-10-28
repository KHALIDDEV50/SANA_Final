package com.example.sana.DTO.IntegrationDTOs;

import com.example.sana.DTO.GraphDefinitionDTO;
import java.util.List;

public record N8nCallbackDTO(Integer schemaVersion, String requestId, String executionId,
                             CallbackStatus status, Result result, Failure error) {
    public enum CallbackStatus { COMPLETED, FAILED }
    public enum ResultType { ADVICE, CLARIFICATION, PROPOSAL }
    public record Result(ResultType type, String message, String modelName, Clarification clarification, Proposal proposal) {}
    public record Clarification(String question, List<String> suggestedAnswers) {}
    public record Proposal(String title, String description, GraphDefinitionDTO proposedGraph) {}
    public record Failure(String code, String message, List<String> details) {}
}
