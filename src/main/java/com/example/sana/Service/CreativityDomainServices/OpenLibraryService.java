package com.example.sana.Service.CreativityDomainServices;

import com.example.sana.API.ApiException;
import com.example.sana.DTO.CreativityDomainDTOs.OpenLibraryResponseDTO;
import com.example.sana.DTO.CreativityDomainDTOs.OpenLibraryResponseDTO.BookInfo;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Service
public class OpenLibraryService {

    private final RestClient restClient;
    private final JsonMapper jsonMapper;

    public OpenLibraryService(JsonMapper jsonMapper, @Value("${openlibrary.user-agent:SANA-Inspiration-Library/1.0}") String userAgent) {
        this.jsonMapper = jsonMapper;

        // Configure connection and response timeouts.
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(5));
        factory.setReadTimeout(Duration.ofSeconds(10));

        // Identify SANA when calling the external library.
        restClient = RestClient.builder().baseUrl("https://openlibrary.org")
                .requestFactory(factory)
                .defaultHeader("User-Agent", userAgent).build();
    }

    // Search public books using a keyword and optional pagination.
    public OpenLibraryResponseDTO searchBooks(String keyword, Integer maxResults, Integer startIndex) {
        if (keyword == null || keyword.isBlank() || keyword.length() > 200) {
            throw new ApiException("Search keyword must contain 1 to 200 characters");
        }

        int limit = maxResults == null ? 10 : maxResults;
        int offset = startIndex == null ? 0 : startIndex;

        if (limit < 1 || limit > 40 || offset < 0) {
            throw new ApiException("Invalid pagination values");
        }

        try {
            // Request only the fields needed by SANA to reduce response size.
            String response = restClient.get().uri(builder -> builder.path("/search.json")
                    .queryParam("q", keyword.trim())
                    .queryParam("limit", limit)
                    .queryParam("offset", offset)
                    .queryParam("fields", "key,title,author_name,first_publish_year,cover_i")
                    .build()).retrieve().body(String.class);

            JsonNode root = jsonMapper.readTree(response);
            List<BookInfo> books = new ArrayList<>();

            // Open Library returns search results in the docs array.
            for (JsonNode book : root.path("docs")) {
                String id = getWorkId(getText(book, "key"));

                if (id != null) {
                    books.add(mapSearchBook(book, id));
                }
            }

            // Support both common variants of the total-count property.
            int total = root.path("numFound").asInt(root.path("num_found").asInt(0));
            return new OpenLibraryResponseDTO(total, books);

        } catch (RestClientResponseException e) {
            // Print the status without exposing raw upstream details.
            System.out.println("Open Library HTTP Status: " + e.getStatusCode());
            throw new ApiException("Open Library returned HTTP " + e.getStatusCode().value());

        } catch (RestClientException | JacksonException e) {
            throw new ApiException("Could not retrieve books from Open Library");
        }
    }

    // Retrieve one book's details using its Work ID.
    public BookInfo getBookById(String workId) {
        if (workId == null || !workId.matches("OL[0-9]+W")) {
            throw new ApiException("Invalid Open Library Work ID");
        }

        try {
            String response = restClient.get().uri("/works/{id}.json", workId).retrieve().body(String.class);
            JsonNode book = jsonMapper.readTree(response);

            // Work descriptions can be either plain text or an object.
            JsonNode descriptionNode = book.path("description");
            String description = descriptionNode.isTextual() ? descriptionNode.asText() : getText(descriptionNode, "value");

            // Some works have no cover image.
            JsonNode covers = book.path("covers");
            String thumbnail = covers.isArray() && !covers.isEmpty() && covers.get(0).asLong() > 0
                    ? "https://covers.openlibrary.org/b/id/" + covers.get(0).asLong() + "-M.jpg" : null;

            // Work details normally contain author references, not author names.
            // Author names are already provided by the search response.
            return new BookInfo(workId, getText(book, "title"), List.of(), description,
                    getText(book, "first_publish_date"), thumbnail, "https://openlibrary.org/works/" + workId);

        } catch (RestClientResponseException e) {
            if (e.getStatusCode().value() == 404) {
                throw new ApiException("Book not found");
            }
            throw new ApiException("Open Library returned HTTP " + e.getStatusCode().value());

        } catch (RestClientException | JacksonException e) {
            throw new ApiException("Could not retrieve book details");
        }
    }

    // Convert one search result into a small DTO.
    private BookInfo mapSearchBook(JsonNode book, String id) {
        List<String> authors = new ArrayList<>();

        for (JsonNode author : book.path("author_name")) {
            authors.add(author.asText());
        }

        long coverId = book.path("cover_i").asLong(0);
        String thumbnail = coverId > 0 ? "https://covers.openlibrary.org/b/id/" + coverId + "-M.jpg" : null;

        return new BookInfo(id, getText(book, "title"), authors, null,
                getText(book, "first_publish_year"), thumbnail, "https://openlibrary.org/works/" + id);
    }

    // Normalize work keys such as /works/OL123W into OL123W.
    private String getWorkId(String key) {
        if (key == null) return null;

        String id = key.startsWith("/works/") ? key.substring(7) : key;
        return id.matches("OL[0-9]+W") ? id : null;
    }

    // Return null when optional book metadata is missing.
    private String getText(JsonNode node, String fieldName) {
        JsonNode value = node.path(fieldName);
        return value.isMissingNode() || value.isNull() ? null : value.asText();
    }
}
