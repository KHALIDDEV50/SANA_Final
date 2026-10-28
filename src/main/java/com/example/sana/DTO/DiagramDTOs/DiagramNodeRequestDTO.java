package com.example.sana.DTO.DiagramDTOs;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.Map;

@Getter
@Setter
public class DiagramNodeRequestDTO {

    @NotBlank(message = "Node type is required")
    @Size(max = 50, message = "Node type must not exceed 50 characters")
    private String type;

    @NotBlank(message = "Node label is required")
    @Size(max = 255, message = "Node label must not exceed 255 characters")
    private String label;

    @NotNull(message = "Node position X is required")
    private Double positionX;

    @NotNull(message = "Node position Y is required")
    private Double positionY;

    // Extra node properties such as description; stored with the label
    private Map<String, Object> data;
}
