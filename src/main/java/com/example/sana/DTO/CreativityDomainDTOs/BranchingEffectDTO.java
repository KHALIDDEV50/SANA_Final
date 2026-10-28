package com.example.sana.DTO.CreativityDomainDTOs;

import com.example.sana.Enums.CreativityDomainEnums.BranchingEffectOperator;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BranchingEffectDTO {

    @NotNull
    private BranchingEffectOperator operator;

    @NotNull
    private Integer value;
}
