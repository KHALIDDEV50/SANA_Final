package com.example.sana.Model;

import com.example.sana.Enums.ProjectStatus;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDateTime;
import java.util.Set;

@Entity
@Table(name = "projects")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor

public class Project {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @NotEmpty(message = "Name is required")
    @Size(max = 100, message = "Name must not exceed 100 characters")
    @Column(nullable = false, length = 100)
    private String name;

    @Size(max = 5000, message = "Description must not exceed 5000 characters")
    @Column(columnDefinition = "TEXT")
    private String description;

    @Size(max = 100, message = "Domain must not exceed 100 characters")
    @Column(length = 100)
    private String domain;

    @NotNull(message = "Project status is required")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50, columnDefinition = "varchar(50)")
    private ProjectStatus status;

    @Version
    @Column(nullable = false)
    private Integer version = 0;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    // Relations

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    @OneToOne(fetch = FetchType.LAZY, mappedBy = "project", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private ProjectUnderstanding understanding;

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "project")
    private Set<ProjectOutput> outputs;

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "project")
    private Set<ProjectRecord> records;

}
