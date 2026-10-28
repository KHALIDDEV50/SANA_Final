package com.example.sana.DTO.CreativityDomainDTOs;

import java.util.List;

// Public information returned by Openverse. No API key is required for anonymous search.
public class OpenverseDTO {

    public record Image(String id, String title, String imageUrl, String thumbnail, String creator,
                        String creatorUrl, String license, String licenseVersion, String licenseUrl,
                        String sourceUrl, String attribution) {}

    public record SearchResult(Integer page, Integer pageCount, Integer resultCount, List<Image> images) {}
}
