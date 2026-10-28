package com.example.sana.DTO.CreativityDomainDTOs;

import jakarta.validation.constraints.*;
import java.time.LocalDate;
import java.util.List;

public class MoodboardDTO {

    // Existing Project + a new ProjectOutput. No additional database tables.
    public record CreateBoard(@NotBlank @Size(max = 255) String title) {}
    public record RenameBoard(@NotBlank @Size(max = 255) String title) {}

    // Prefer provider + imageId. openverseId is retained for old clients, but new Openverse saves are disabled.
    public record AddImage(String openverseId, String imageId, String provider,
                           @NotNull Double positionX, @NotNull Double positionY) {}

    // Labels are standalone ProjectNodes and can be moved independently of images.
    public record AddLabel(@NotBlank @Size(max = 1000) String text, String color, String backgroundColor,
                           Integer fontSize, @NotNull Double positionX, @NotNull Double positionY) {}

    public record EditLabel(@NotBlank @Size(max = 1000) String text, String color, String backgroundColor,
                            Integer fontSize) {}

    public record MoveItem(@NotNull Double positionX, @NotNull Double positionY) {}

    public record LabelData(String text, String color, String backgroundColor, Integer fontSize) {}

    // One image node stores Openverse attribution information alongside the image URL.
    public record Item(Integer id, String type, Double positionX, Double positionY,
                       MoodboardImageDTO.Image image, LabelData label) {}

    public record Board(Integer id, Integer projectId, String title, List<Item> items) {}
    public record BoardSummary(Integer id, Integer projectId, String title) {}
}
