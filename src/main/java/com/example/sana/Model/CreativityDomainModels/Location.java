package com.example.sana.Model.CreativityDomainModels;

import com.example.sana.Model.ProjectNode;
import jakarta.persistence.*;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotEmpty;
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
public class Location {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    @NotEmpty(message = "name cannot be empty")
    @Column(columnDefinition = "varchar(50) not null")
    private String name;
    @Column(columnDefinition = "varchar(50)")
    private String locationType;
    @Column(columnDefinition = "text")
    private String narrativeSignificance;
    @Column(columnDefinition = "text")
    private String notes;
    @Column(columnDefinition = "date")
    private LocalDate createdAt;
    @Column(columnDefinition = "date")
    private LocalDate updateAt;


    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_node_id")
    private ProjectNode projectNode;

    // Keep world metadata in JSON, but avoid recursively expanding all its locations and rules.
    @ManyToOne(fetch = FetchType.LAZY)
    @JsonIgnoreProperties({"locations", "rules"})
    @JoinColumn(name = "world_id")
    private World world;

    // Parent can be shown, but do not expand its children back into this location.
    @ManyToOne(fetch = FetchType.LAZY)
    @JsonIgnoreProperties("childLocations")
    @JoinColumn(name = "parent_location_id")
    private Location parentLocation;

    @OneToMany(mappedBy = "parentLocation")
    private Set<Location> childLocations;

}
