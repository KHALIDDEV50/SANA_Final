package com.example.sana.Service.CreativityDomainServices;

import com.example.sana.API.ApiException;
import com.example.sana.DTO.CreativityDomainDTOs.OpenverseDTO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.net.URI;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Service
public class OpenverseService {

    private final RestClient client;
    private final JsonMapper jsonMapper;

    public OpenverseService(JsonMapper jsonMapper, @Value("${openverse.user-agent:SANA-Moodboard/1.0}") String userAgent) {
        this.jsonMapper = jsonMapper;

        // Fixed API host: never accept external URLs directly from the frontend.
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(5));
        factory.setReadTimeout(Duration.ofSeconds(12));
        this.client = RestClient.builder().baseUrl("https://api.openverse.org")
                .defaultHeader("User-Agent", userAgent).requestFactory(factory).build();
    }

    // Search for openly licensed images. Anonymous use is supported but rate-limited.
    public OpenverseDTO.SearchResult searchImages(String keyword, Integer page, Integer pageSize) {
        if (keyword == null || keyword.isBlank() || keyword.length() > 200) {
            throw new ApiException("Search keyword must contain 1 to 200 characters");
        }
        int currentPage = page == null ? 1 : page;
        int limit = pageSize == null ? 12 : pageSize;
        if (currentPage < 1 || currentPage > 20 || limit < 1 || limit > 20) {
            throw new ApiException("Page must be 1-20 and pageSize must be 1-20");
        }

        try {
            String response = client.get().uri(builder -> builder.path("/v1/images/")
                    .queryParam("q", keyword.trim()).queryParam("page", currentPage)
                    .queryParam("page_size", limit).queryParam("mature", "false")
                    .build()).retrieve().body(String.class);

            JsonNode root = parse(response);
            List<OpenverseDTO.Image> images = new ArrayList<>();
            JsonNode results = root.path("results");
            if (results.isArray()) {
                for (JsonNode item : results) {
                    // Ignore mature results defensively even if the upstream filter changes.
                    if (!item.path("mature").asBoolean(false)) {
                        images.add(mapImage(item));
                    }
                }
            }
            return new OpenverseDTO.SearchResult(root.path("page").asInt(currentPage),
                    root.path("page_count").asInt(0), root.path("result_count").asInt(0), images);

        } catch (RestClientResponseException e) {
            throw upstreamError(e);
        } catch (RestClientException | JacksonException e) {
            throw new ApiException("Cannot retrieve images from Openverse");
        }
    }

    // Fetch the authoritative image metadata before allowing it onto a saved board.
    public OpenverseDTO.Image getImage(String openverseId) {
        if (openverseId == null || !openverseId.matches("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}")) {
            throw new ApiException("Invalid Openverse image ID");
        }
        try {
            String response = client.get().uri("/v1/images/{id}/", openverseId)
                    .retrieve().body(String.class);
            JsonNode root = parse(response);
            if (root.path("mature").asBoolean(false)) {
                throw new ApiException("This image cannot be added to the moodboard");
            }
            OpenverseDTO.Image image = mapImage(root);
            if (image.imageUrl() == null || image.license() == null || image.sourceUrl() == null) {
                throw new ApiException("Image is missing a usable URL or license information");
            }
            return image;

        } catch (RestClientResponseException e) {
            if (e.getStatusCode().value() == 404) {
                throw new ApiException("Openverse image not found");
            }
            throw upstreamError(e);
        } catch (RestClientException | JacksonException e) {
            throw new ApiException("Cannot retrieve image details from Openverse");
        }
    }

    private JsonNode parse(String response) {
        if (response == null || response.isBlank()) {
            throw new ApiException("Openverse returned an empty response");
        }
        JsonNode root = jsonMapper.readTree(response);
        if (root == null || root.isNull()) {
            throw new ApiException("Openverse returned invalid JSON");
        }
        return root;
    }

    // Keep only http(s) URLs; never return executable schemes to the frontend.
    private String safeUrl(JsonNode node, String field) {
        String value = text(node, field);
        if (value == null) return null;
        try {
            URI uri = URI.create(value);
            if (uri.getHost() == null || !("http".equalsIgnoreCase(uri.getScheme()) || "https".equalsIgnoreCase(uri.getScheme()))) {
                return null;
            }
            return value;
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private String text(JsonNode node, String field) {
        JsonNode value = node.path(field);
        return value.isMissingNode() || value.isNull() || value.asText().isBlank() ? null : value.asText();
    }

    private OpenverseDTO.Image mapImage(JsonNode image) {
        return new OpenverseDTO.Image(text(image, "id"), text(image, "title"),
                safeUrl(image, "url"), safeUrl(image, "thumbnail"), text(image, "creator"),
                safeUrl(image, "creator_url"), text(image, "license"), text(image, "license_version"),
                safeUrl(image, "license_url"), safeUrl(image, "foreign_landing_url"), text(image, "attribution"));
    }

    private ApiException upstreamError(RestClientResponseException e) {
        if (e.getStatusCode().value() == 429) {
            return new ApiException("Openverse rate limit reached; try again later");
        }
        return new ApiException("Openverse returned HTTP " + e.getStatusCode().value());
    }
}
