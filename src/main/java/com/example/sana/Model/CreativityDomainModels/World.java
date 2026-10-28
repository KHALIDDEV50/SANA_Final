package com.example.sana.Model.CreativityDomainModels;

import com.example.sana.Model.ProjectNode;
import jakarta.persistence.*;
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
public class World {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    @NotEmpty(message = "name cannot be null")
    @Column(columnDefinition = "varchar(50) not null")
    private String name;
    @NotEmpty(message = "description cannot be empty")
    @Column(columnDefinition = "text not null")
    private String description;
    @Column(columnDefinition = "text")
    private String history;
    @Column(columnDefinition = "text")
    private String lore;
    @Column(columnDefinition = "text")
    private String atmosphere;
    @Column(columnDefinition = "text")
    private String notes;
    @Column(columnDefinition = "date")
    private LocalDate createdAt;
    @Column(columnDefinition = "date")
    private LocalDate updatedAt;


    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_node_id")
    private ProjectNode projectNode;

    @OneToMany(mappedBy = "world")
    private Set<Location> locations;

    @OneToMany(mappedBy = "world")
    private Set<WorldRule> rules;


}
