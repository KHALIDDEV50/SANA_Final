package com.example.sana.DTO.CreativityDomainDTOs;

import com.example.sana.Enums.CreativityDomainEnums.BranchingNodeType;

import java.util.List;

public record BranchingMapDTO(
        List<NodeItem> nodes,
        List<ChoiceItem> choices
) {

    // One scene displayed on the visual canvas
    public record NodeItem(
            Integer branchingNodeId,
            Integer projectNodeId,
            String title,
            String content,
            BranchingNodeType type,
            Double positionX,
            Double positionY
    ) {}


    // One connection displayed between two scenes
    public record ChoiceItem(
            Integer choiceId,
            Integer edgeId,
            Integer sourceBranchingNodeId,
            Integer targetBranchingNodeId,
            String text,
            Integer displayOrder
    ) {}
}
