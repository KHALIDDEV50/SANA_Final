package com.example.sana.Service.IntegrationServices;

import com.example.sana.Config.N8nProperties;
import com.example.sana.DTO.IntegrationDTOs.N8nAcknowledgementDTO;
import com.example.sana.DTO.IntegrationDTOs.N8nClassificationDTO;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import java.net.ConnectException;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

@Service
@lombok.extern.slf4j.Slf4j
public class N8nClient {
    private final N8nProperties properties;
    private final AIJson json;
    private final RestClient client;
    // Classification waits for the AI's answer, so it needs a longer read timeout than a dispatch
    private final RestClient classifyClient;

    @org.springframework.beans.factory.annotation.Autowired
    public N8nClient(N8nProperties properties, AIJson json) {
        this.properties = properties; this.json = json;
        var factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(properties.getConnectTimeoutSeconds()));
        factory.setReadTimeout(Duration.ofSeconds(properties.getReadTimeoutSeconds()));
        client = RestClient.builder().requestFactory(factory).build();
        var classifyFactory = new SimpleClientHttpRequestFactory();
        classifyFactory.setConnectTimeout(Duration.ofSeconds(properties.getConnectTimeoutSeconds()));
        classifyFactory.setReadTimeout(Duration.ofSeconds(90));
        classifyClient = RestClient.builder().requestFactory(classifyFactory).build();
    }

    // Exposed constructor allows mocked HTTP transport without model calls in tests.
    N8nClient(N8nProperties properties, AIJson json, RestClient client) {
        this.properties = properties; this.json = json; this.client = client; this.classifyClient = client;
    }

    public N8nAcknowledgementDTO dispatch(String requestId, String storedRequest) {
        try {
            return client.post().uri(properties.getWebhookUrl()).header("X-Sana-Key", properties.getRequestKey())
                    .contentType(org.springframework.http.MediaType.APPLICATION_JSON).body(storedRequest)
                    .exchange((request, response) -> {
                        int status = response.getStatusCode().value();
                        // Log the status only; never expose credentials or request content.
                        log.info(
                                "n8n dispatch response. Request ID: {}, HTTP status: {}",
                                requestId,
                                status
                        );
                        if (status == 429) throw new DispatchFailure(true, "DISPATCH_RATE_LIMITED");
                        if (status >= 400 && status < 500) throw new DispatchFailure(false, "DISPATCH_REJECTED");
                        if (status != 202) throw new DispatchFailure(null, "DISPATCH_UNCERTAIN");
                        byte[] body = response.getBody().readNBytes(8193);
                        if (body.length > 8192) throw new DispatchFailure(null, "INVALID_ACKNOWLEDGEMENT");
                        try {
                            var ack = json.read(new String(body, StandardCharsets.UTF_8), N8nAcknowledgementDTO.class);
                            if (!Integer.valueOf(1).equals(ack.schemaVersion()) || !requestId.equals(ack.requestId())
                                    || ack.executionId() == null || ack.executionId().isBlank() || ack.executionId().length() > 255)
                                throw new IllegalArgumentException();
                            return ack;
                        } catch (RuntimeException e) { throw new DispatchFailure(null, "INVALID_ACKNOWLEDGEMENT"); }
                    });
        } catch (ResourceAccessException e) {
            boolean definitelyUnsent = false;
            for (Throwable cause = e; cause != null; cause = cause.getCause())
                if (cause instanceof UnknownHostException || cause instanceof ConnectException) definitelyUnsent = true;
            // A read timeout may follow successful acceptance. Never blindly submit it again.
            throw new DispatchFailure(definitelyUnsent ? Boolean.TRUE : null, "DISPATCH_UNCERTAIN");
        } catch (DispatchFailure e) { throw e; }
        catch (RuntimeException e) { throw new DispatchFailure(null, "DISPATCH_UNCERTAIN"); }
    }

    // Quick start: ask the n8n classify workflow (<webhook URL>/classify) which domain and diagram fit an idea.
    // Returns null when n8n did not give a usable answer; the caller turns that into a user-facing error.
    public N8nClassificationDTO classify(String requestBody) {
        String url = properties.getWebhookUrl().replaceAll("/+$", "") + "/classify";
        try {
            return classifyClient.post().uri(url).header("X-Sana-Key", properties.getRequestKey())
                    .contentType(org.springframework.http.MediaType.APPLICATION_JSON).body(requestBody)
                    .exchange((request, response) -> {
                        int status = response.getStatusCode().value();
                        log.info("n8n classify response. HTTP status: {}", status);
                        if (status != 200) return null;
                        byte[] body = response.getBody().readNBytes(16385);
                        if (body.length > 16384) return null;
                        try {
                            return json.read(new String(body, StandardCharsets.UTF_8), N8nClassificationDTO.class);
                        } catch (RuntimeException e) { return null; }
                    });
        } catch (RuntimeException e) {
            log.info("n8n classify failed: {}", e.getClass().getSimpleName());
            return null;
        }
    }

    public static class DispatchFailure extends RuntimeException {
        // true = safe to retry; false = definitive rejection; null = acceptance unknown.
        public final Boolean retrySafe;
        public final String code;
        public DispatchFailure(Boolean retrySafe, String code) {
            super(code); this.retrySafe = retrySafe; this.code = code;
        }
    }
}
