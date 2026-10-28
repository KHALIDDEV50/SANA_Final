package com.example.sana.DTO.AIDTOs;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

// "Start with SANA AI": an idea whose domain and diagram type the AI should choose
@Getter
@Setter
public class QuickStartRequestDTO {

    @NotBlank(message = "Idea is required")
    @Size(max = 6000, message = "Idea must not exceed 6000 characters")
    private String idea;

    // Language for the suggested project name and reason
    @Pattern(regexp = "ar|en", message = "Language must be ar or en")
    private String language = "ar";
}
