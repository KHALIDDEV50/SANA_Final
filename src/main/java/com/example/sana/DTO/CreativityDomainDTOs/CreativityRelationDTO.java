package com.example.sana.DTO.CreativityDomainDTOs;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class CreativityRelationDTO {

    private Integer edgeId;
    private Integer sourceNodeId;
    private Integer targetNodeId;
    private String type;
    private String label;
}
