package com.example.sana.Model;

import com.example.sana.Enums.ExecutionStatus;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
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
public class ToolRun {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Positive(message = "Tool run ID must be greater than zero")
    private Integer id;

    @NotBlank(message = "Tool type must not be blank")
    @Size(max = 255, message = "Tool type must not exceed 255 characters")
    private String toolType;

    @Column(columnDefinition = "LONGTEXT")
    @NotBlank(message = "Tool input JSON must not be blank")
    private String inputJson;

    @Column(columnDefinition = "LONGTEXT")
    private String outputJson;

    @NotNull(message = "Tool run status is required")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 255, columnDefinition = "varchar(255)")
    private ExecutionStatus status;

    @CreationTimestamp
    @PastOrPresent(message = "Tool run creation time must not be in the future")
    private LocalDateTime createdAt;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", referencedColumnName = "id", nullable = false)
    @JsonIgnore
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "project_id", referencedColumnName = "id", nullable = false)
    @JsonIgnore
    private Project project;
}
