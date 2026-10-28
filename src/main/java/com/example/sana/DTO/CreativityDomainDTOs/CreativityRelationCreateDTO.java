package com.example.sana.DTO.CreativityDomainDTOs;

import com.example.sana.Enums.CreativityDomainEnums.CreativityRelationType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreativityRelationCreateDTO {

    // Selected relationship type
    @NotNull(message = "Relationship type cannot be null")
    private CreativityRelationType type;

    // Optional label for predefined relationships
    // Required when type is CUSTOM
    @Size(max = 50, message = "Label cannot exceed 50 characters")
    private String label;
}
