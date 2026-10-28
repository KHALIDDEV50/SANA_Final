
package com.example.sana.DTO.CreativityDomainDTOs;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CharacterProfileDTO {

    // ProjectNode fields
    @NotNull(message = "Position X cannot be null")
    private Double positionX;

    @NotNull(message = "Position Y cannot be null")
    private Double positionY;

    // CharacterProfile fields
    @NotEmpty(message = "Name cannot be empty")
    private String name;

    @NotEmpty(message = "Description cannot be empty")
    private String shortDescription;

    @NotEmpty(message = "Role cannot be empty")
    private String role;

    private String aliases;

    @NotNull(message = "Age cannot be null")
    @Positive(message = "Age must be positive")
    private Integer age;

    private String appearance;
    private String traits;
    private String strengths;
    private String weaknesses;
    private String skills;
    private String history;
    private String goals;
    private String fears;
    private String innerConflict;
    private String secrets;
    private String characterValues;
    private String characterArc;
    private String writerNotes;
}
