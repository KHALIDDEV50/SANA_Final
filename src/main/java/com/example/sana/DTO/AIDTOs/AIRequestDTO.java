package com.example.sana.DTO.AIDTOs;

import com.example.sana.Enums.AITask;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class AIRequestDTO {
    @NotNull(message = "AI task is required")
    private AITask task;

    @NotBlank(message = "AI prompt is required")
    @Size(max = 10000, message = "AI prompt must not exceed 10000 characters")
    private String prompt;

    @Size(max = 50, message = "Diagram type must not exceed 50 characters")
    private String diagramType;

    @Positive(message = "Output ID must be greater than zero")
    private Integer outputId;

    @PositiveOrZero(message = "Base revision cannot be negative")
    private Integer baseRevision;

    @Size(max = 100, message = "Select at most 100 elements")
    private List<@NotBlank(message = "Selected element key is required")
            @Pattern(regexp = "(node|edge):[1-9][0-9]*", message = "Selected elements must use node:<id> or edge:<id>") String> selectedElementIds;
}
