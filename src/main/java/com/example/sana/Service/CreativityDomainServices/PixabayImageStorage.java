package com.example.sana.Service.CreativityDomainServices;

import com.example.sana.API.ApiException;
import com.example.sana.DTO.CreativityDomainDTOs.MoodboardImageDTO.Image;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;

/** Pixabay forbids permanent image hotlinking; save selected images to persistent server storage. */
@Service
public class PixabayImageStorage {
    private static final int MAX_BYTES = 8 * 1024 * 1024;
    private final Path root;
    private final HttpClient http;

    public PixabayImageStorage(@Value("${sana.images.storage-dir:./sana-moodboard-images}") String path) {
        this.root = Path.of(path).toAbsolutePath().normalize();
        this.http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5))
                .followRedirects(HttpClient.Redirect.NEVER).build();
    }

    public Image store(Image remote) {
        String id = remote.id().replace("PIXABAY:", "");
        if (!id.matches("[0-9]{1,15}") || !"PIXABAY".equals(remote.provider()))
            throw new ApiException("Invalid Pixabay reference");
        Path destination = root.resolve(id + ".bin");
        try {
            Files.createDirectories(root);
            if (!Files.isRegularFile(destination)) {
                URI uri = URI.create(remote.imageUrl());
                byte[] bytes = fetch(uri);
                if (detect(bytes) == null) throw new ApiException("Pixabay returned an unsupported image format");
                Path temp = Files.createTempFile(root, "pixabay-", ".tmp");
                try {
                    Files.write(temp, bytes);
                    Files.move(temp, destination, StandardCopyOption.REPLACE_EXISTING);
                } finally {
                    Files.deleteIfExists(temp);
                }
            }
        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            throw new ApiException("Could not store selected Pixabay image; check persistent image storage directory");
        }
        String localUrl = "/api/v1/creativity/moodboard/images/pixabay-assets/" + id;
        return new Image(remote.id(), remote.title(), localUrl, localUrl, remote.creator(),
                remote.creatorUrl(), remote.license(), remote.licenseVersion(), remote.licenseUrl(),
                remote.sourceUrl(), remote.attribution(), remote.provider());
    }

    private byte[] fetch(URI initial) throws Exception {
        URI uri = initial;
        for (int redirect = 0; redirect < 4; redirect++) {
            // Never follow an untrusted redirect or fetch localhost/internal services.
            if (!"https".equalsIgnoreCase(uri.getScheme()) || uri.getUserInfo() != null ||
                    !("pixabay.com".equalsIgnoreCase(uri.getHost()) || "cdn.pixabay.com".equalsIgnoreCase(uri.getHost())))
                throw new ApiException("Untrusted Pixabay image host");
            HttpRequest request = HttpRequest.newBuilder(uri).GET().timeout(Duration.ofSeconds(18)).build();
            HttpResponse<InputStream> response = http.send(request, HttpResponse.BodyHandlers.ofInputStream());
            int status = response.statusCode();
            if (status >= 300 && status < 400) {
                response.body().close();
                String location = response.headers().firstValue("Location").orElseThrow();
                uri = uri.resolve(location);
                continue;
            }
            if (status != 200) {
                response.body().close();
                throw new ApiException("Pixabay image server returned HTTP " + status);
            }
            String contentType = response.headers().firstValue("Content-Type").orElse("");
            if (!contentType.toLowerCase().startsWith("image/")) {
                response.body().close();
                throw new ApiException("Pixabay response was not an image");
            }
            try (InputStream stream = response.body(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
                byte[] chunk = new byte[8192];
                int count;
                while ((count = stream.read(chunk)) != -1) {
                    if (out.size() + count > MAX_BYTES) throw new ApiException("Pixabay image is too large");
                    out.write(chunk, 0, count);
                }
                return out.toByteArray();
            }
        }
        throw new ApiException("Too many Pixabay image redirects");
    }

    public byte[] read(String id) {
        if (id == null || !id.matches("[0-9]{1,15}")) throw new ApiException("Invalid Pixabay asset ID");
        try {
            Path file = root.resolve(id + ".bin");
            if (!Files.isRegularFile(file) || Files.size(file) > MAX_BYTES) throw new ApiException("Pixabay asset missing");
            return Files.readAllBytes(file);
        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            throw new ApiException("Could not read saved Pixabay asset");
        }
    }

    public MediaType mimeType(byte[] bytes) {
        String detected = detect(bytes);
        if (detected == null) throw new ApiException("Invalid stored image");
        return MediaType.parseMediaType(detected);
    }

    private String detect(byte[] bytes) {
        if (bytes == null || bytes.length < 12) return null;
        if ((bytes[0] & 255) == 255 && (bytes[1] & 255) == 216 && (bytes[2] & 255) == 255) return "image/jpeg";
        if ((bytes[0] & 255) == 137 && bytes[1] == 'P' && bytes[2] == 'N' && bytes[3] == 'G') return "image/png";
        if (bytes[0] == 'R' && bytes[1] == 'I' && bytes[2] == 'F' && bytes[3] == 'F'
                && bytes[8] == 'W' && bytes[9] == 'E' && bytes[10] == 'B' && bytes[11] == 'P') return "image/webp";
        return null;
    }
}
