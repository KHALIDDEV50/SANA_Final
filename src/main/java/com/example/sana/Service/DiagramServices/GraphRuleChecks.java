package com.example.sana.Service.DiagramServices;

import com.example.sana.API.ApiException;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

// Small checks shared by every DiagramRules implementation and the n8n callback.
public final class GraphRuleChecks {

    private GraphRuleChecks() {
    }

    // The value must be a non-blank string with at most limit characters
    public static void requireText(Object value, int limit, String field) {

        if (!(value instanceof String text) || text.isBlank() || text.length() > limit) {
            throw new ApiException(field + " is required and must not exceed " + limit + " characters");
        }
    }

    // When the property is present it must be a string with at most limit characters
    public static void optionalText(Map<String, Object> properties, String key, int limit) {

        Object value = properties.get(key);

        if (value != null && (!(value instanceof String text) || text.length() > limit)) {
            throw new ApiException("Invalid " + key + " property");
        }
    }

    // Node coordinates must be finite and inside the canvas limits
    public static void requirePosition(Double positionX, Double positionY) {

        if (positionX == null || positionY == null
                || !Double.isFinite(positionX) || !Double.isFinite(positionY)
                || Math.abs(positionX) > 1000000 || Math.abs(positionY) > 1000000) {
            throw new ApiException("Node coordinates must be finite and within the canvas limits");
        }
    }

    // A list of objects such as ERD attributes; an empty list when the property is missing
    @SuppressWarnings("unchecked")
    public static List<Map<String, Object>> objectList(Map<String, Object> properties, String key) {

        Object value = properties.get(key);

        if (value == null) {
            return List.of();
        }

        if (!(value instanceof List<?> list) || list.stream().anyMatch(item -> !(item instanceof Map))) {
            throw new ApiException("Property " + key + " must be a list of objects");
        }

        if (list.size() > 200) {
            throw new ApiException("Property " + key + " has too many items");
        }

        return (List<Map<String, Object>>) list;
    }

    // When present, the property must be true or false
    public static void optionalBoolean(Map<String, Object> properties, String key) {

        Object value = properties.get(key);

        if (value != null && !(value instanceof Boolean)) {
            throw new ApiException("Property " + key + " must be true or false");
        }
    }

    // When present, the property must be one of the allowed values
    public static void optionalChoice(Map<String, Object> properties, String key, List<String> allowed) {

        Object value = properties.get(key);

        if (value != null && !allowed.contains(value)) {
            throw new ApiException("Property " + key + " must be one of " + allowed);
        }
    }

    // Item names in a list, such as attribute names, must be unique ignoring case
    public static void requireUniqueNames(List<Map<String, Object>> items, String what, String owner) {

        Set<String> names = new HashSet<>();

        for (Map<String, Object> item : items) {
            if (!names.add(String.valueOf(item.get("name")).toLowerCase())) {
                throw new ApiException(what + " names must be unique in " + owner);
            }
        }
    }
}
