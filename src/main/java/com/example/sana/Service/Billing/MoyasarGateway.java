package com.example.sana.Service.Billing;

import com.example.sana.Config.BillingProperties;
import com.example.sana.Model.BillingOrder;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import java.net.URI;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;

@Service
@RequiredArgsConstructor
public class MoyasarGateway implements BillingGateway {
    private final BillingProperties properties;
    private final JsonMapper mapper;
    // Never follow redirects with the private API key.
    private final HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5))
            .followRedirects(HttpClient.Redirect.NEVER).build();

    @Override
    public Invoice createInvoice(BillingOrder order, String callbackUrl, String successUrl, String backUrl) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("amount", order.getAmountHalalas()); body.put("currency", "SAR");
        body.put("description", "SANA " + order.getPlanCode() + " - one month - " + order.getId());
        body.put("callback_url", callbackUrl); body.put("success_url", successUrl); body.put("back_url", backUrl);
        body.put("expired_at", order.getExpiresAt().toString());
        return send("", mapper.writeValueAsString(body));
    }
    @Override
    public Invoice fetchInvoice(String invoiceId) {
        UUID.fromString(invoiceId);
        return send("/" + invoiceId, null);
    }
    private Invoice send(String path, String body) {
        if (!properties.isEnabled()) throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Billing is disabled");
        String auth = Base64.getEncoder().encodeToString((properties.getSecretKey() + ":").getBytes(StandardCharsets.UTF_8));
        HttpRequest.Builder request = HttpRequest.newBuilder(URI.create("https://api.moyasar.com/v1/invoices" + path))
                .timeout(Duration.ofSeconds(15)).header("Authorization", "Basic " + auth)
                .header("Accept", "application/json");
        if (body != null) request.header("Content-Type", "application/json").POST(HttpRequest.BodyPublishers.ofString(body));
        else request.GET();
        try {
            HttpResponse<String> response = client.send(request.build(), HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300)
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Payment provider could not complete the request");
            JsonNode node = mapper.readTree(response.body());
            if (!node.path("id").isTextual() || !node.path("amount").isIntegralNumber()
                    || !node.path("currency").isTextual() || !node.path("status").isTextual())
                throw new IllegalArgumentException("Invalid invoice response");
            String id = node.path("id").asText(); UUID.fromString(id);
            String url = node.path("url").asText();
            URI checkout = URI.create(url);
            if (!"https".equals(checkout.getScheme()) || !"checkout.moyasar.com".equals(checkout.getHost())
                    || checkout.getUserInfo() != null || (checkout.getPort() != -1 && checkout.getPort() != 443))
                throw new IllegalArgumentException("Invalid checkout URL");
            return new Invoice(id, node.path("status").asText(), node.path("amount").asInt(), node.path("currency").asText(), url);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Payment provider request interrupted");
        } catch (ResponseStatusException e) { throw e;
        } catch (Exception e) {
            // Do not expose provider bodies, callback tokens, or API keys in client errors/logs.
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Could not verify the payment provider response");
        }
    }
}
