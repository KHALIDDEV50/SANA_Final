package com.example.sana.DTO.CreativityDomainDTOs;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BranchingPositionDTO {

    // Horizontal position on the canvas
    @NotNull(message = "Position X cannot be null")
    private Double positionX;

    // Vertical position on the canvas
    @NotNull(message = "Position Y cannot be null")
    private Double positionY;
}
