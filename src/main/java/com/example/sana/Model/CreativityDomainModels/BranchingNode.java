package com.example.sana.Model.CreativityDomainModels;

import com.example.sana.Enums.CreativityDomainEnums.BranchingNodeType;
import com.example.sana.Model.ProjectNode;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
public class BranchingNode {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @NotBlank(message = "Title cannot be empty")
    @Column(nullable = false)
    private String title;

    @NotBlank(message = "Content cannot be empty")
    @Column(columnDefinition = "text not null")
    private String content;

    @NotNull(message = "Node type cannot be null")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private BranchingNodeType type;

    private LocalDate createdAt;
    private LocalDate updatedAt;


    // Connect the branching scene to its visual ProjectNode
    // ProjectNode stores positionX, positionY and ProjectOutput
    // Each BranchingNode must have exactly one ProjectNode
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "project_node_id",
            nullable = false,
            unique = true
    )
    @JsonIgnore
    private ProjectNode projectNode;


    // Optional character speaking in this branching scene
    // Reuses an existing CharacterProfile from the project
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "speaker_character_id")
    @JsonIgnore
    private CharacterProfile speaker;


    // Optional StoryEvent associated with this scene
    // Multiple scenes can reference the same StoryEvent
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "related_story_event_id")
    @JsonIgnore
    private StoryEvent relatedStoryEvent;

}
