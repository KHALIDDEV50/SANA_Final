package com.example.sana.Enums.CreativityDomainEnums;

public enum CreativityRelationType {

    // Character ↔ Character
    FAMILY("Family"),
    FRIENDSHIP("Friendship"),
    RIVALRY("Rivalry"),
    WORK("Work"),
    ROMANTIC("Romantic"),

    // Character → StoryEvent
    PARTICIPATES_IN("Participates in"),
    CAUSES("Causes"),
    AFFECTED_BY("Affected by"),

    // Character → Location
    LIVES_IN("Lives in"),
    BELONGS_TO("Belongs to"),
    VISITS("Visits"),

    // StoryEvent → StoryEvent
    LEADS_TO("Leads to"),
    PRECEDES("Precedes"),
    SIMULTANEOUS_WITH("Simultaneous with"),
    NARRATIVE_LINK("Narrative link"),

    // Location ↔ Location
    INSIDE("Inside"),
    CONNECTED_TO("Connected to"),
    NEAR("Near"),

    // WorldRule → Other creativity elements
    APPLIES_TO("Applies to"),
    RESTRICTS("Restricts"),
    AFFECTS("Affects"),

    // Custom relationship between creativity elements
    CUSTOM("Custom");

    private final String defaultLabel;

    CreativityRelationType(String defaultLabel) {
        this.defaultLabel = defaultLabel;
    }

    public String getDefaultLabel() {
        return defaultLabel;
    }

    // Check whether a relationship type is valid
    // for the selected source and target Node types
    public boolean isAllowed(String sourceType, String targetType) {

        if (!isCreativityNode(sourceType)
                || !isCreativityNode(targetType)) {
            return false;
        }

        return switch (this) {

            case FAMILY, FRIENDSHIP, RIVALRY, WORK, ROMANTIC ->
                    isPair(sourceType, targetType,
                            "CHARACTER", "CHARACTER");

            case PARTICIPATES_IN, CAUSES, AFFECTED_BY ->
                    sourceType.equals("CHARACTER")
                            && targetType.equals("STORY_EVENT");

            case LIVES_IN, BELONGS_TO, VISITS ->
                    sourceType.equals("CHARACTER")
                            && targetType.equals("LOCATION");

            case LEADS_TO, PRECEDES,
                 SIMULTANEOUS_WITH, NARRATIVE_LINK ->
                    isPair(sourceType, targetType,
                            "STORY_EVENT", "STORY_EVENT");

            case INSIDE, CONNECTED_TO, NEAR ->
                    isPair(sourceType, targetType,
                            "LOCATION", "LOCATION");

            case APPLIES_TO, RESTRICTS, AFFECTS ->
                    sourceType.equals("WORLD_RULE")
                            && !targetType.equals("WORLD_RULE");

            case CUSTOM -> true;
        };
    }

    // Check whether both Nodes have the expected types
    private boolean isPair(String sourceType, String targetType,
                           String expectedSource, String expectedTarget) {

        return sourceType.equals(expectedSource)
                && targetType.equals(expectedTarget);
    }

    // Only allow the five creativity Node types
    private boolean isCreativityNode(String type) {

        if (type == null) {
            return false;
        }

        return switch (type) {
            case "CHARACTER", "WORLD", "LOCATION",
                 "WORLD_RULE", "STORY_EVENT" -> true;
            default -> false;
        };
    }
}
