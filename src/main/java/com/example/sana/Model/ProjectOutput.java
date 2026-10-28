package com.example.sana.Model;

import com.example.sana.Enums.RepresentationType;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "project_outputs")
public class ProjectOutput {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @NotBlank(message = "Output title cannot be blank")
    @Size(max = 255, message = "Output title must not exceed 255 characters")
    @Column(nullable = false)
    private String title;

    @NotBlank(message = "Output type cannot be blank")
    @Size(max = 50, message = "Output type must not exceed 50 characters")
    @Column(nullable = false, length = 50)
    private String outputType;

    @NotNull(message = "Representation type is required")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50, columnDefinition = "varchar(50)")
    private RepresentationType representationType;

    @PositiveOrZero(message = "Graph revision cannot be negative")
    @Column(name = "graph_revision", nullable = false, columnDefinition = "integer default 0")
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Integer graphRevision = 0;

    @Column(name = "properties_json", columnDefinition = "LONGTEXT")
    @JsonIgnore
    private String propertiesJson;

    @Version
    @PositiveOrZero(message = "Output version cannot be negative")
    @Column(nullable = false)
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Integer version;

    @PastOrPresent(message = "Output creation date cannot be in the future")
    @Column(nullable = false, updatable = false)
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private LocalDateTime createdAt;

    @PastOrPresent(message = "Output update date cannot be in the future")
    @Column(nullable = false)
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private LocalDateTime updatedAt;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "project_id", referencedColumnName = "id", nullable = false)
    @JsonIgnore
    private Project project;

    @OneToMany(mappedBy = "projectOutput", cascade = CascadeType.ALL)
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Set<ProjectNode> nodes = new HashSet<>();

    @OneToMany(mappedBy = "projectOutput", cascade = CascadeType.ALL)
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Set<ProjectEdge> edges = new HashSet<>();
}
