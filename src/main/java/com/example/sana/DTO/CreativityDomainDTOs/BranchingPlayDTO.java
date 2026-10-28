
package com.example.sana.DTO.CreativityDomainDTOs;

import com.example.sana.Enums.CreativityDomainEnums.BranchingNodeType;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class BranchingPlayDTO {

    private Integer branchingNodeId;
    private Integer projectNodeId;
    private String title;
    private String content;
    private BranchingNodeType type;

    private Boolean ending;
    private Boolean deadEnd;

    private List<ChoiceOption> choices;


    @Getter
    @AllArgsConstructor
    public static class ChoiceOption {

        private Integer choiceId;
        private String text;
        private Integer targetBranchingNodeId;
        private Integer targetProjectNodeId;
    }
}
