package com.example.sana.Model.CreativityDomainModels;

import com.example.sana.Model.ProjectOutput;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "playable_characters")
public class PlayableCharacter {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @NotBlank
    @Column(nullable = false, length = 100)
    private String name;

    // Optional character role or gameplay class
    @Column(length = 100)
    private String role;

    // Weapon details are optional for non-combat games
    @Column(length = 100)
    private String weaponName;

    @Column(length = 100)
    private String weaponType;

    @Column(columnDefinition = "TEXT")
    private String strengths;

    @Column(columnDefinition = "TEXT")
    private String weaknesses;

    // Optional stats: each value ranges from 0 to 10
    @Min(0) @Max(10)
    private Integer attack;

    @Min(0) @Max(10)
    private Integer defense;

    @Min(0) @Max(10)
    private Integer speed;

    @Min(0) @Max(10)
    private Integer rangeStat;

    @Min(0) @Max(10)
    private Integer stamina;

    @Min(0) @Max(10)
    private Integer special;

    private LocalDate createdAt;
    private LocalDate updatedAt;

    // Every playable character belongs to one output
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "project_output_id", nullable = false)
    @JsonIgnore
    private ProjectOutput projectOutput;

    // Optional link to an existing story character
    // Do not cascade deletion to CharacterProfile
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "character_profile_id")
    @JsonIgnore
    private CharacterProfile characterProfile;
}
