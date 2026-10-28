package com.example.sana.Service.CreativityDomainServices;

import com.example.sana.API.ApiException;
import com.example.sana.DTO.GraphDefinitionDTO;
import com.example.sana.Enums.CreativityDomainEnums.BranchingComparisonOperator;
import com.example.sana.Enums.CreativityDomainEnums.BranchingEffectOperator;
import com.example.sana.Enums.CreativityDomainEnums.BranchingNodeType;
import com.example.sana.Enums.CreativityDomainEnums.CreativityOutputType;
import com.example.sana.Service.DiagramServices.BasicDiagramRules;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.example.sana.Service.CreativityDomainServices.CreativityProperties.LONG_TEXT;
import static com.example.sana.Service.CreativityDomainServices.CreativityProperties.integer;
import static com.example.sana.Service.CreativityDomainServices.CreativityProperties.optionalPositiveInteger;
import static com.example.sana.Service.CreativityDomainServices.CreativityProperties.requireInteger;
import static com.example.sana.Service.CreativityDomainServices.CreativityProperties.requireText;
import static com.example.sana.Service.CreativityDomainServices.CreativityProperties.text;

// Rules for the CREATIVITY Branching Story: scenes, player choices and game variables.
@Component
public class BranchingStoryRules extends BasicDiagramRules {

    public static final String CHOICE = "BRANCH_CHOICE";

    public BranchingStoryRules() {
        super("CREATIVITY",
                CreativityOutputType.BRANCHING_STORY.name(),
                Arrays.stream(BranchingNodeType.values()).map(Enum::name).toList(),
                List.of(CHOICE),
                "Branching story for a game. Nodes are scenes: DIALOGUE, DECISION, EVENT or ENDING. properties.label is the scene title "
                        + "(max 255) and properties.content (required) is the scene text. Optional speakerCharacterId and storyEventId are IDs "
                        + "from context.projectCharacters and context.projectStoryEvents. Edges are BRANCH_CHOICE player choices from one scene "
                        + "to the next: properties.text (required) is what the player picks and properties.displayOrder (whole number, 0 or more) "
                        + "orders the choices. ENDING scenes have no outgoing choices. Game variables are listed in the graph's "
                        + "properties.variables as [{\"name\": \"courage\", \"initialValue\": 0}]; names start with a letter and use letters, "
                        + "digits or _ (max 40). A choice may have properties.condition {\"variable\", \"operator\": EQ|NE|GT|GTE|LT|LTE, "
                        + "\"value\"} to show it only when the condition holds, and properties.effect {\"variable\", \"operator\": SET|ADD, "
                        + "\"value\"} to change a variable when chosen. Values are whole numbers. New IDs must be null.");
    }

    // Check a scene
    @Override
    public void validateNode(String type, Map<String, Object> properties, Double positionX, Double positionY) {

        super.validateNode(type, properties, positionX, positionY);

        requireText(properties, "content", LONG_TEXT);
        optionalPositiveInteger(properties, "speakerCharacterId");
        optionalPositiveInteger(properties, "storyEventId");
    }

    // Check a player choice
    @Override
    public void validateEdge(String type, String label, String sourceType, String targetType, Map<String, Object> properties) {

        super.validateEdge(type, label, sourceType, targetType, properties);

        if (BranchingNodeType.ENDING.name().equals(sourceType)) {
            throw new ApiException("Ending scenes cannot have outgoing choices");
        }

        requireText(properties, "text", LONG_TEXT);
        requireInteger(properties, "displayOrder");
        if (integer(properties, "displayOrder") < 0) {
            throw new ApiException("Choice displayOrder cannot be negative");
        }

        Map<String, Object> condition = optionalObject(properties, "condition");
        if (condition != null) {
            checkVariableReference(condition, "condition");
            readEnum(BranchingComparisonOperator.class, condition.get("operator"), "condition operator");
        }

        Map<String, Object> effect = optionalObject(properties, "effect");
        if (effect != null) {
            checkVariableReference(effect, "effect");
            readEnum(BranchingEffectOperator.class, effect.get("operator"), "effect operator");
        }
    }

    // Variables must be valid and unique, and every choice may only use declared variables
    @Override
    public void validateGraph(GraphDefinitionDTO graph) {

        Set<String> variableNames = new HashSet<>();

        for (Map<String, Object> variable : readVariables(graph)) {

            String name = text(variable, "name");
            if (name == null || !name.matches("[A-Za-z][A-Za-z0-9_]{0,39}")) {
                throw new ApiException("Variable names must start with a letter and use letters, digits or _ (max 40)");
            }
            if (!variableNames.add(name)) {
                throw new ApiException("Variable " + name + " is declared more than once");
            }
            requireInteger(variable, "initialValue");
        }

        for (GraphDefinitionDTO.EdgeDTO edge : graph.getEdges()) {
            for (String part : List.of("condition", "effect")) {

                Map<String, Object> reference = optionalObject(edge.getProperties(), part);
                if (reference != null && !variableNames.contains(text(reference, "variable"))) {
                    throw new ApiException("Choice " + edge.getKey() + " uses an undeclared variable in its " + part);
                }
            }
        }
    }

    // The graph's properties.variables list, or an empty list
    @SuppressWarnings("unchecked")
    public static List<Map<String, Object>> readVariables(GraphDefinitionDTO graph) {

        Object value = graph.getProperties().get("variables");

        if (value == null) {
            return List.of();
        }

        if (!(value instanceof List<?> list) || list.stream().anyMatch(item -> !(item instanceof Map))) {
            throw new ApiException("properties.variables must be a list of {name, initialValue} objects");
        }

        return (List<Map<String, Object>>) list;
    }

    // A condition or effect object, or null when the choice has none
    @SuppressWarnings("unchecked")
    public static Map<String, Object> optionalObject(Map<String, Object> properties, String key) {

        Object value = properties.get(key);

        if (value == null) {
            return null;
        }

        if (!(value instanceof Map<?, ?>)) {
            throw new ApiException("Choice " + key + " must be an object");
        }

        return (Map<String, Object>) value;
    }

    // Read an enum value from a property, failing with the allowed values
    public static <E extends Enum<E>> E readEnum(Class<E> type, Object value, String field) {

        try {
            return Enum.valueOf(type, String.valueOf(value));
        } catch (IllegalArgumentException e) {
            throw new ApiException("Invalid " + field + ". Allowed values: " + Arrays.toString(type.getEnumConstants()));
        }
    }

    private void checkVariableReference(Map<String, Object> reference, String part) {

        if (text(reference, "variable") == null) {
            throw new ApiException("Choice " + part + " needs a variable name");
        }

        requireInteger(reference, "value");
    }
}
