package com.example.sana.DTO.CreativityDomainDTOs;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class BranchingReferenceDTO {

    private Integer branchingNodeId;

    // Speaker information
    private Integer characterId;
    private String characterName;

    // Related event information
    private Integer storyEventId;
    private String storyEventTitle;
}
