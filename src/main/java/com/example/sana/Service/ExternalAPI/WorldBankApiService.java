package com.example.sana.Service.ExternalAPI;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.example.sana.DTO.WorldBankIndicatorDTO;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Service
public class WorldBankApiService {

    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    private static final Map<String, String> INDICATORS = Map.of(
            "NY.GDP.MKTP.CD", "الناتج المحلي الإجمالي",
            "NY.GDP.MKTP.KD.ZG", "نمو الناتج المحلي الإجمالي",
            "FP.CPI.TOTL.ZG", "التضخم",
            "SP.POP.TOTL", "عدد السكان"
    );

    public WorldBankApiService(
            RestClient.Builder restClientBuilder,
            ObjectMapper objectMapper) {

        this.restClient = restClientBuilder
                .baseUrl("https://api.worldbank.org/v2")
                .build();

        this.objectMapper = objectMapper;
    }

    public List<WorldBankIndicatorDTO> getSaudiEconomicIndicators() {
        List<WorldBankIndicatorDTO> results = new ArrayList<>();

        for (String indicatorCode : INDICATORS.keySet()) {
            results.addAll(getIndicator("SAU", indicatorCode, 5));
        }

        return results;
    }

    private List<WorldBankIndicatorDTO> getIndicator(
            String countryCode,
            String indicatorCode,
            int count) {

        String response = restClient.get()
                .uri("/country/{country}/indicator/{indicator}"
                                + "?mrnev={count}&format=json",
                        countryCode, indicatorCode, count)
                .retrieve()
                .body(String.class);

        if (response == null || response.isBlank()) {
            throw new IllegalStateException(
                    "World Bank API returned an empty response");
        }

        try {
            JsonNode root = objectMapper.readTree(response);

            // استجابة البنك الدولي عبارة عن مصفوفة:
            // العنصر الأول بيانات وصفية، والثاني بيانات المؤشر.
            if (!root.isArray()
                    || root.size() < 2
                    || !root.get(1).isArray()) {
                return List.of();
            }

            JsonNode observations = root.get(1);
            List<WorldBankIndicatorDTO> results = new ArrayList<>();

            for (JsonNode item : observations) {
                JsonNode valueNode = item.get("value");

                // قد لا تتوفر قيمة لبعض السنوات.
                if (valueNode == null || valueNode.isNull()) {
                    continue;
                }

                JsonNode indicator = item.path("indicator");
                JsonNode country = item.path("country");

                results.add(new WorldBankIndicatorDTO(
                        indicator.path("id").asText(),
                        INDICATORS.get(indicatorCode),
                        country.path("id").asText(),
                        country.path("value").asText(),
                        item.path("date").asText(),
                        valueNode.asDouble(),
                        item.path("unit").asText("")
                ));
            }

            return results;

        } catch (JacksonException exception) {
            throw new IllegalStateException(
                    "Failed to parse World Bank API response",
                    exception);
        }
    }
}
