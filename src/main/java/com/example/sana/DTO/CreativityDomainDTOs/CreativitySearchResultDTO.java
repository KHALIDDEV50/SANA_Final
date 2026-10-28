
package com.example.sana.DTO.CreativityDomainDTOs;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class CreativitySearchResultDTO {

    // ID of the actual entity (Character, World, Location, etc.)
    private Integer id;

    // ID of the ProjectNode connected to the entity
    private Integer nodeId;

    // CHARACTER, WORLD, LOCATION, WORLD_RULE, STORY_EVENT
    private String type;

    // Display name or event title
    private String title;

    // Short description shown in search results
    private String description;

    // Position of the Node in the visual workspace
    private Double positionX;
    private Double positionY;
}
