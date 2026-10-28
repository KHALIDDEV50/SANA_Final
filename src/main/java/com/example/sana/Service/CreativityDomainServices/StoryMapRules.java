package com.example.sana.Service.CreativityDomainServices;

import com.example.sana.API.ApiException;
import com.example.sana.DTO.GraphDefinitionDTO;
import com.example.sana.Enums.CreativityDomainEnums.CreativityOutputType;
import com.example.sana.Enums.CreativityDomainEnums.CreativityRelationType;
import com.example.sana.Service.DiagramServices.BasicDiagramRules;
import com.example.sana.Service.DiagramServices.GraphRuleChecks;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.example.sana.Service.CreativityDomainServices.CreativityProperties.LONG_TEXT;
import static com.example.sana.Service.CreativityDomainServices.CreativityProperties.optionalPositiveInteger;
import static com.example.sana.Service.CreativityDomainServices.CreativityProperties.optionalText;
import static com.example.sana.Service.CreativityDomainServices.CreativityProperties.optionalTextOrList;
import static com.example.sana.Service.CreativityDomainServices.CreativityProperties.requireInteger;
import static com.example.sana.Service.CreativityDomainServices.CreativityProperties.requireText;
import static com.example.sana.Service.CreativityDomainServices.CreativityProperties.text;

// Rules for the CREATIVITY Story Map: characters, worlds, locations, world rules and story events.
// Node properties are the fields of each element; properties.label is its name or title.
// Text limits follow the database columns of each entity.
@Component
public class StoryMapRules extends BasicDiagramRules {

    public StoryMapRules() {
        super("CREATIVITY",
                CreativityOutputType.STORY_MAP.name(),
                List.of("CHARACTER", "WORLD", "LOCATION", "WORLD_RULE", "STORY_EVENT"),
                Arrays.stream(CreativityRelationType.values()).map(Enum::name).toList(),
                "Story map of characters, worlds, locations, world rules and story events. properties.label is the element's name or title. "
                        + "CHARACTER properties: shortDescription, role (max 50 chars), age (positive number) are required; optional aliases (100), "
                        + "appearance, history, innerConflict, characterArc, writerNotes, and traits, strengths, weaknesses, skills, goals, fears, "
                        + "secrets, characterValues (each a short text of max 50 chars in total, e.g. \"brave, loyal\"; a list of short words is also "
                        + "accepted and joined with commas, but must still fit 50 chars). Label max 255. "
                        + "WORLD properties: description required; optional history, lore, atmosphere, notes. Label max 50. "
                        + "LOCATION properties: optional locationType (50), narrativeSignificance, notes, worldKey (key of a WORLD node), "
                        + "parentLocationKey (key of another LOCATION node). Label max 50. "
                        + "WORLD_RULE properties: description required; optional scope, conditions, exceptions, consequences (each a short text of "
                        + "max 50 chars; put longer explanations in description or notes), notes, "
                        + "worldKey. Label max 50. "
                        + "STORY_EVENT properties: description and presentationOrder (whole number) required; optional eventType (50), "
                        + "chronologicalOrder (positive), inWorldTime (50), notes. Label max 255. "
                        + "Edges use relationship types only between allowed elements: FAMILY, FRIENDSHIP, RIVALRY, WORK, ROMANTIC connect two "
                        + "CHARACTERs; PARTICIPATES_IN, CAUSES, AFFECTED_BY go from CHARACTER to STORY_EVENT; LIVES_IN, BELONGS_TO, VISITS go from "
                        + "CHARACTER to LOCATION; LEADS_TO, PRECEDES, SIMULTANEOUS_WITH, NARRATIVE_LINK connect two STORY_EVENTs; INSIDE, "
                        + "CONNECTED_TO, NEAR connect two LOCATIONs; APPLIES_TO, RESTRICTS, AFFECTS go from WORLD_RULE to any other element; "
                        + "CUSTOM connects any two elements and needs properties.label. New IDs must be null.");
    }

    // Check the element's own fields
    @Override
    public void validateNode(String type, Map<String, Object> properties, Double positionX, Double positionY) {

        super.validateNode(type, properties, positionX, positionY);

        switch (type) {
            case "CHARACTER" -> {
                requireText(properties, "shortDescription", LONG_TEXT);
                requireText(properties, "role", 50);
                requireInteger(properties, "age");
                optionalPositiveInteger(properties, "age");
                optionalText(properties, "aliases", 100);
                for (String key : List.of("appearance", "history", "innerConflict", "characterArc", "writerNotes")) {
                    optionalText(properties, key, LONG_TEXT);
                }
                for (String key : List.of("traits", "strengths", "weaknesses", "skills", "goals", "fears", "secrets", "characterValues")) {
                    optionalTextOrList(properties, key, 50);
                }
            }
            case "WORLD" -> {
                GraphRuleChecks.requireText(properties.get("label"), 50, "World name");
                requireText(properties, "description", LONG_TEXT);
                for (String key : List.of("history", "lore", "atmosphere", "notes")) {
                    optionalText(properties, key, LONG_TEXT);
                }
            }
            case "LOCATION" -> {
                GraphRuleChecks.requireText(properties.get("label"), 50, "Location name");
                optionalText(properties, "locationType", 50);
                optionalText(properties, "narrativeSignificance", LONG_TEXT);
                optionalText(properties, "notes", LONG_TEXT);
                optionalText(properties, "worldKey", 100);
                optionalText(properties, "parentLocationKey", 100);
            }
            case "WORLD_RULE" -> {
                GraphRuleChecks.requireText(properties.get("label"), 50, "World rule name");
                requireText(properties, "description", LONG_TEXT);
                for (String key : List.of("scope", "conditions", "exceptions", "consequences")) {
                    optionalTextOrList(properties, key, 50);
                }
                optionalText(properties, "notes", LONG_TEXT);
                optionalText(properties, "worldKey", 100);
            }
            case "STORY_EVENT" -> {
                requireText(properties, "description", LONG_TEXT);
                requireInteger(properties, "presentationOrder");
                optionalPositiveInteger(properties, "chronologicalOrder");
                optionalText(properties, "eventType", 50);
                optionalText(properties, "inWorldTime", 50);
                optionalText(properties, "notes", LONG_TEXT);
            }
            default -> throw new ApiException("Unsupported story map node type");
        }
    }

    // The relationship type must fit the two element types
    @Override
    public void validateEdge(String type, String label, String sourceType, String targetType, Map<String, Object> properties) {

        super.validateEdge(type, label, sourceType, targetType, properties);

        CreativityRelationType relation = CreativityRelationType.valueOf(type);

        if (!relation.isAllowed(sourceType, targetType)) {
            throw new ApiException(type + " is not allowed from " + sourceType + " to " + targetType);
        }

        if (relation == CreativityRelationType.CUSTOM && (label == null || label.equals(type))) {
            throw new ApiException("Custom relationships need a label");
        }
    }

    // worldKey and parentLocationKey must point to the right kind of node, without parent cycles
    @Override
    public void validateGraph(GraphDefinitionDTO graph) {

        Map<String, GraphDefinitionDTO.NodeDTO> nodesByKey = new HashMap<>();
        for (GraphDefinitionDTO.NodeDTO node : graph.getNodes()) {
            nodesByKey.put(node.getKey(), node);
        }

        for (GraphDefinitionDTO.NodeDTO node : graph.getNodes()) {

            String worldKey = text(node.getProperties(), "worldKey");
            if (worldKey != null) {
                requireNodeType(nodesByKey, worldKey, "WORLD", "worldKey of " + node.getKey());
            }

            String parentKey = text(node.getProperties(), "parentLocationKey");
            if (parentKey != null) {

                if (!"LOCATION".equals(node.getType())) {
                    throw new ApiException("Only locations can have a parentLocationKey");
                }
                requireNodeType(nodesByKey, parentKey, "LOCATION", "parentLocationKey of " + node.getKey());

                // A location and its parent must be in the same world when both name one
                String parentWorldKey = text(nodesByKey.get(parentKey).getProperties(), "worldKey");
                if (worldKey != null && parentWorldKey != null && !worldKey.equals(parentWorldKey)) {
                    throw new ApiException("Location " + node.getKey() + " and its parent must belong to the same world");
                }

                checkNoParentCycle(nodesByKey, node.getKey());
            }
        }
    }

    private void requireNodeType(Map<String, GraphDefinitionDTO.NodeDTO> nodesByKey, String key, String type, String field) {

        GraphDefinitionDTO.NodeDTO target = nodesByKey.get(key);

        if (target == null || !type.equals(target.getType())) {
            throw new ApiException(field + " must be the key of a " + type + " node in this graph");
        }
    }

    private void checkNoParentCycle(Map<String, GraphDefinitionDTO.NodeDTO> nodesByKey, String startKey) {

        Set<String> visited = new HashSet<>();
        String current = startKey;

        while (current != null && nodesByKey.containsKey(current)) {
            if (!visited.add(current)) {
                throw new ApiException("Circular parent locations are not allowed");
            }
            current = text(nodesByKey.get(current).getProperties(), "parentLocationKey");
        }
    }
}
