package com.example.sana.DTO.CreativityDomainDTOs;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class StoryEventDTO {

    // ProjectNode fields
    @NotNull(message = "Position X cannot be null")
    private Double positionX;

    @NotNull(message = "Position Y cannot be null")
    private Double positionY;

    // StoryEvent fields
    @NotEmpty(message = "Title cannot be empty")
    private String title;

    @NotEmpty(message = "Description cannot be empty")
    private String description;

    private String eventType;

    @NotNull(message = "Presentation order cannot be null")
    private Integer presentationOrder;


    // Optional chronological position of the event
    @Positive(message = "Chronological order must be positive")
    private Integer chronologicalOrder;


    private String inWorldTime;
    private String notes;
}

