package com.example.sana.DTO.DiagramDTOs;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DiagramRequestDTO {

    @NotBlank(message = "Diagram title is required")
    @Size(max = 255, message = "Diagram title must not exceed 255 characters")
    private String title;

    // Required when creating through /api/v1/diagram, ignored on update.
    // The Architecture endpoints set it themselves.
    @Size(max = 50, message = "Diagram type must not exceed 50 characters")
    private String diagramType;
}
