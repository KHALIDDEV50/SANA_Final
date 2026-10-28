package com.example.sana.Service.AIServices;

import com.example.sana.Config.N8nProperties;
import com.example.sana.DTO.AIDTOs.QuickStartRequestDTO;
import com.example.sana.DTO.AIDTOs.QuickStartSuggestionDTO;
import com.example.sana.DTO.IntegrationDTOs.N8nClassificationDTO;
import com.example.sana.Service.DiagramServices.DiagramRules;
import com.example.sana.Service.DiagramServices.DiagramRulesRegistry;
import com.example.sana.Service.IntegrationServices.AIJson;
import com.example.sana.Service.IntegrationServices.N8nClient;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

// "Start with SANA AI": the AI chooses the project domain and diagram type for an idea.
// Nothing is created here; the user confirms the suggestion and the frontend creates the project.
@Service
@RequiredArgsConstructor
public class QuickStartService {

    // The frontend's domains and the stored project domain of each. Writing and games are both CREATIVITY.
    private static final Map<String, String> CATEGORY_DOMAINS = new LinkedHashMap<>();

    static {
        CATEGORY_DOMAINS.put("TECHNOLOGY", "TECH");
        CATEGORY_DOMAINS.put("BUSINESS", "BUSINESS");
        CATEGORY_DOMAINS.put("EDUCATION", "EDUCATION");
        CATEGORY_DOMAINS.put("WRITING", "CREATIVITY");
        CATEGORY_DOMAINS.put("GAMES", "CREATIVITY");
    }

    private final N8nProperties n8nProperties;
    private final N8nClient n8nClient;
    private final DiagramRulesRegistry diagramRulesRegistry;
    private final AIJson aiJson;

    public QuickStartSuggestionDTO classify(QuickStartRequestDTO request) {

        if (!n8nProperties.isEnabled()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "SANA AI is not configured on this server");
        }

        Map<String, List<DiagramRules>> options = options();

        // The AI may only pick from the categories and diagram types listed here
        List<Map<String, Object>> categories = new ArrayList<>();
        for (Map.Entry<String, List<DiagramRules>> option : options.entrySet()) {
            List<Map<String, String>> diagramTypes = new ArrayList<>();
            for (DiagramRules rules : option.getValue()) {
                diagramTypes.add(Map.of("diagramType", rules.getDiagramType(),
                        "summary", rules.getRulesSummary() == null ? "" : rules.getRulesSummary()));
            }
            categories.add(Map.of("category", option.getKey(), "diagramTypes", diagramTypes));
        }

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("schemaVersion", 1);
        body.put("idea", request.getIdea().trim());
        body.put("language", request.getLanguage() == null ? "ar" : request.getLanguage());
        body.put("categories", categories);

        N8nClassificationDTO answer = n8nClient.classify(aiJson.write(body));
        if (answer == null) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "SANA AI could not classify the idea. Please try again");
        }

        // Never trust the AI's choice: it must be one of the options that were offered
        List<DiagramRules> allowed = answer.category() == null ? null : options.get(answer.category());
        DiagramRules chosen = allowed == null ? null : allowed.stream()
                .filter(rules -> rules.getDiagramType().equals(answer.diagramType())).findFirst().orElse(null);
        if (chosen == null) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "SANA AI suggested an unsupported diagram. Please try again");
        }

        return new QuickStartSuggestionDTO(answer.category(), CATEGORY_DOMAINS.get(answer.category()), chosen.getDiagramType(),
                clean(answer.projectName(), 100, request.getIdea()), clean(answer.reason(), 300, ""));
    }

    // Supported diagram types per category. Branching stories have their own Studio tab, and Business
    // diagrams are offered only when the Business AI is enabled.
    private Map<String, List<DiagramRules>> options() {

        Map<String, List<DiagramRules>> options = new LinkedHashMap<>();

        for (Map.Entry<String, String> category : CATEGORY_DOMAINS.entrySet()) {
            if (category.getValue().equals("BUSINESS") && !n8nProperties.isBusinessEnabled()) {
                continue;
            }
            List<DiagramRules> types = diagramRulesRegistry.getAllRules().stream()
                    .filter(rules -> rules.getDomain().equals(category.getValue()))
                    .filter(rules -> !rules.getDiagramType().equals("BRANCHING_STORY"))
                    .toList();
            if (!types.isEmpty()) {
                options.put(category.getKey(), types);
            }
        }

        return options;
    }

    // Trim a text from the AI to a length the project accepts, or use the fallback when it is empty
    private String clean(String text, int maxLength, String fallback) {

        String value = text == null || text.isBlank() ? fallback : text;
        value = value.strip().replaceAll("\\s+", " ");

        return value.length() > maxLength ? value.substring(0, maxLength).strip() : value;
    }
}
