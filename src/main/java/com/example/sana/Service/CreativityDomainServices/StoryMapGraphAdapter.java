package com.example.sana.Service.CreativityDomainServices;

import com.example.sana.API.ApiConflictException;
import com.example.sana.DTO.GraphDefinitionDTO;
import com.example.sana.Enums.CreativityDomainEnums.CreativityOutputType;
import com.example.sana.Model.CreativityDomainModels.CharacterProfile;
import com.example.sana.Model.CreativityDomainModels.Location;
import com.example.sana.Model.CreativityDomainModels.StoryEvent;
import com.example.sana.Model.CreativityDomainModels.World;
import com.example.sana.Model.CreativityDomainModels.WorldRule;
import com.example.sana.Model.ProjectNode;
import com.example.sana.Model.ProjectOutput;
import com.example.sana.Repository.CreativityDomainRepository.BranchingNodeRepository;
import com.example.sana.Repository.CreativityDomainRepository.CharacterProfileRepository;
import com.example.sana.Repository.CreativityDomainRepository.LocationRepository;
import com.example.sana.Repository.CreativityDomainRepository.PlayableCharacterRepository;
import com.example.sana.Repository.CreativityDomainRepository.StoryEventRepository;
import com.example.sana.Repository.CreativityDomainRepository.WorldRepository;
import com.example.sana.Repository.CreativityDomainRepository.WorldRuleRepository;
import com.example.sana.Service.DiagramServices.DiagramGraphAdapter;
import com.example.sana.Service.DiagramServices.GraphApplier;
import com.example.sana.Service.DiagramServices.GraphApplyHooks;
import com.example.sana.Service.DiagramServices.GraphMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.example.sana.Service.CreativityDomainServices.CreativityProperties.integer;
import static com.example.sana.Service.CreativityDomainServices.CreativityProperties.text;
import static com.example.sana.Service.CreativityDomainServices.CreativityProperties.textOrList;

// Reads and saves Story Map diagrams. Each node's properties are the fields of its
// CharacterProfile, World, Location, WorldRule or StoryEvent row; properties.label is the name or title.
// Location/rule worlds and parent locations are properties holding the key of the linked node.
@Component
@RequiredArgsConstructor
public class StoryMapGraphAdapter implements DiagramGraphAdapter {

    // Properties stored in the entity rows; anything else is kept in ProjectNode.dataJson
    private static final Map<String, List<String>> ENTITY_KEYS = Map.of(
            "CHARACTER", List.of("label", "shortDescription", "role", "aliases", "age", "appearance", "traits", "strengths",
                    "weaknesses", "skills", "history", "goals", "fears", "innerConflict", "secrets", "characterValues",
                    "characterArc", "writerNotes"),
            "WORLD", List.of("label", "description", "history", "lore", "atmosphere", "notes"),
            "LOCATION", List.of("label", "locationType", "narrativeSignificance", "notes", "worldKey", "parentLocationKey"),
            "WORLD_RULE", List.of("label", "description", "scope", "conditions", "exceptions", "consequences", "notes", "worldKey"),
            "STORY_EVENT", List.of("label", "description", "eventType", "presentationOrder", "chronologicalOrder", "inWorldTime", "notes")
    );

    private final GraphMapper graphMapper;
    private final GraphApplier graphApplier;
    private final CharacterProfileRepository characterProfileRepository;
    private final WorldRepository worldRepository;
    private final LocationRepository locationRepository;
    private final WorldRuleRepository worldRuleRepository;
    private final StoryEventRepository storyEventRepository;
    private final BranchingNodeRepository branchingNodeRepository;
    private final PlayableCharacterRepository playableCharacterRepository;

    @Override
    public String getDiagramType() {
        return CreativityOutputType.STORY_MAP.name();
    }

    // Read nodes and edges, then fill each node's properties from its entity row
    @Override
    public GraphDefinitionDTO read(ProjectOutput output) {

        GraphDefinitionDTO graph = graphMapper.toGraphDTO(output);

        Map<Integer, ProjectNode> nodesById = new HashMap<>();
        for (ProjectNode node : output.getNodes()) {
            nodesById.put(node.getId(), node);
        }

        for (GraphDefinitionDTO.NodeDTO dto : graph.getNodes()) {

            ProjectNode node = nodesById.get(dto.getId());
            Map<String, Object> properties = new LinkedHashMap<>();

            switch (dto.getType()) {
                case "CHARACTER" -> readCharacter(node.getCharacterProfile(), properties);
                case "WORLD" -> readWorld(node.getWorld(), properties);
                case "LOCATION" -> readLocation(node.getLocation(), properties);
                case "WORLD_RULE" -> readWorldRule(node.getWorldRule(), properties);
                case "STORY_EVENT" -> readStoryEvent(node.getStoryEvent(), properties);
                default -> {
                    // Unknown types keep their stored properties; validation reports them
                }
            }

            // Extra properties stored on the node are kept, entity fields win
            for (Map.Entry<String, Object> extra : dto.getProperties().entrySet()) {
                properties.putIfAbsent(extra.getKey(), extra.getValue());
            }

            dto.setProperties(properties);
        }

        return graph;
    }

    // Save nodes and edges, then create, update or delete the entity rows
    @Override
    public Map<String, Integer> apply(ProjectOutput output, GraphDefinitionDTO graph) {
        return graphApplier.apply(output, graph, new StoryMapHooks());
    }

    private class StoryMapHooks implements GraphApplyHooks {

        // Entity fields live in their own tables; only extra properties go into dataJson
        @Override
        public Map<String, Object> storedNodeProperties(GraphDefinitionDTO.NodeDTO node) {

            Map<String, Object> extra = new LinkedHashMap<>(node.getProperties());
            ENTITY_KEYS.getOrDefault(node.getType(), List.of()).forEach(extra::remove);

            return extra;
        }

        @Override
        public void afterNodeSaved(ProjectNode node, GraphDefinitionDTO.NodeDTO dto) {

            rejectTypeChange(node, dto.getType());
            Map<String, Object> properties = dto.getProperties();

            switch (dto.getType()) {
                case "CHARACTER" -> saveCharacter(node, properties);
                case "WORLD" -> saveWorld(node, properties);
                case "LOCATION" -> saveLocation(node, properties);
                case "WORLD_RULE" -> saveWorldRule(node, properties);
                case "STORY_EVENT" -> saveStoryEvent(node, properties);
                default -> throw new ApiConflictException("Unsupported story map node type");
            }
        }

        // Every element of the new graph exists now, so worlds and parent locations can be linked
        @Override
        public void afterElementsSaved(ProjectOutput output, Map<String, ProjectNode> nodesByKey, GraphDefinitionDTO graph) {

            for (GraphDefinitionDTO.NodeDTO dto : graph.getNodes()) {

                ProjectNode node = nodesByKey.get(dto.getKey());
                String worldKey = text(dto.getProperties(), "worldKey");
                World world = worldKey == null ? null : nodesByKey.get(worldKey).getWorld();

                if ("LOCATION".equals(dto.getType())) {
                    String parentKey = text(dto.getProperties(), "parentLocationKey");
                    Location location = node.getLocation();
                    location.setWorld(world);
                    location.setParentLocation(parentKey == null ? null : nodesByKey.get(parentKey).getLocation());
                    locationRepository.save(location);
                }

                if ("WORLD_RULE".equals(dto.getType())) {
                    WorldRule rule = node.getWorldRule();
                    rule.setWorld(world);
                    worldRuleRepository.save(rule);
                }
            }
        }

        // Deleting the ProjectNode also deletes its entity row; refuse when other diagrams still use it
        @Override
        public void beforeNodeDelete(ProjectNode node) {

            CharacterProfile character = node.getCharacterProfile();
            if (character != null && (branchingNodeRepository.existsBySpeaker_Id(character.getId())
                    || playableCharacterRepository.existsByCharacterProfile_Id(character.getId()))) {
                throw new ApiConflictException("Character " + character.getName()
                        + " is used by a branching story or playable character. Unlink it before deleting it");
            }

            StoryEvent event = node.getStoryEvent();
            if (event != null && branchingNodeRepository.existsByRelatedStoryEvent_Id(event.getId())) {
                throw new ApiConflictException("Story event " + event.getTitle()
                        + " is used by a branching story. Unlink it before deleting it");
            }
        }
    }

    // An existing element cannot become a different kind of element
    private void rejectTypeChange(ProjectNode node, String type) {

        boolean changed = (node.getCharacterProfile() != null && !"CHARACTER".equals(type))
                || (node.getWorld() != null && !"WORLD".equals(type))
                || (node.getLocation() != null && !"LOCATION".equals(type))
                || (node.getWorldRule() != null && !"WORLD_RULE".equals(type))
                || (node.getStoryEvent() != null && !"STORY_EVENT".equals(type));

        if (changed) {
            throw new ApiConflictException("Story elements cannot change their type. Create a new element instead");
        }
    }

    private void readCharacter(CharacterProfile character, Map<String, Object> properties) {

        if (character == null) {
            return;
        }

        putIfPresent(properties, "label", character.getName());
        putIfPresent(properties, "shortDescription", character.getShortDescription());
        putIfPresent(properties, "role", character.getRole());
        putIfPresent(properties, "aliases", character.getAliases());
        putIfPresent(properties, "age", character.getAge());
        putIfPresent(properties, "appearance", character.getAppearance());
        putIfPresent(properties, "traits", character.getTraits());
        putIfPresent(properties, "strengths", character.getStrengths());
        putIfPresent(properties, "weaknesses", character.getWeaknesses());
        putIfPresent(properties, "skills", character.getSkills());
        putIfPresent(properties, "history", character.getHistory());
        putIfPresent(properties, "goals", character.getGoals());
        putIfPresent(properties, "fears", character.getFears());
        putIfPresent(properties, "innerConflict", character.getInnerConflict());
        putIfPresent(properties, "secrets", character.getSecrets());
        putIfPresent(properties, "characterValues", character.getCharacterValues());
        putIfPresent(properties, "characterArc", character.getCharacterArc());
        putIfPresent(properties, "writerNotes", character.getWriterNotes());
    }

    private void saveCharacter(ProjectNode node, Map<String, Object> properties) {

        CharacterProfile character = node.getCharacterProfile();
        if (character == null) {
            character = new CharacterProfile();
            character.setCreatedAt(LocalDate.now());
            character.setProjectNode(node);
            node.setCharacterProfile(character);
        }

        character.setName(text(properties, "label"));
        character.setShortDescription(text(properties, "shortDescription"));
        character.setRole(text(properties, "role"));
        character.setAliases(text(properties, "aliases"));
        character.setAge(integer(properties, "age"));
        character.setAppearance(text(properties, "appearance"));
        character.setTraits(textOrList(properties, "traits"));
        character.setStrengths(textOrList(properties, "strengths"));
        character.setWeaknesses(textOrList(properties, "weaknesses"));
        character.setSkills(textOrList(properties, "skills"));
        character.setHistory(text(properties, "history"));
        character.setGoals(textOrList(properties, "goals"));
        character.setFears(textOrList(properties, "fears"));
        character.setInnerConflict(text(properties, "innerConflict"));
        character.setSecrets(textOrList(properties, "secrets"));
        character.setCharacterValues(textOrList(properties, "characterValues"));
        character.setCharacterArc(text(properties, "characterArc"));
        character.setWriterNotes(text(properties, "writerNotes"));
        character.setUpdatedAt(LocalDate.now());

        characterProfileRepository.save(character);
    }

    private void readWorld(World world, Map<String, Object> properties) {

        if (world == null) {
            return;
        }

        putIfPresent(properties, "label", world.getName());
        putIfPresent(properties, "description", world.getDescription());
        putIfPresent(properties, "history", world.getHistory());
        putIfPresent(properties, "lore", world.getLore());
        putIfPresent(properties, "atmosphere", world.getAtmosphere());
        putIfPresent(properties, "notes", world.getNotes());
    }

    private void saveWorld(ProjectNode node, Map<String, Object> properties) {

        World world = node.getWorld();
        if (world == null) {
            world = new World();
            world.setCreatedAt(LocalDate.now());
            world.setProjectNode(node);
            node.setWorld(world);
        }

        world.setName(text(properties, "label"));
        world.setDescription(text(properties, "description"));
        world.setHistory(text(properties, "history"));
        world.setLore(text(properties, "lore"));
        world.setAtmosphere(text(properties, "atmosphere"));
        world.setNotes(text(properties, "notes"));
        world.setUpdatedAt(LocalDate.now());

        worldRepository.save(world);
    }

    private void readLocation(Location location, Map<String, Object> properties) {

        if (location == null) {
            return;
        }

        putIfPresent(properties, "label", location.getName());
        putIfPresent(properties, "locationType", location.getLocationType());
        putIfPresent(properties, "narrativeSignificance", location.getNarrativeSignificance());
        putIfPresent(properties, "notes", location.getNotes());

        if (location.getWorld() != null && location.getWorld().getProjectNode() != null) {
            properties.put("worldKey", "node:" + location.getWorld().getProjectNode().getId());
        }
        if (location.getParentLocation() != null && location.getParentLocation().getProjectNode() != null) {
            properties.put("parentLocationKey", "node:" + location.getParentLocation().getProjectNode().getId());
        }
    }

    // World and parent location are linked later in afterElementsSaved
    private void saveLocation(ProjectNode node, Map<String, Object> properties) {

        Location location = node.getLocation();
        if (location == null) {
            location = new Location();
            location.setCreatedAt(LocalDate.now());
            location.setProjectNode(node);
            node.setLocation(location);
        }

        location.setName(text(properties, "label"));
        location.setLocationType(text(properties, "locationType"));
        location.setNarrativeSignificance(text(properties, "narrativeSignificance"));
        location.setNotes(text(properties, "notes"));
        location.setUpdateAt(LocalDate.now());

        locationRepository.save(location);
    }

    private void readWorldRule(WorldRule rule, Map<String, Object> properties) {

        if (rule == null) {
            return;
        }

        putIfPresent(properties, "label", rule.getName());
        putIfPresent(properties, "description", rule.getDescription());
        putIfPresent(properties, "scope", rule.getScope());
        putIfPresent(properties, "conditions", rule.getConditions());
        putIfPresent(properties, "exceptions", rule.getExceptions());
        putIfPresent(properties, "consequences", rule.getConsequences());
        putIfPresent(properties, "notes", rule.getNotes());

        if (rule.getWorld() != null && rule.getWorld().getProjectNode() != null) {
            properties.put("worldKey", "node:" + rule.getWorld().getProjectNode().getId());
        }
    }

    // World is linked later in afterElementsSaved
    private void saveWorldRule(ProjectNode node, Map<String, Object> properties) {

        WorldRule rule = node.getWorldRule();
        if (rule == null) {
            rule = new WorldRule();
            rule.setCreatedAt(LocalDate.now());
            rule.setProjectNode(node);
            node.setWorldRule(rule);
        }

        rule.setName(text(properties, "label"));
        rule.setDescription(text(properties, "description"));
        rule.setScope(textOrList(properties, "scope"));
        rule.setConditions(textOrList(properties, "conditions"));
        rule.setExceptions(textOrList(properties, "exceptions"));
        rule.setConsequences(textOrList(properties, "consequences"));
        rule.setNotes(text(properties, "notes"));
        rule.setUpdatedAt(LocalDate.now());

        worldRuleRepository.save(rule);
    }

    private void readStoryEvent(StoryEvent event, Map<String, Object> properties) {

        if (event == null) {
            return;
        }

        putIfPresent(properties, "label", event.getTitle());
        putIfPresent(properties, "description", event.getDescription());
        putIfPresent(properties, "eventType", event.getEventType());
        putIfPresent(properties, "presentationOrder", event.getPresentationOrder());
        putIfPresent(properties, "chronologicalOrder", event.getChronologicalOrder());
        putIfPresent(properties, "inWorldTime", event.getInWorldTime());
        putIfPresent(properties, "notes", event.getNotes());
    }

    private void saveStoryEvent(ProjectNode node, Map<String, Object> properties) {

        StoryEvent event = node.getStoryEvent();
        if (event == null) {
            event = new StoryEvent();
            event.setCreatedAt(LocalDate.now());
            event.setProjectNode(node);
            node.setStoryEvent(event);
        }

        event.setTitle(text(properties, "label"));
        event.setDescription(text(properties, "description"));
        event.setEventType(text(properties, "eventType"));
        event.setPresentationOrder(integer(properties, "presentationOrder"));
        event.setChronologicalOrder(integer(properties, "chronologicalOrder"));
        event.setInWorldTime(text(properties, "inWorldTime"));
        event.setNotes(text(properties, "notes"));
        event.setUpdatedAt(LocalDate.now());

        storyEventRepository.save(event);
    }

    private void putIfPresent(Map<String, Object> properties, String key, Object value) {

        if (value != null) {
            properties.put(key, value);
        }
    }
}
