package com.example.sana.Service;

import com.example.sana.DTO.Notification.UserRegisteredEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import tools.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;

@Service
@Slf4j
public class RegistrationNotificationService {

    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;
    private final boolean enabled;
    private final String webhookUrl;
    private final String requestKey;

    public RegistrationNotificationService(
            ObjectMapper objectMapper,
            @Value("${sana.notifications.enabled:false}") boolean enabled,
            @Value("${sana.notifications.webhook-url:}") String webhookUrl,
            @Value("${sana.notifications.request-key:}") String requestKey) {

        this.objectMapper = objectMapper;
        this.enabled = enabled;
        this.webhookUrl = webhookUrl;
        this.requestKey = requestKey;

        // Keep connection attempts short and do not forward secrets via redirects.
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(3))
                .followRedirects(HttpClient.Redirect.NEVER)
                .build();
    }

    // Run only after the user registration transaction commits successfully.
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void sendWelcomeEmail(UserRegisteredEvent event) {

        if (!enabled) {
            return;
        }

        try {
            URI uri = URI.create(webhookUrl);

            // Send notification data and the private key only over HTTPS.
            if (!"https".equalsIgnoreCase(uri.getScheme())
                    || uri.getHost() == null
                    || uri.getUserInfo() != null
                    || uri.getFragment() != null
                    || requestKey.isBlank()) {

                log.warn(
                        "Registration notification configuration is invalid. User ID: {}",
                        event.userId()
                );

                return;
            }

            // Never include passwords or password hashes in notifications.
            String body = objectMapper.writeValueAsString(
                    Map.of(
                            "eventType", "USER_REGISTERED",
                            "userId", event.userId(),
                            "name", event.name(),
                            "email", event.email()
                    )
            );

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(uri)
                    .timeout(Duration.ofSeconds(5))
                    .header("Content-Type", "application/json")
                    .header("X-Sana-Notification-Key", requestKey)
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();

            HttpResponse<Void> response = httpClient.send(
                    request,
                    HttpResponse.BodyHandlers.discarding()
            );

            if (response.statusCode() >= 200
                    && response.statusCode() < 300) {

                // Acceptance by n8n does not confirm email delivery.
                log.info(
                        "Registration notification accepted by n8n. User ID: {}",
                        event.userId()
                );

            } else {

                log.warn(
                        "Registration notification rejected. User ID: {}, HTTP status: {}",
                        event.userId(),
                        response.statusCode()
                );
            }

        } catch (InterruptedException exception) {

            Thread.currentThread().interrupt();

            log.warn(
                    "Registration notification interrupted. User ID: {}",
                    event.userId()
            );

        } catch (Exception exception) {

            // Notification failures must not make registration fail.
            // Do not log private keys, request bodies, or provider responses.
            log.warn(
                    "Registration notification could not be sent. User ID: {}",
                    event.userId()
            );
        }
    }
}