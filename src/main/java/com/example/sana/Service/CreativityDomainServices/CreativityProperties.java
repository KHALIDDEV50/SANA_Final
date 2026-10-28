package com.example.sana.Service.CreativityDomainServices;

import com.example.sana.API.ApiException;
import com.example.sana.Service.DiagramServices.GraphRuleChecks;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

// Reads and checks typed values in Creativity graph properties.
// JSON numbers can arrive as Integer, Long or BigDecimal, so integers are read through Number.
public final class CreativityProperties {

    // Limit for TEXT columns, well below MySQL's 65,535 bytes
    public static final int LONG_TEXT = 20000;

    private CreativityProperties() {
    }

    // A text property, or null when it is missing
    public static String text(Map<String, Object> properties, String key) {

        Object value = properties.get(key);

        return value instanceof String text ? text : null;
    }

    // A short text property that may also arrive as a list of short texts (AI agents often send
    // ["brave", "loyal"]); a list is joined with commas, an Arabic comma when the text is Arabic
    public static String textOrList(Map<String, Object> properties, String key) {

        Object value = properties.get(key);

        if (value instanceof String text) {
            return text;
        }

        if (value instanceof List<?> list && !list.isEmpty() && list.stream().allMatch(item -> item instanceof String)) {
            String joined = list.stream().map(item -> ((String) item).trim()).collect(Collectors.joining(", "));
            return joined.codePoints().anyMatch(c -> c >= 0x0600 && c <= 0x06FF) ? joined.replace(", ", "، ") : joined;
        }

        return null;
    }

    // When present, the property must be text, or a list of texts, that fits in limit characters once joined
    public static void optionalTextOrList(Map<String, Object> properties, String key, int limit) {

        Object value = properties.get(key);

        if (value == null) {
            return;
        }

        String text = textOrList(properties, key);

        if (text == null || text.length() > limit) {
            throw new ApiException("Property " + key + " must be text, or a list of short texts, of at most " + limit + " characters in total");
        }
    }

    // An integer property, or null when it is missing
    public static Integer integer(Map<String, Object> properties, String key) {

        Object value = properties.get(key);

        if (value == null) {
            return null;
        }

        if (!(value instanceof Number number)) {
            throw new ApiException("Property " + key + " must be a whole number");
        }

        try {
            return new BigDecimal(number.toString()).intValueExact();
        } catch (ArithmeticException | NumberFormatException e) {
            throw new ApiException("Property " + key + " must be a whole number");
        }
    }

    // The property must be non-blank text with at most limit characters
    public static void requireText(Map<String, Object> properties, String key, int limit) {
        GraphRuleChecks.requireText(properties.get(key), limit, "Property " + key);
    }

    // When present, the property must be text with at most limit characters
    public static void optionalText(Map<String, Object> properties, String key, int limit) {
        GraphRuleChecks.optionalText(properties, key, limit);
    }

    // The property must be a whole number
    public static void requireInteger(Map<String, Object> properties, String key) {

        if (integer(properties, key) == null) {
            throw new ApiException("Property " + key + " is required");
        }
    }

    // When present, the property must be a positive whole number
    public static void optionalPositiveInteger(Map<String, Object> properties, String key) {

        Integer value = integer(properties, key);

        if (value != null && value <= 0) {
            throw new ApiException("Property " + key + " must be greater than zero");
        }
    }
}
