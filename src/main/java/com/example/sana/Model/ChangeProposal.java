package com.example.sana.Model;

import com.example.sana.Enums.ProposalStatus;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.Set;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Entity
public class ChangeProposal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Positive(message = "Proposal ID must be greater than zero")
    private Integer id;

    @NotBlank(message = "Proposal title must not be blank")
    @Size(max = 255, message = "Proposal title must not exceed 255 characters")
    private String title;

    @Column(columnDefinition = "TEXT")
    @NotBlank(message = "Proposal description must not be blank")
    private String description;

    @NotNull(message = "Proposal status is required")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 255, columnDefinition = "varchar(255)")
    private ProposalStatus status;

    @NotNull(message = "Base version is required")
    @PositiveOrZero(message = "Base version must be zero or greater")
    private Integer baseVersion;

    @Column(name = "managed_request_id", length = 36, unique = true)
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private String managedRequestId;

    @Column(name = "proposed_graph_json", columnDefinition = "LONGTEXT")
    @JsonIgnore
    private String proposedGraphJson;

    @Column(name = "output_id")
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Integer outputId;

    @Column(name = "base_revision")
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Integer baseRevision;

    @Column(name = "applied_output_id")
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Integer appliedOutputId;

    @Column(name = "applied_revision")
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Integer appliedRevision;

    @Column(name = "applied_mapping_json", columnDefinition = "LONGTEXT")
    @JsonIgnore
    private String appliedMappingJson;

    @CreationTimestamp
    @PastOrPresent(message = "Proposal creation time must not be in the future")
    private LocalDateTime createdAt;

    @PastOrPresent(message = "Proposal review time must not be in the future")
    private LocalDateTime reviewedAt;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "project_id", referencedColumnName = "id", nullable = false)
    @JsonIgnore
    private Project project;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ai_interaction_id", referencedColumnName = "id")
    @JsonIgnore
    private AIInteraction aiInteraction;

    @OneToMany(mappedBy = "changeProposal")
    private Set<ChangeProposalItem> items;
    public ChangeProposal(Integer id, String title, String description, ProposalStatus status, Integer baseVersion,
                          LocalDateTime createdAt, LocalDateTime reviewedAt, Project project,
                          AIInteraction aiInteraction, Set<ChangeProposalItem> items) {
        this.id = id; this.title = title; this.description = description; this.status = status;
        this.baseVersion = baseVersion; this.createdAt = createdAt; this.reviewedAt = reviewedAt;
        this.project = project; this.aiInteraction = aiInteraction; this.items = items;
    }

}
