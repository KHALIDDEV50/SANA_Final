package com.example.sana.DTO.CreativityDomainDTOs;

import java.util.List;

public record OpenLibraryResponseDTO(Integer totalItems, List<BookInfo> books) {

    // Book information displayed in SANA's Inspiration Library.
    public record BookInfo(String id, String title, List<String> authors, String description, String publishedDate, String thumbnail, String previewLink) {}
}
