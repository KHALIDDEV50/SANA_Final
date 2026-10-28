
package com.example.sana.DTO.CreativityDomainDTOs;

import com.example.sana.Enums.CreativityDomainEnums.BranchingNodeType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BranchingNodeDTO {

    @NotBlank(message = "Title cannot be empty")
    private String title;

    @NotBlank(message = "Content cannot be empty")
    private String content;

    @NotNull(message = "Node type cannot be null")
    private BranchingNodeType type;

    // Position of the scene on the visual canvas
    @NotNull(message = "Position X cannot be null")
    private Double positionX;

    @NotNull(message = "Position Y cannot be null")
    private Double positionY;
}
