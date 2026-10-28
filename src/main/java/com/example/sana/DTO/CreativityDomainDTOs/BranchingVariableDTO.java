package com.example.sana.DTO.CreativityDomainDTOs;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BranchingVariableDTO {

    // Use stable names suitable for game scripting
    @Pattern(
            regexp = "[A-Za-z][A-Za-z0-9_]{0,39}",
            message = "Variable name must start with a letter"
    )
    @NotNull
    private String name;

    // Starting value for a new Preview
    @NotNull
    private Integer initialValue;
}
