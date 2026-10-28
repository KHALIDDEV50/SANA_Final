package com.example.sana.DTO.DiagramDTOs;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.Map;

@Getter
@Setter
public class DiagramEdgeRequestDTO {

    @NotBlank(message = "Edge type is required")
    @Size(max = 50, message = "Edge type must not exceed 50 characters")
    private String type;

    // Optional; the edge type is used when it is empty
    @Size(max = 50, message = "Edge label must not exceed 50 characters")
    private String label;

    @NotNull(message = "Source node ID is required")
    @Positive(message = "Source node ID must be greater than zero")
    private Integer sourceNodeId;

    @NotNull(message = "Target node ID is required")
    @Positive(message = "Target node ID must be greater than zero")
    private Integer targetNodeId;

    // Extra edge properties such as description
    private Map<String, Object> data;
}
