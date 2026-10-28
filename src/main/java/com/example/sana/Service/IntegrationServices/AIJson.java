package com.example.sana.Service.IntegrationServices;

import com.example.sana.API.ApiException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.json.JsonMapper;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;

@Service
@RequiredArgsConstructor
public class AIJson {
    private final JsonMapper mapper;

    public <T> T read(String json, Class<T> type) {
        try {
            return mapper.rebuild().disable(tools.jackson.databind.MapperFeature.ALLOW_COERCION_OF_SCALARS).build().readerFor(type).without(DeserializationFeature.ACCEPT_FLOAT_AS_INT).with(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES,
                    DeserializationFeature.FAIL_ON_TRAILING_TOKENS, DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS).readValue(json);
        } catch (RuntimeException e) { throw new ApiException("Invalid AI payload structure"); }
    }

    public String write(Object value) { return mapper.writeValueAsString(value); }
    public JsonNode tree(Object value) { return mapper.valueToTree(value); }

    public String hash(Object value) {
        try {
            byte[] data = mapper.writeValueAsString(canonical(tree(value))).getBytes(StandardCharsets.UTF_8);
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(data));
        } catch (java.security.NoSuchAlgorithmException e) { throw new IllegalStateException(e); }
    }

    private Object canonical(JsonNode node) {
        if (node.isObject()) {
            Map<String, Object> result = new TreeMap<>();
            node.properties().forEach(entry -> result.put(entry.getKey(), canonical(entry.getValue())));
            return result;
        }
        if (node.isArray()) {
            List<Object> result = new ArrayList<>(); node.forEach(item -> result.add(canonical(item))); return result;
        }
        if (node.isNumber()) return node.decimalValue().stripTrailingZeros();
        if (node.isBoolean()) return node.booleanValue();
        if (node.isNull()) return null;
        return node.asText();
    }
}
