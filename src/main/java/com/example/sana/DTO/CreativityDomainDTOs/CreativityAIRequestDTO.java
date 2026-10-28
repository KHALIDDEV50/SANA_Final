package com.example.sana.DTO.CreativityDomainDTOs;

import com.example.sana.Enums.AITask;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;
import java.util.List;

/** Creativity-only AI request: the shared AIRequestDTO is deliberately unchanged. */
@Getter
@Setter
public class CreativityAIRequestDTO {
    @NotNull
    private AITask task;

    @NotBlank
    @Size(max = 10000)
    private String prompt;

    @Size(max = 50)
    private String diagramType;

    @Positive
    private Integer outputId;

    @PositiveOrZero
    private Integer baseRevision;

    @Size(max = 100)
    private List<@NotBlank String> selectedElementIds;

    /** The user supplies only conversation turns from the currently open Creativity panel. */
    @Size(max = 12)
    private List<@Valid ConversationTurn> conversationHistory;

    public record ConversationTurn(
            @NotBlank @Pattern(regexp = "user|assistant") String role,
            @NotBlank @Size(max = 1500) String content
    ) {}
}
