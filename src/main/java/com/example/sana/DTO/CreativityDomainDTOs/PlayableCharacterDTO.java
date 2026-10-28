package com.example.sana.DTO.CreativityDomainDTOs;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PlayableCharacterDTO {

    @NotBlank(message = "Character name cannot be empty")
    @Size(max = 100)
    private String name;

    @Size(max = 100)
    private String role;

    // Optional equipment
    @Size(max = 100)
    private String weaponName;

    @Size(max = 100)
    private String weaponType;

    private String strengths;
    private String weaknesses;

    // Optional gameplay stats
    @Min(0) @Max(10)
    private Integer attack;

    @Min(0) @Max(10)
    private Integer defense;

    @Min(0) @Max(10)
    private Integer speed;

    @Min(0) @Max(10)
    private Integer rangeStat;

    @Min(0) @Max(10)
    private Integer stamina;

    @Min(0) @Max(10)
    private Integer special;
}
