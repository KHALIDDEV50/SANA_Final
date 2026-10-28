
package com.example.sana.DTO.CreativityDomainDTOs;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LocationDTO {

    // ProjectNode
    @NotNull(message = "Position X cannot be null")
    private Double positionX;

    @NotNull(message = "Position Y cannot be null")
    private Double positionY;

    // Location
    @NotEmpty(message = "Name cannot be empty")
    private String name;

    private String locationType;
    private String narrativeSignificance;
    private String notes;
}
