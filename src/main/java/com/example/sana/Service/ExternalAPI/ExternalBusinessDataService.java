
package com.example.sana.Service.ExternalAPI;

import com.example.sana.DTO.WorldBankIndicatorDTO;
import com.example.sana.Enums.BusinessToolType;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@Slf4j
@RequiredArgsConstructor
public class ExternalBusinessDataService {

    private final WorldBankApiService worldBankApiService;

    /*
     * Defines the World Bank indicators required by each business tool.
     */
    private static final Map<BusinessToolType, Set<String>> TOOL_INDICATORS =
            Map.of(
                    BusinessToolType.BUSINESS_MODEL_BUILDER, Set.of(
                            "NY.GDP.MKTP.CD",
                            "NY.GDP.MKTP.KD.ZG",
                            "FP.CPI.TOTL.ZG",
                            "SP.POP.TOTL"
                    ),

                    BusinessToolType.TARGET_AUDIENCE_BUILDER, Set.of(
                            "SP.POP.TOTL"
                    ),

                    BusinessToolType.SWOT_ANALYZER, Set.of(
                            "NY.GDP.MKTP.CD",
                            "NY.GDP.MKTP.KD.ZG",
                            "FP.CPI.TOTL.ZG"
                    ),

                    BusinessToolType.RISK_ANALYZER, Set.of(
                            "NY.GDP.MKTP.CD",
                            "NY.GDP.MKTP.KD.ZG",
                            "FP.CPI.TOTL.ZG"
                    ),

                    BusinessToolType.COMPETITOR_ANALYSIS, Set.of()
            );

    /**
     * Retrieves external data relevant to the selected business tool.
     *
     * External data is optional. If the external source fails,
     * the analysis can continue without external data.
     *
     * @param toolType selected business analysis tool
     * @return relevant external indicators, or an empty list if unavailable
     */
    public List<WorldBankIndicatorDTO> getDataForTool(
            BusinessToolType toolType) {

        long startTime = System.nanoTime();

        if (toolType == null) {
            log.warn(
                    "External data retrieval skipped: tool type is null."
            );
            return List.of();
        }

        Set<String> requiredIndicators =
                TOOL_INDICATORS.get(toolType);

        if (requiredIndicators == null || requiredIndicators.isEmpty()) {
            log.info(
                    "No external indicators configured for tool: {}",
                    toolType
            );
            return List.of();
        }

        log.info(
                "Starting external data retrieval. Tool: {}, indicators: {}",
                toolType,
                requiredIndicators
        );

        try {
            /*
             * The current WorldBankApiService fetches all configured
             * indicators. Filter the results for this tool afterward.
             */
            List<WorldBankIndicatorDTO> allIndicators =
                    worldBankApiService.getSaudiEconomicIndicators();

            List<WorldBankIndicatorDTO> filteredIndicators =
                    allIndicators.stream()
                            .filter(indicator ->
                                    requiredIndicators.contains(
                                            indicator.indicatorCode()
                                    )
                            )
                            .toList();

            long elapsedMs =
                    (System.nanoTime() - startTime) / 1_000_000;

            log.info(
                    "External data retrieval completed. "
                            + "Tool: {}, requested indicators: {}, "
                            + "fetched records: {}, returned records: {}, "
                            + "duration: {} ms",
                    toolType,
                    requiredIndicators.size(),
                    allIndicators.size(),
                    filteredIndicators.size(),
                    elapsedMs
            );

            if (filteredIndicators.isEmpty()) {
                log.warn(
                        "No matching external data found for tool: {}. "
                                + "Check the API response and indicator codes.",
                        toolType
                );
            }

            return filteredIndicators;

        } catch (RuntimeException exception) {
            long elapsedMs =
                    (System.nanoTime() - startTime) / 1_000_000;

            log.error(
                    "External data retrieval failed. "
                            + "Tool: {}, duration: {} ms. "
                            + "Analysis will continue without external data.",
                    toolType,
                    elapsedMs,
                    exception
            );

            return List.of();
        }
    }
}
