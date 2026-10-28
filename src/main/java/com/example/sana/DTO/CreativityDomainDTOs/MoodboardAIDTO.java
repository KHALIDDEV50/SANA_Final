package com.example.sana.DTO.CreativityDomainDTOs;

import jakarta.validation.constraints.*;
import java.time.Instant;
import java.util.List;

/** Separate from graph requests, graph proposals and n8n callback DTOs. */
public final class MoodboardAIDTO {
    private MoodboardAIDTO() {}
    public record Generate(@Size(max = 10000) String prompt,
                           @NotBlank @Pattern(regexp = "ar|en") String language) {}
    public record Color(String name, String hex) {}
    public record Plan(String title, String theme, List<String> searchTerms,
                       List<String> notes, List<Color> palette) {}
    public record ImageReference(MoodboardImageDTO.Image image, String searchTerm,
                                 double positionX, double positionY) {}
    public record Note(String text, String color, String backgroundColor,
                       double positionX, double positionY) {}
    public record Preview(String draftId, Integer projectId, String status, Instant expiresAt,
                          String title, String theme, List<ImageReference> images,
                          List<Note> notes, List<Color> palette, List<String> warnings,
                          boolean imagesVisuallyAnalyzed, Integer savedBoardId) {}
    public record Decision(@NotNull @Min(1) @Max(1) Integer version) {}
    public record Saved(String draftId, Integer boardId, Integer projectId) {}
}
