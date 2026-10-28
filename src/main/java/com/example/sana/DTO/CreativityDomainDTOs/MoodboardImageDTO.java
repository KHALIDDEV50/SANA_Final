package com.example.sana.DTO.CreativityDomainDTOs;

import java.util.List;

/** Unified, provider-identified image metadata. The first eleven fields match legacy Openverse JSON. */
public final class MoodboardImageDTO {
    private MoodboardImageDTO() {}

    public record Image(String id, String title, String imageUrl, String thumbnail, String creator,
                        String creatorUrl, String license, String licenseVersion, String licenseUrl,
                        String sourceUrl, String attribution, String provider) {}

    public record SearchResult(Integer page, Integer pageCount, Integer resultCount, List<Image> images) {}
}
