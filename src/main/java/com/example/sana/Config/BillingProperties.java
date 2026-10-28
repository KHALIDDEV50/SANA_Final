package com.example.sana.Config;

import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import java.net.URI;

@Getter
@Setter
@ConfigurationProperties(prefix = "sana.billing")
public class BillingProperties {
    // Values come from application.properties (environment variables). Billing stays off until it is configured.
    private boolean enabled = false;
    private boolean testMode = true;
    private String secretKey = "";
    private String publicBaseUrl = "";
    private String frontendBaseUrl = "";

    @PostConstruct
    public void validate() {
        if (!enabled) return;
        if (!testMode) throw new IllegalStateException("This delivery integration supports Moyasar TEST mode only");
        String prefix = "sk_test_";
        if (secretKey == null || !secretKey.startsWith(prefix))
            throw new IllegalStateException("Billing requires the matching Moyasar secret key in the environment");
        validateUrl(publicBaseUrl); validateUrl(frontendBaseUrl);
    }
    private void validateUrl(String value) {
        try {
            URI uri = URI.create(value);
            if (!"https".equals(uri.getScheme()) || uri.getHost() == null || uri.getUserInfo() != null
                    || uri.getQuery() != null || uri.getFragment() != null || !uri.getPath().matches("/?"))
                throw new IllegalArgumentException();
        } catch (RuntimeException e) {
            throw new IllegalStateException("Billing public and frontend base URLs must be HTTPS origins without paths");
        }
    }
    public String publicOrigin() { return publicBaseUrl.replaceAll("/+$", ""); }
    public String frontendOrigin() { return frontendBaseUrl.replaceAll("/+$", ""); }
}
