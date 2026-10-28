package com.example.sana.Model;

import com.example.sana.Enums.ExecutionStatus;
import com.example.sana.Enums.AIDispatchStatus;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Entity
public class AIInteraction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Positive(message = "AI interaction ID must be greater than zero")
    private Integer id;

    @NotBlank(message = "AI action type must not be blank")
    @Size(max = 255, message = "AI action type must not exceed 255 characters")
    private String actionType;

    @Column(columnDefinition = "TEXT")
    @NotBlank(message = "AI prompt summary must not be blank")
    private String promptSummary;

    @Column(columnDefinition = "LONGTEXT")
    private String responseJson;

    @Pattern(regexp = "(?s).*\\S.*", message = "AI model name must not be blank")
    @Size(max = 255, message = "AI model name must not exceed 255 characters")
    private String modelName;

    @NotNull(message = "AI interaction status is required")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 255, columnDefinition = "varchar(255)")
    private ExecutionStatus status;

    // Nullable for legacy interactions. Non-null requestId identifies a managed AI run.
    @Column(name = "request_id", length = 36, unique = true)
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private String requestId;

    // Immutable authoritative request, captured before any AI execution.
    @Column(name = "request_json", columnDefinition = "LONGTEXT")
    @JsonIgnore
    private String requestJson;

    // Scalar identity retains the target in run history even if the output is later deleted.
    @Column(name = "output_id")
    @Positive(message = "AI output ID must be greater than zero")
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Integer outputId;

    @Column(name = "base_revision")
    @PositiveOrZero(message = "AI base revision cannot be negative")
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Integer baseRevision;

    @Column(name = "execution_id", length = 255)
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private String executionId;

    @Enumerated(EnumType.STRING)
    @Column(name = "dispatch_status", length = 32)
    @JsonIgnore
    private AIDispatchStatus dispatchStatus;

    @Column(name = "dispatch_attempts")
    @JsonIgnore
    private Integer dispatchAttempts;

    @Column(name = "next_dispatch_at")
    @JsonIgnore
    private LocalDateTime nextDispatchAt;

    @Column(name = "dispatch_lease_until")
    @JsonIgnore
    private LocalDateTime dispatchLeaseUntil;

    @Column(name = "deadline_at")
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private LocalDateTime deadlineAt;

    @Column(name = "accepted_at")
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private LocalDateTime acceptedAt;

    @Column(name = "completed_at")
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private LocalDateTime completedAt;

    @Column(name = "callback_hash", length = 64)
    @JsonIgnore
    private String callbackHash;

    @Column(name = "error_code", length = 64)
    @JsonIgnore
    private String errorCode;

    @Column(name = "error_message", length = 512)
    @JsonIgnore
    private String errorMessage;

    @Column(name = "proposal_id")
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Integer proposalId;

    @CreationTimestamp
    @PastOrPresent(message = "AI interaction creation time must not be in the future")
    private LocalDateTime createdAt;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", referencedColumnName = "id", nullable = false)
    @JsonIgnore
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "project_id", referencedColumnName = "id", nullable = false)
    @JsonIgnore
    private Project project;

    @AssertTrue(message = "Completed AI interactions require a model name")
    @JsonIgnore
    public boolean isCompletedModelKnown() {
        return status != ExecutionStatus.COMPLETED || (modelName != null && !modelName.isBlank());
    }

    // Preserve the constructor used by existing domain code and tests.
    public AIInteraction(Integer id, String actionType, String promptSummary, String responseJson,
                         String modelName, ExecutionStatus status, LocalDateTime createdAt, User user, Project project) {
        this.id = id;
        this.actionType = actionType;
        this.promptSummary = promptSummary;
        this.responseJson = responseJson;
        this.modelName = modelName;
        this.status = status;
        this.createdAt = createdAt;
        this.user = user;
        this.project = project;
    }

}
