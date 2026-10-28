package com.example.sana.DTO.CreativityDomainDTOs;

import com.example.sana.Enums.CreativityDomainEnums.BranchingComparisonOperator;
import com.example.sana.Enums.CreativityDomainEnums.BranchingEffectOperator;

public record BranchingChoiceSettingsDTO(

        Integer choiceId,
        Integer sourceNodeId,
        Integer targetNodeId,

        String text,
        Integer displayOrder,

        // Optional condition
        Integer conditionVariableId,
        String conditionVariableName,
        BranchingComparisonOperator conditionOperator,
        Integer conditionValue,

        // Optional effect
        Integer effectVariableId,
        String effectVariableName,
        BranchingEffectOperator effectOperator,
        Integer effectValue
) {}
