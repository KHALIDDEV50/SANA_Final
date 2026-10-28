package com.example.sana.Model.CreativityDomainModels;

import com.example.sana.Model.ProjectOutput;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(
        name = "branching_variables",
        uniqueConstraints = @UniqueConstraint(
                columnNames = {"project_output_id", "variable_name"}
        )
)
public class BranchingVariable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    // Unique variable name inside the selected output
    @NotBlank
    @Column(name = "variable_name", nullable = false, length = 40)
    private String name;

    // Value used when starting a new game preview
    @NotNull
    @Column(nullable = false)
    private Integer initialValue;

    // A variable belongs to one branching ProjectOutput
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "project_output_id", nullable = false)
    @JsonIgnore
    private ProjectOutput projectOutput;
}
