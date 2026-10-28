package com.example.sana.DTO.CreativityDomainDTOs;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BranchingChoiceDTO {

    @NotBlank(message = "Choice text cannot be empty")
    private String text;

    @NotNull(message = "Display order cannot be null")
    @PositiveOrZero
    private Integer displayOrder;
}
