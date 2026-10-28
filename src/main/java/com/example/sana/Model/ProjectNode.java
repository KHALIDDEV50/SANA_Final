package com.example.sana.Model;

import com.example.sana.Model.CreativityDomainModels.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.Set;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
public class ProjectNode {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    @NotEmpty(message = "Type cannot be empty")
    @Column(columnDefinition = "varchar(50) not null")
    private String type;

    @Column(columnDefinition = "LONGTEXT")
    private String dataJson;
    @NotNull(message = "position X cannot be null")
    @Column(columnDefinition = "double not null")
    private Double positionX;
    @NotNull(message = "position Y cannot be null")
    @Column(columnDefinition = "double not null")
    private Double positionY;
    @Column(columnDefinition = "date")
    private LocalDate createdAt;
    @Column(columnDefinition = "date")
    private LocalDate updatedAt;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "project_output_id", referencedColumnName = "id", nullable = false)
    @JsonIgnore
    private ProjectOutput projectOutput;

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "sourceNode")
    @JsonIgnore
    private Set<ProjectEdge> outgoingEdges;

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "targetNode")
    @JsonIgnore
    private Set<ProjectEdge> incomingEdges;


    @OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL, mappedBy = "projectNode")
    @JsonIgnore
    private CharacterProfile characterProfile;

    @OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL,mappedBy = "projectNode")
    @JsonIgnore
    private World world;

    @OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL,mappedBy = "projectNode")
    @JsonIgnore
    private Location location;

    @OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL,mappedBy = "projectNode")
    @JsonIgnore
    private WorldRule worldRule;

    @OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL,mappedBy = "projectNode")
    @JsonIgnore
    private StoryEvent storyEvent;



}
