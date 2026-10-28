package com.example.sana.DTO.CreativityDomainDTOs;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class WorldDTO {

    // ProjectNode fields
    @NotNull(message = "Position X cannot be null")
    private Double positionX;

    @NotNull(message = "Position Y cannot be null")
    private Double positionY;

    // World fields
    @NotEmpty(message = "Name cannot be empty")
    private String name;

    @NotEmpty(message = "Description cannot be empty")
    private String description;

    private String history;
    private String lore;
    private String atmosphere;
    private String notes;
}
