package com.example.sana.Service.Business;

import com.example.sana.Enums.BusinessToolType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import tools.jackson.databind.json.JsonMapper;

// Deterministic input/result validation remains in Spring Boot. AI execution is exclusively n8n.
@Service
@RequiredArgsConstructor
public class BusinessAnalysisService {
    private final BusinessResponseValidator validator;
    private final JsonMapper json;

    public Object normalizeInput(String input) {
        try {
            var node = json.readTree(input == null || input.isBlank() ? "{}" : input);
            if (node == null || !node.isObject()) throw new IllegalArgumentException();
            return json.convertValue(node, java.util.Map.class);
        } catch (RuntimeException e) {throw new IllegalArgumentException("Business input must be a JSON object");}
    }

    public String validateResponse(BusinessToolType type, String response) {
        return json.writeValueAsString(validator.validate(response, type));
    }
}
