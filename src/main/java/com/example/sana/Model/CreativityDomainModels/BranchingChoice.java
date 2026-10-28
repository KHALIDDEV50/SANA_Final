package com.example.sana.Model.CreativityDomainModels;

import com.example.sana.Enums.CreativityDomainEnums.BranchingComparisonOperator;
import com.example.sana.Enums.CreativityDomainEnums.BranchingEffectOperator;
import com.example.sana.Model.ProjectEdge;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
public class BranchingChoice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @NotBlank(message = "Choice text cannot be empty")
    @Column(columnDefinition = "text not null")
    private String text;

    @NotNull(message = "Display order cannot be null")
    @PositiveOrZero
    @Column(nullable = false)
    private Integer displayOrder;


    // ProjectEdge stores sourceNode and targetNode
    // BranchingChoice stores only the choice-specific details
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "project_edge_id",
            nullable = false,
            unique = true
    )
    @JsonIgnore
    private ProjectEdge projectEdge;



    // Optional condition controlling whether the choice appears
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "condition_variable_id")
    @JsonIgnore
    private BranchingVariable conditionVariable;

    @Enumerated(EnumType.STRING)
    private BranchingComparisonOperator conditionOperator;

    private Integer conditionValue;


    // Optional effect applied after choosing this option
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "effect_variable_id")
    @JsonIgnore
    private BranchingVariable effectVariable;

    @Enumerated(EnumType.STRING)
    private BranchingEffectOperator effectOperator;

    private Integer effectValue;



}
