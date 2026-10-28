package com.example.sana.Model;

import com.example.sana.Enums.RecordStatus;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
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
public class ProjectRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @NotEmpty(message = "title cannot be empty")
    @Column(columnDefinition = "varchar(50) not null")
    private String title;

    @NotEmpty(message = "content cannot be empty")
    @Column(columnDefinition = "varchar(50) not null")
    private String content;

    @NotEmpty(message = "type cannot be empty")
    @Column(columnDefinition = "varchar(50) not null")
    private String type;

    @NotNull(message = "Record status is required")
    @Enumerated(EnumType.STRING)
    @Column(columnDefinition = "varchar(50) not null")
    private RecordStatus status;

    @NotEmpty(message = "source cannot be empty")
    @Column(columnDefinition = "varchar(50) not null")
    private String source;

    @NotNull(message = "version cannot be null")
    @Column(columnDefinition = "int not null")
    private Integer version;

    @Column(columnDefinition = "date")
    private LocalDate createdAt;

    @Column(columnDefinition = "date")
    private LocalDate updatedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn
    @JsonIgnore
    private Project project;

}
