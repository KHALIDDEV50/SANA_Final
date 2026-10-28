package com.example.sana.DTO.CreativityDomainDTOs;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
public class CreativityDeletePreviewDTO {

    // Node selected for deletion
    private Integer nodeId;
    private String nodeType;

    // Number of connected ProjectEdges
    private Integer affectedRelationsCount;

    // Relationships that may be removed with the Node
    private List<CreativityRelationDTO> affectedRelations;

    // Whether known structural relationships allow deletion
    private Boolean canDelete;

    // Reasons why deletion may be blocked
    private List<String> blockingReasons;
}
