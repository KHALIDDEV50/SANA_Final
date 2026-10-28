package com.example.sana.Model;

import com.example.sana.Enums.ChangeOperation;
import com.example.sana.Enums.ChangeTargetType;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Entity
public class ChangeProposalItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @NotNull(message = "Change operation is required")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 255, columnDefinition = "varchar(255)")
    private ChangeOperation operation;

    @NotNull(message = "Change target type is required")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 255, columnDefinition = "varchar(255)")
    private ChangeTargetType targetType;

    @Positive(message = "Change target ID must be greater than zero")
    private Integer targetId;

    @Column(columnDefinition = "LONGTEXT")
    private String beforeJson;

    @Column(columnDefinition = "LONGTEXT")
    private String afterJson;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "change_proposal_id", referencedColumnName = "id", nullable = false)
    @JsonIgnore
    private ChangeProposal changeProposal;
}
