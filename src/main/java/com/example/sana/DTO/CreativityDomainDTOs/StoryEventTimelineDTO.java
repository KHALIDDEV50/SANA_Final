
package com.example.sana.DTO.CreativityDomainDTOs;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class StoryEventTimelineDTO {

    private Integer id;
    private Integer nodeId;

    private String title;
    private String description;
    private String eventType;

    // Narrative order
    private Integer presentationOrder;

    // Actual chronological order
    private Integer chronologicalOrder;

    // Fictional date or time displayed to the writer
    private String inWorldTime;
}
