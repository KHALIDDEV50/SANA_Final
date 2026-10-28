package com.example.sana.Config;

import jakarta.annotation.PostConstruct;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.net.URI;

@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "sana.n8n")
public class N8nProperties {
    // Only tests/administrative overrides need an explicit switch. Normally the URL enables n8n.
    @Getter(lombok.AccessLevel.NONE)
    @Setter(lombok.AccessLevel.NONE)
    private Boolean enabled;
    private boolean businessEnabled;
    private boolean workerEnabled = true;
    private String webhookUrl = "";
    private String requestKey = "";
    private String callbackKey = "";
    @Min(1) @Max(30) private int connectTimeoutSeconds = 5;
    @Min(1) @Max(60) private int readTimeoutSeconds = 10;
    @Min(10) @Max(3600) private int requestTimeoutSeconds = 180;
    @Min(1) @Max(10) private int maxDispatchAttempts = 3;
    @Min(1) @Max(300) private int retryDelaySeconds = 5;
    @Min(1) @Max(100) private int batchSize = 10;
    @Min(1024) @Max(4194304) private int maxRequestBytes = 1048576;
    @Min(1024) @Max(4194304) private int maxCallbackBytes = 2097152;

    public boolean isEnabled() {return enabled != null ? enabled : webhookUrl != null && !webhookUrl.isBlank();}
    public void setEnabled(boolean enabled) {this.enabled = enabled;}

    @PostConstruct
    public void validateEnabledConfiguration() {

        // Temporary diagnostics: never print secret values.
        boolean webhookPresent = webhookUrl != null && !webhookUrl.isBlank();
        boolean requestKeyPresent = requestKey != null && !requestKey.isBlank();
        boolean callbackKeyPresent = callbackKey != null && !callbackKey.isBlank();

        boolean webhookValid = false;

        try {
            URI uri = URI.create(webhookUrl);
            webhookValid =
                    ("https".equals(uri.getScheme()) || "http".equals(uri.getScheme()))
                            && uri.getHost() != null
                            && uri.getUserInfo() == null
                            && uri.getFragment() == null;
        } catch (RuntimeException ignored) {
            // Report an invalid URL without exposing its value.
        }

        System.out.println("n8n enabled: " + isEnabled());
        System.out.println("Webhook configured: " + webhookPresent);
        System.out.println("Webhook format valid: " + webhookValid);
        System.out.println("Request key configured: " + requestKeyPresent);
        System.out.println("Callback key configured: " + callbackKeyPresent);
        System.out.println("Keys are different: "
                + (requestKeyPresent && callbackKeyPresent
                && !requestKey.equals(callbackKey)));
        if (!isEnabled()) return;
        try {
            URI uri = URI.create(webhookUrl);
            if (!("https".equals(uri.getScheme()) || "http".equals(uri.getScheme()))
                    || uri.getHost() == null || uri.getUserInfo() != null || uri.getFragment() != null
                    || requestKey.isBlank() || callbackKey.isBlank() || requestKey.equals(callbackKey)) throw new IllegalArgumentException();
        } catch (RuntimeException e) {
            throw new IllegalStateException("Enabled n8n integration requires a valid webhook URL and both private keys");
        }

    }

}
