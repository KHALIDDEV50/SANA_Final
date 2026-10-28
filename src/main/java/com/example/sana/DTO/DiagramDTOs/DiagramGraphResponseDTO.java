package com.example.sana.DTO.DiagramDTOs;

import com.example.sana.DTO.GraphDefinitionDTO;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class DiagramGraphResponseDTO {
    private Integer outputId;
    private Integer graphRevision;
    private GraphDefinitionDTO graph;
}
