package com.example.sana.Service.Business;

import com.example.sana.Enums.BusinessToolType;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Component
@AllArgsConstructor
public class BusinessResponseValidator {

    private final ObjectMapper objectMapper;

    public JsonNode validate(String responseJson, BusinessToolType expectedToolType) {

        if (expectedToolType == null) {
            throw new IllegalArgumentException("Expected business tool type is required.");
        }

        if (responseJson == null || responseJson.isBlank()) {
            throw new IllegalArgumentException("AI response must not be empty.");
        }

        List<String> errors = new ArrayList<>();
        JsonNode root;

        try {
            root = objectMapper.readTree(responseJson);
        } catch (Exception exception) {
            throw new IllegalArgumentException("AI response is not valid JSON.");
        }

        if (root == null || !root.isObject()) {
            throw new IllegalArgumentException("AI response must be a JSON object.");
        }

        requireText(root, "toolType", "$", errors);
        requireText(root, "executiveSummary", "$", errors);

        // Reject a copied schema label instead of an actual executive summary.
        JsonNode summary = root.get("executiveSummary");
        if (summary != null && summary.isTextual()) {
            String normalized = summary.asText().strip().replaceAll("\\s+", " ");
            if (Set.of("ملخص تنفيذي", "الملخص التنفيذي", "Executive summary",
                    "Executive Summary").contains(normalized)) {
                errors.add("$.executiveSummary must contain an actual analysis, not a placeholder");
            }
        }

        JsonNode toolTypeNode = root.get("toolType");

        if (toolTypeNode != null && toolTypeNode.isTextual()) {
            if (!expectedToolType.name().equals(toolTypeNode.asText())) {
                errors.add(
                        "$.toolType must match "
                                + expectedToolType.name());
            }
        }

        switch (expectedToolType) {
            case BUSINESS_MODEL_BUILDER ->
                    validateBusinessModel(root, errors);

            case TARGET_AUDIENCE_BUILDER ->
                    validateTargetAudience(root, errors);

            case SWOT_ANALYZER ->
                    validateSwot(root, errors);

            case COMPETITOR_ANALYSIS ->
                    validateCompetitors(root, errors);

            case RISK_ANALYZER ->
                    validateRisks(root, errors);
        }

        if (!errors.isEmpty()) {
            throw new IllegalArgumentException(
                    "AI response validation failed: "
                            + String.join("; ", errors));
        }

        return root;
    }

    private void validateBusinessModel(
            JsonNode root, List<String> errors) {

        validateAllowedFields(
                root,
                "$",
                errors,
                Set.of("toolType", "executiveSummary", "businessModel", "businessModelCoherence", "gaps", "assumptions", "recommendations", "followUpQuestions"));

        requireObject(root, "businessModel", "$", errors);
        requireArray(root, "businessModelCoherence", "$", errors);
        requireArray(root, "gaps", "$", errors);
        requireArray(root, "assumptions", "$", errors);
        requireArray(root, "recommendations", "$", errors);
        requireArray(root, "followUpQuestions", "$", errors);

        JsonNode model = root.get("businessModel");

        if (model == null || !model.isObject()) {
            return;
        }

        validateAllowedFields(
                model,
                "$.businessModel",
                errors,
                Set.of("customerSegments", "valuePropositions", "channels",
                        "customerRelationships", "revenueStreams", "keyResources",
                        "keyActivities", "keyPartnerships", "costStructure"));
        for (String arrayName : List.of(
                "customerSegments", "valuePropositions", "channels",
                "customerRelationships", "revenueStreams", "keyResources",
                "keyActivities", "keyPartnerships", "costStructure")) {
            requireArray(model, arrayName, "$.businessModel", errors);
        }

        validateObjectArray(model, "customerSegments", "businessModel",
                errors, "name", "description", "needs", "evidenceBasis");

        validateObjectArray(model, "valuePropositions", "businessModel",
                errors, "name", "customerProblem", "customerBenefit",
                "differentiation", "evidenceBasis");

        validateObjectArray(model, "channels", "businessModel",
                errors, "name", "purpose", "stage", "evidenceBasis");

        validateObjectArray(model, "customerRelationships", "businessModel",
                errors, "type", "description", "evidenceBasis");

        validateObjectArray(model, "revenueStreams", "businessModel",
                errors, "name", "mechanism", "pricingConsiderations",
                "evidenceBasis");

        validateObjectArray(model, "keyResources", "businessModel",
                errors, "name", "purpose", "evidenceBasis");

        validateObjectArray(model, "keyActivities", "businessModel",
                errors, "name", "purpose", "evidenceBasis");

        validateObjectArray(model, "keyPartnerships", "businessModel",
                errors, "name", "contribution", "dependency",
                "evidenceBasis");

        validateObjectArray(model, "costStructure", "businessModel",
                errors, "name", "costType", "description",
                "evidenceBasis");

        validateEnumInArray(model, "channels", "stage",
                "$.businessModel.channels",
                errors, "AWARENESS", "ACQUISITION", "DELIVERY", "RETENTION");

        validateEnumInArray(model, "costStructure", "costType",
                "$.businessModel.costStructure",
                errors, "FIXED", "VARIABLE", "MIXED");

        validateObjectArray(root, "businessModelCoherence", "$", errors,
                "finding", "businessImpact", "priority");
        validateObjectArray(root, "gaps", "$", errors,
                "area", "description", "whyItMatters", "priority");
        validateObjectArray(root, "assumptions", "$", errors,
                "assumption", "reason", "validationMethod");
        validateObjectArray(root, "recommendations", "$", errors,
                "title", "action", "rationale", "priority", "firstStep");
        validateObjectArray(root, "followUpQuestions", "$", errors,
                "question", "reason");
        validatePriorityArrays(root, List.of("businessModelCoherence", "gaps", "recommendations"), "$", errors);

        for (String arrayName : List.of(
                "customerSegments", "valuePropositions", "channels",
                "customerRelationships", "revenueStreams", "keyResources",
                "keyActivities", "keyPartnerships", "costStructure")) {
            validateEnumInArray(
                    model, arrayName, "evidenceBasis",
                    "$.businessModel." + arrayName,
                    errors, "FACT", "ASSUMPTION", "INFERENCE");
        }
    }

    private void validateTargetAudience(
            JsonNode root, List<String> errors) {

        validateAllowedFields(
                root,
                "$",
                errors,
                Set.of("toolType", "executiveSummary", "audienceSegments", "customerPersonas", "segmentPrioritization", "gaps", "assumptions", "recommendations", "researchQuestions"));

        requireArray(root, "audienceSegments", "$", errors);
        requireArray(root, "customerPersonas", "$", errors);
        requireArray(root, "segmentPrioritization", "$", errors);
        requireArray(root, "gaps", "$", errors);
        requireArray(root, "assumptions", "$", errors);
        requireArray(root, "recommendations", "$", errors);
        requireArray(root, "researchQuestions", "$", errors);

        validateObjectArray(root, "audienceSegments", "$", errors,
                "name", "description", "customerType", "needs",
                "painPoints", "motivations", "buyingBarriers",
                "decisionCriteria", "purchasingRole",
                "evidenceBasis", "confidence");

        validateObjectArray(root, "customerPersonas", "$", errors,
                "name", "segmentName", "profile", "goals",
                "painPoints", "motivations", "objections",
                "preferredChannels", "buyingTriggers", "evidenceBasis");

        validateObjectArray(root, "segmentPrioritization", "$", errors,
                "segmentName", "priority", "rationale", "keyUncertainty");
        validateObjectArray(root, "gaps", "$", errors,
                "area", "description", "validationMethod", "priority");
        validateObjectArray(root, "assumptions", "$", errors,
                "assumption", "reason", "validationMethod");
        validateObjectArray(root, "recommendations", "$", errors,
                "title", "action", "rationale", "priority", "firstStep");
        validateObjectArray(root, "researchQuestions", "$", errors,
                "question", "purpose");
        validatePriorityArrays(root, List.of("segmentPrioritization", "gaps", "recommendations"), "$", errors);

        validateEnumInArray(root, "audienceSegments", "customerType",
                "$.audienceSegments", errors,
                "B2C", "B2B", "B2G", "B2B2C", "OTHER");

        validateEnumInArray(root, "audienceSegments", "purchasingRole",
                "$.audienceSegments", errors,
                "USER", "BUYER", "DECISION_MAKER", "INFLUENCER", "OTHER");

        validateEnumInArray(root, "audienceSegments", "confidence",
                "$.audienceSegments", errors,
                "HIGH", "MEDIUM", "LIMITED");

        validateEnumInArray(root, "segmentPrioritization", "priority",
                "$.segmentPrioritization", errors,
                "HIGH", "MEDIUM", "LOW");

        validateEnumInArray(root, "audienceSegments", "evidenceBasis",
                "$.audienceSegments", errors, "FACT", "ASSUMPTION", "INFERENCE");

        validateEnumInArray(root, "customerPersonas", "evidenceBasis",
                "$.customerPersonas", errors, "FACT", "ASSUMPTION", "INFERENCE");

        Set<String> segmentNames = collectTextValues(root, "audienceSegments", "name");
        validateReferences(root, "customerPersonas", "segmentName", "$", segmentNames, errors);
        validateReferences(root, "segmentPrioritization", "segmentName", "$", segmentNames, errors);
    }

    private void validateSwot(
            JsonNode root, List<String> errors) {

        validateAllowedFields(
                root,
                "$",
                errors,
                Set.of("toolType", "executiveSummary", "strengths", "weaknesses", "opportunities", "threats", "strategies", "criticalGaps", "assumptions", "recommendations", "followUpQuestions"));

        requireArray(root, "strengths", "$", errors);
        requireArray(root, "weaknesses", "$", errors);
        requireArray(root, "opportunities", "$", errors);
        requireArray(root, "threats", "$", errors);
        requireObject(root, "strategies", "$", errors);
        requireArray(root, "criticalGaps", "$", errors);
        requireArray(root, "assumptions", "$", errors);
        requireArray(root, "recommendations", "$", errors);
        requireArray(root, "followUpQuestions", "$", errors);

        validateObjectArray(root, "strengths", "$", errors,
                "title", "description", "businessImpact",
                "evidenceBasis", "priority");

        validateObjectArray(root, "weaknesses", "$", errors,
                "title", "description", "businessImpact",
                "evidenceBasis", "priority");

        validateObjectArray(root, "opportunities", "$", errors,
                "title", "description", "potentialBenefit",
                "conditions", "evidenceBasis", "priority");

        validateObjectArray(root, "threats", "$", errors,
                "title", "description", "potentialImpact",
                "warningIndicators", "evidenceBasis", "priority");

        for (String arrayName : List.of(
                "strengths", "weaknesses", "opportunities", "threats")) {
            validateEnumInArray(
                    root, arrayName, "evidenceBasis",
                    "$." + arrayName, errors,
                    "FACT", "ASSUMPTION", "INFERENCE");
            validateEnumInArray(
                    root, arrayName, "priority",
                    "$." + arrayName, errors,
                    "HIGH", "MEDIUM", "LOW");
        }

        validateObjectArray(root, "criticalGaps", "$", errors,
                "area", "description", "validationMethod", "priority");
        validateObjectArray(root, "assumptions", "$", errors,
                "assumption", "reason", "validationMethod");
        validateObjectArray(root, "recommendations", "$", errors,
                "title", "action", "rationale", "priority", "firstStep");
        validateObjectArray(root, "followUpQuestions", "$", errors,
                "question", "reason");
        validatePriorityArrays(root, List.of("criticalGaps", "recommendations"), "$", errors);

        JsonNode strategies = root.get("strategies");

        if (strategies != null && strategies.isObject()) {
            for (String key : List.of("SO", "WO", "ST", "WT")) {
                requireArray(strategies, key, "$.strategies", errors);
            }

            validateObjectArray(strategies, "SO", "strategies", errors,
                    "title", "action", "linkedStrengths",
                    "linkedOpportunities", "priority");

            validateObjectArray(strategies, "WO", "strategies", errors,
                    "title", "action", "linkedWeaknesses",
                    "linkedOpportunities", "priority");

            validateObjectArray(strategies, "ST", "strategies", errors,
                    "title", "action", "linkedStrengths",
                    "linkedThreats", "priority");

            validateObjectArray(strategies, "WT", "strategies", errors,
                    "title", "action", "linkedWeaknesses",
                    "linkedThreats", "priority");

            for (String strategyType : List.of("SO", "WO", "ST", "WT")) {
                validateEnumInArray(
                        strategies, strategyType, "priority",
                        "$.strategies." + strategyType, errors,
                        "HIGH", "MEDIUM", "LOW");
            }

            // Each strategy must reference existing items in its corresponding SWOT category.
            for (String type : List.of("SO", "WO", "ST", "WT")) {
                if (type.equals("SO") || type.equals("ST")) {
                    validateReferences(strategies, type, "linkedStrengths", "$.strategies",
                            collectTextValues(root, "strengths", "title"), errors);
                } else {
                    validateReferences(strategies, type, "linkedWeaknesses", "$.strategies",
                            collectTextValues(root, "weaknesses", "title"), errors);
                }
                if (type.equals("SO") || type.equals("WO")) {
                    validateReferences(strategies, type, "linkedOpportunities", "$.strategies",
                            collectTextValues(root, "opportunities", "title"), errors);
                } else {
                    validateReferences(strategies, type, "linkedThreats", "$.strategies",
                            collectTextValues(root, "threats", "title"), errors);
                }
            }

            validateAllowedFields(
                    strategies,
                    "$.strategies",
                    errors,
                    Set.of("SO", "WO", "ST", "WT"));
        }
    }

    private void validateCompetitors(
            JsonNode root, List<String> errors) {

        validateAllowedFields(
                root,
                "$",
                errors,
                Set.of("toolType", "executiveSummary", "competitors", "comparisonCriteria", "comparison", "competitiveAdvantages", "marketGaps", "informationGaps", "assumptions", "recommendations", "researchQuestions"));

        requireArray(root, "competitors", "$", errors);
        requireArray(root, "comparisonCriteria", "$", errors);
        requireArray(root, "comparison", "$", errors);
        requireArray(root, "competitiveAdvantages", "$", errors);
        requireArray(root, "marketGaps", "$", errors);
        requireArray(root, "informationGaps", "$", errors);
        requireArray(root, "assumptions", "$", errors);
        requireArray(root, "recommendations", "$", errors);
        requireArray(root, "researchQuestions", "$", errors);

        validateObjectArray(root, "competitors", "$", errors,
                "name", "competitorType", "description",
                "targetCustomers", "knownProductsOrServices",
                "knownStrengths", "knownWeaknesses",
                "evidenceBasis", "confidence", "informationGaps");

        validateObjectArray(root, "comparisonCriteria", "$", errors,
                "criterion", "description");

        validateObjectArray(root, "comparison", "$", errors,
                "competitorName", "criterion", "assessment",
                "evidenceBasis", "confidence");

        validateObjectArray(root, "competitiveAdvantages", "$", errors,
                "advantage", "description", "customerValue",
                "evidenceBasis", "confidence");

        validateObjectArray(root, "marketGaps", "$", errors,
                "gap", "description", "potentialOpportunity",
                "evidenceBasis", "validationMethod");

        validateObjectArray(root, "informationGaps", "$", errors,
                "competitorName", "missingInformation",
                "whyItMatters", "researchMethod", "priority");

        validateObjectArray(root, "assumptions", "$", errors,
                "assumption", "reason", "validationMethod");
        validateObjectArray(root, "recommendations", "$", errors,
                "title", "action", "rationale", "priority", "firstStep");
        validateObjectArray(root, "researchQuestions", "$", errors,
                "question", "purpose");
        validateEnumInArray(root, "recommendations", "priority",
                "$.recommendations", errors, "HIGH", "MEDIUM", "LOW");

        validateEnumInArray(root, "competitors", "competitorType",
                "$.competitors", errors, "DIRECT", "INDIRECT");

        for (String arrayName : List.of("competitors", "comparison", "competitiveAdvantages")) {
            validateEnumInArray(root, arrayName, "confidence",
                    "$." + arrayName, errors, "HIGH", "MEDIUM", "LIMITED");
        }

        // informationGaps may intentionally use an unspecified competitor, so it is not linked here.
        validateReferences(root, "comparison", "competitorName", "$",
                collectTextValues(root, "competitors", "name"), errors);
        validateReferences(root, "comparison", "criterion", "$",
                collectTextValues(root, "comparisonCriteria", "criterion"), errors);

        for (String arrayName : List.of(
                "competitors", "comparison", "competitiveAdvantages", "marketGaps")) {
            validateEnumInArray(
                    root, arrayName, "evidenceBasis",
                    "$." + arrayName, errors,
                    "PROVIDED_DATA", "ASSUMPTION", "INFERENCE");
        }

        validateEnumInArray(root, "informationGaps", "priority",
                "$.informationGaps", errors, "HIGH", "MEDIUM", "LOW");
    }

    private void validateRisks(
            JsonNode root, List<String> errors) {

        validateAllowedFields(
                root,
                "$",
                errors,
                Set.of("toolType", "executiveSummary", "riskContext", "risks", "criticalRiskAreas", "assumptions", "informationGaps", "recommendations", "followUpQuestions"));

        // Validate riskContext as a non-empty string.
        requireText(root, "riskContext", "$", errors);
        requireArray(root, "risks", "$", errors);
        requireArray(root, "criticalRiskAreas", "$", errors);
        requireArray(root, "assumptions", "$", errors);
        requireArray(root, "informationGaps", "$", errors);
        requireArray(root, "recommendations", "$", errors);
        requireArray(root, "followUpQuestions", "$", errors);

        validateObjectArray(root, "risks", "$", errors,
                "riskId", "title", "description", "category",
                "causes", "potentialConsequences", "likelihood",
                "impact", "severity", "priority",
                "earlyWarningIndicators", "preventiveMeasures",
                "mitigationMeasures", "contingencyPlan",
                "responsibleRole", "evidenceBasis", "confidence");

        validateObjectArray(root, "criticalRiskAreas", "$", errors,
                "area", "description", "priority", "informationNeeded");
        validateObjectArray(root, "assumptions", "$", errors,
                "assumption", "reason", "validationMethod");
        validateObjectArray(root, "informationGaps", "$", errors,
                "area", "missingInformation", "whyItMatters", "validationMethod", "priority");
        validateObjectArray(root, "recommendations", "$", errors,
                "title", "action", "rationale", "priority", "firstStep");
        validateObjectArray(root, "followUpQuestions", "$", errors,
                "question", "reason");
        validatePriorityArrays(root, List.of("criticalRiskAreas", "informationGaps", "recommendations"), "$", errors);

        validateEnumInArray(root, "risks", "category",
                "$.risks", errors,
                "STRATEGIC", "FINANCIAL", "OPERATIONAL",
                "TECHNICAL", "MARKET", "LEGAL", "REPUTATIONAL", "SECURITY");

        for (String field : List.of("likelihood", "impact", "severity", "priority")) {
            validateEnumInArray(root, "risks", field,
                    "$.risks", errors, "HIGH", "MEDIUM", "LOW");
        }

        validateEnumInArray(root, "risks", "confidence",
                "$.risks", errors, "HIGH", "MEDIUM", "LIMITED");

        validateEnumInArray(root, "risks", "evidenceBasis",
                "$.risks", errors, "FACT", "ASSUMPTION", "INFERENCE");
    }

    private void validatePriorityArrays(
            JsonNode parent,
            List<String> arrayNames,
            String parentPath,
            List<String> errors) {
        for (String arrayName : arrayNames) {
            validateEnumInArray(parent, arrayName, "priority",
                    parentPath + "." + arrayName, errors,
                    "HIGH", "MEDIUM", "LOW");
        }
    }

    private void validateObjectArray(
            JsonNode parent,
            String arrayName,
            String parentPath,
            List<String> errors,
            String... requiredFields) {

        JsonNode array = parent.get(arrayName);

        if (array == null || !array.isArray()) {
            return;
        }

        for (int i = 0; i < array.size(); i++) {
            JsonNode item = array.get(i);
            String path = parentPath + "." + arrayName + "[" + i + "]";

            if (!item.isObject()) {
                errors.add(path + " must be an object");
                continue;
            }

            // Reject properties that are not declared for this object schema.
            validateAllowedFields(
                    item,
                    path,
                    errors,
                    new HashSet<>(Arrays.asList(requiredFields)));

            for (String field : requiredFields) {
                JsonNode value = item.get(field);

                if (value == null || value.isNull()) {
                    errors.add(path + "." + field + " is required");
                    continue;
                }

                if (isArrayField(field)) {
                    if (!value.isArray()) {
                        errors.add(path + "." + field + " must be an array");
                    } else {
                        for (int j = 0; j < value.size(); j++) {
                            if (!value.get(j).isTextual()
                                    || value.get(j).asText().isBlank()) {
                                errors.add(path + "." + field
                                        + "[" + j + "] must be a non-empty string");
                            }
                        }
                    }
                } else {
                    // Required analytical text must contain meaningful non-blank content.
                    requireText(item, field, path, errors);
                }
            }
        }
    }

    /**
     * Validates that a field in each array object contains an allowed enum value.
     */
    private void validateEnumInArray(
            JsonNode parent,
            String arrayName,
            String fieldName,
            String path,
            List<String> errors,
            String... allowedValues) {

        JsonNode array = parent.get(arrayName);

        if (array == null || !array.isArray()) {
            return;
        }

        Set<String> allowed = new HashSet<>(Arrays.asList(allowedValues));

        for (int i = 0; i < array.size(); i++) {
            JsonNode item = array.get(i);
            String fieldPath = path + "[" + i + "]." + fieldName;

            if (!item.isObject()) {
                continue;
            }

            JsonNode value = item.get(fieldName);

            if (value == null || value.isNull()) {
                errors.add(fieldPath + " is required");
                continue;
            }

            if (!value.isTextual() || value.asText().isBlank()) {
                errors.add(fieldPath + " must be a non-empty string");
                continue;
            }

            if (!allowed.contains(value.asText())) {
                errors.add(
                        fieldPath + " has invalid value: " + value.asText()
                                + ". Allowed values: " + allowed);
            }
        }
    }
    /**
     * Rejects fields that are not declared in the expected object schema.
     */
    private void validateAllowedFields(
            JsonNode object,
            String path,
            List<String> errors,
            Set<String> allowedFields) {

        if (object == null || !object.isObject()) {
            return;
        }

        object.properties().forEach(entry -> {
            String field = entry.getKey();

            if (!allowedFields.contains(field)) {
                errors.add(
                        path + "." + field
                                + " is not allowed by the response schema");
            }
        });
    }

    private void requireText(
            JsonNode parent,
            String field,
            String path,
            List<String> errors) {

        JsonNode value = parent.get(field);

        if (value == null || !value.isTextual()
                || value.asText().isBlank()) {
            errors.add(path + "." + field
                    + " must be a non-empty string");
        }
    }

    private void requireArray(
            JsonNode parent,
            String field,
            String path,
            List<String> errors) {

        JsonNode value = parent.get(field);

        if (value == null || !value.isArray()) {
            errors.add(path + "." + field + " must be an array");
        }
    }

    private void requireObject(
            JsonNode parent,
            String field,
            String path,
            List<String> errors) {

        JsonNode value = parent.get(field);

        if (value == null || !value.isObject()) {
            errors.add(path + "." + field + " must be an object");
        }
    }

    /** Collects valid reference targets without duplicating structural validation. */
    private Set<String> collectTextValues(JsonNode parent, String arrayName, String field) {
        Set<String> values = new HashSet<>();
        JsonNode array = parent.get(arrayName);
        if (array != null && array.isArray()) {
            for (JsonNode item : array) {
                JsonNode value = item.isObject() ? item.get(field) : null;
                if (value != null && value.isTextual() && !value.asText().isBlank()) {
                    values.add(value.asText());
                }
            }
        }
        return values;
    }

    /** Checks scalar and array references against the names supplied in this response. */
    private void validateReferences(JsonNode parent, String arrayName, String field,
                                    String parentPath, Set<String> targets, List<String> errors) {
        JsonNode array = parent.get(arrayName);
        if (array == null || !array.isArray()) {
            return;
        }
        for (int i = 0; i < array.size(); i++) {
            JsonNode item = array.get(i);
            JsonNode value = item.isObject() ? item.get(field) : null;
            String path = parentPath + "." + arrayName + "[" + i + "]." + field;
            if (value != null && value.isTextual() && !value.asText().isBlank()) {
                if (!targets.contains(value.asText())) {
                    errors.add(path + " must reference an existing item: " + value.asText());
                }
            } else if (value != null && value.isArray()) {
                for (int j = 0; j < value.size(); j++) {
                    JsonNode reference = value.get(j);
                    if (reference.isTextual() && !reference.asText().isBlank()
                            && !targets.contains(reference.asText())) {
                        errors.add(path + "[" + j + "] must reference an existing item: "
                                + reference.asText());
                    }
                }
            }
        }
    }

    private boolean isArrayField(String field) {
        return Set.of(
                "needs", "conditions", "warningIndicators",
                "linkedStrengths", "linkedWeaknesses",
                "linkedOpportunities", "linkedThreats",
                "targetCustomers", "knownProductsOrServices",
                "knownStrengths", "knownWeaknesses",
                "informationGaps", "causes",
                "potentialConsequences", "earlyWarningIndicators",
                "preventiveMeasures", "mitigationMeasures",
                "customerSegments", "painPoints", "motivations",
                "buyingBarriers", "decisionCriteria", "goals",
                "objections", "preferredChannels", "buyingTriggers",
                "informationNeeded"
        ).contains(field);
    }
}
