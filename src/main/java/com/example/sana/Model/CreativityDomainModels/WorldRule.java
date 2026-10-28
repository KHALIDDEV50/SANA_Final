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

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
public class WorldRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    @NotEmpty(message = "name cannot be empty")
    @Column(columnDefinition = "varchar(50) not null")
    private String name;
    @NotEmpty(message = "description cannot be empty")
    @Column(columnDefinition = "text not null")
    private String description;
    @Column(columnDefinition = "text")
    private String scope;
    @Column(columnDefinition = "text")
    private String conditions;
    @Column(columnDefinition = "text")
    private String exceptions;
    @Column(columnDefinition = "text")
    private String consequences;
    @Column(columnDefinition = "text")
    private String notes;
    @Column(columnDefinition = "date")
    private LocalDate createdAt;
    @Column(columnDefinition = "date")
    private LocalDate updatedAt;



    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_node_id")
    private ProjectNode projectNode;

    // Include linked world metadata without recursively returning its rules/locations.
    @ManyToOne(fetch = FetchType.LAZY)
    @JsonIgnoreProperties({"locations", "rules"})
    @JoinColumn(name = "world_id")
    private World world;

}
