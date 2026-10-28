package com.example.sana.Service.CreativityDomainServices;

import com.example.sana.API.ApiException;
import com.example.sana.DTO.GraphDefinitionDTO;
import com.example.sana.Enums.CreativityDomainEnums.BranchingComparisonOperator;
import com.example.sana.Enums.CreativityDomainEnums.BranchingEffectOperator;
import com.example.sana.Enums.CreativityDomainEnums.BranchingNodeType;
import com.example.sana.Enums.CreativityDomainEnums.CreativityOutputType;
import com.example.sana.Model.CreativityDomainModels.BranchingChoice;
import com.example.sana.Model.CreativityDomainModels.BranchingNode;
import com.example.sana.Model.CreativityDomainModels.BranchingVariable;
import com.example.sana.Model.CreativityDomainModels.CharacterProfile;
import com.example.sana.Model.CreativityDomainModels.StoryEvent;
import com.example.sana.Model.Project;
import com.example.sana.Model.ProjectEdge;
import com.example.sana.Model.ProjectNode;
import com.example.sana.Model.ProjectOutput;
import com.example.sana.Repository.CreativityDomainRepository.BranchingChoiceRepository;
import com.example.sana.Repository.CreativityDomainRepository.BranchingNodeRepository;
import com.example.sana.Repository.CreativityDomainRepository.BranchingVariableRepository;
import com.example.sana.Repository.CreativityDomainRepository.CharacterProfileRepository;
import com.example.sana.Repository.CreativityDomainRepository.StoryEventRepository;
import com.example.sana.Service.DiagramServices.DiagramGraphAdapter;
import com.example.sana.Service.DiagramServices.GraphApplier;
import com.example.sana.Service.DiagramServices.GraphApplyHooks;
import com.example.sana.Service.DiagramServices.GraphMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.example.sana.Service.CreativityDomainServices.CreativityProperties.integer;
import static com.example.sana.Service.CreativityDomainServices.CreativityProperties.text;

// Reads and saves Branching Story diagrams.
// Scenes are BranchingNode rows (ProjectNode type BRANCHING), choices are BranchingChoice rows
// on BRANCH_CHOICE edges, and the game variables are listed in the graph's properties.variables.
@Component
@RequiredArgsConstructor
public class BranchingStoryGraphAdapter implements DiagramGraphAdapter {

    // Type stored on ProjectNode by BranchingNarrativeService; the scene type is on BranchingNode
    private static final String STORED_NODE_TYPE = "BRANCHING";
    private static final List<String> SCENE_KEYS = List.of("label", "content", "speakerCharacterId", "storyEventId");

    private final GraphMapper graphMapper;
    private final GraphApplier graphApplier;
    private final BranchingNodeRepository branchingNodeRepository;
    private final BranchingChoiceRepository branchingChoiceRepository;
    private final BranchingVariableRepository branchingVariableRepository;
    private final CharacterProfileRepository characterProfileRepository;
    private final StoryEventRepository storyEventRepository;

    @Override
    public String getDiagramType() {
        return CreativityOutputType.BRANCHING_STORY.name();
    }

    // Read scenes, choices and variables into the graph format
    @Override
    public GraphDefinitionDTO read(ProjectOutput output) {

        GraphDefinitionDTO graph = graphMapper.toGraphDTO(output);

        Map<Integer, BranchingNode> scenesByNodeId = new HashMap<>();
        for (BranchingNode scene : branchingNodeRepository.findByProjectNode_ProjectOutput_IdOrderByIdAsc(output.getId())) {
            scenesByNodeId.put(scene.getProjectNode().getId(), scene);
        }

        for (GraphDefinitionDTO.NodeDTO dto : graph.getNodes()) {

            BranchingNode scene = scenesByNodeId.get(dto.getId());
            if (scene == null) {
                continue;
            }

            Map<String, Object> properties = new LinkedHashMap<>();
            properties.put("label", scene.getTitle());
            properties.put("content", scene.getContent());
            if (scene.getSpeaker() != null) {
                properties.put("speakerCharacterId", scene.getSpeaker().getId());
            }
            if (scene.getRelatedStoryEvent() != null) {
                properties.put("storyEventId", scene.getRelatedStoryEvent().getId());
            }
            for (Map.Entry<String, Object> extra : dto.getProperties().entrySet()) {
                properties.putIfAbsent(extra.getKey(), extra.getValue());
            }

            dto.setType(scene.getType().name());
            dto.setProperties(properties);
        }

        Map<Integer, BranchingChoice> choicesByEdgeId = new HashMap<>();
        for (BranchingChoice choice : branchingChoiceRepository.findByProjectEdge_ProjectOutput_IdOrderByIdAsc(output.getId())) {
            choicesByEdgeId.put(choice.getProjectEdge().getId(), choice);
        }

        for (GraphDefinitionDTO.EdgeDTO dto : graph.getEdges()) {

            BranchingChoice choice = choicesByEdgeId.get(dto.getId());
            if (choice == null) {
                continue;
            }

            Map<String, Object> properties = new LinkedHashMap<>();
            properties.put("text", choice.getText());
            properties.put("displayOrder", choice.getDisplayOrder());
            if (choice.getConditionVariable() != null) {
                properties.put("condition", variableReference(choice.getConditionVariable(),
                        choice.getConditionOperator().name(), choice.getConditionValue()));
            }
            if (choice.getEffectVariable() != null) {
                properties.put("effect", variableReference(choice.getEffectVariable(),
                        choice.getEffectOperator().name(), choice.getEffectValue()));
            }
            for (Map.Entry<String, Object> extra : dto.getProperties().entrySet()) {
                properties.putIfAbsent(extra.getKey(), extra.getValue());
            }

            dto.setProperties(properties);
        }

        List<Map<String, Object>> variables = new ArrayList<>();
        for (BranchingVariable variable : branchingVariableRepository.findByProjectOutput_IdOrderByIdAsc(output.getId())) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("name", variable.getName());
            item.put("initialValue", variable.getInitialValue());
            variables.add(item);
        }
        graph.getProperties().put("variables", variables);

        return graph;
    }

    // Save scenes, choices and variables from an accepted graph
    @Override
    public Map<String, Integer> apply(ProjectOutput output, GraphDefinitionDTO graph) {
        return graphApplier.apply(output, graph, new BranchingHooks());
    }

    // Characters and story events of the project, so the agent can set speakers and related events
    @Override
    public Map<String, Object> extraContext(Project project) {

        List<Map<String, Object>> characters = new ArrayList<>();
        for (CharacterProfile character : characterProfileRepository.findByProjectNode_ProjectOutput_Project_IdOrderByIdAsc(project.getId())) {
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("id", character.getId());
            entry.put("name", character.getName());
            if (character.getRole() != null) entry.put("role", character.getRole());
            if (character.getShortDescription() != null) entry.put("description", character.getShortDescription().substring(0, Math.min(350, character.getShortDescription().length())));
            if (character.getAliases() != null) entry.put("aliases", character.getAliases());
            if (character.getTraits() != null) entry.put("traits", character.getTraits());
            if (character.getHistory() != null) entry.put("history", character.getHistory().substring(0, Math.min(350, character.getHistory().length())));
            characters.add(entry);
        }

        List<Map<String, Object>> events = new ArrayList<>();
        for (StoryEvent event : storyEventRepository.findByProjectNode_ProjectOutput_Project_IdOrderByIdAsc(project.getId())) {
            events.add(Map.of("id", event.getId(), "title", event.getTitle()));
        }

        Map<String, Object> context = new LinkedHashMap<>();
        context.put("projectCharacters", characters);
        context.put("projectStoryEvents", events);
        return context;
    }

    private Map<String, Object> variableReference(BranchingVariable variable, String operator, Integer value) {

        Map<String, Object> reference = new LinkedHashMap<>();
        reference.put("variable", variable.getName());
        reference.put("operator", operator);
        reference.put("value", value);
        return reference;
    }

    private class BranchingHooks implements GraphApplyHooks {

        // Variables of this output by name, filled in beforeApply
        private final Map<String, BranchingVariable> variablesByName = new HashMap<>();

        // Create or update variables first, because choices point to them
        @Override
        public void beforeApply(ProjectOutput output, GraphDefinitionDTO graph) {

            for (BranchingVariable variable : branchingVariableRepository.findByProjectOutput_IdOrderByIdAsc(output.getId())) {
                variablesByName.put(variable.getName(), variable);
            }

            for (Map<String, Object> item : BranchingStoryRules.readVariables(graph)) {

                String name = text(item, "name");
                BranchingVariable variable = variablesByName.get(name);
                if (variable == null) {
                    variable = new BranchingVariable();
                    variable.setName(name);
                    variable.setProjectOutput(output);
                    variablesByName.put(name, variable);
                }

                variable.setInitialValue(integer(item, "initialValue"));
                branchingVariableRepository.save(variable);
            }
        }

        @Override
        public String storedNodeType(GraphDefinitionDTO.NodeDTO node) {
            return STORED_NODE_TYPE;
        }

        // Scene fields live in BranchingNode; only extra properties go into dataJson
        @Override
        public Map<String, Object> storedNodeProperties(GraphDefinitionDTO.NodeDTO node) {

            Map<String, Object> extra = new LinkedHashMap<>(node.getProperties());
            SCENE_KEYS.forEach(extra::remove);

            return extra;
        }

        @Override
        public void afterNodeSaved(ProjectNode node, GraphDefinitionDTO.NodeDTO dto) {

            BranchingNode scene = branchingNodeRepository.findByProjectNode_Id(node.getId());
            if (scene == null) {
                scene = new BranchingNode();
                scene.setProjectNode(node);
                scene.setCreatedAt(LocalDate.now());
            }

            Map<String, Object> properties = dto.getProperties();
            Integer projectId = node.getProjectOutput().getProject().getId();

            scene.setTitle(text(properties, "label"));
            scene.setContent(text(properties, "content"));
            scene.setType(BranchingNodeType.valueOf(dto.getType()));
            scene.setSpeaker(findCharacter(integer(properties, "speakerCharacterId"), projectId));
            scene.setRelatedStoryEvent(findStoryEvent(integer(properties, "storyEventId"), projectId));
            scene.setUpdatedAt(LocalDate.now());

            branchingNodeRepository.save(scene);
        }

        @Override
        public void afterEdgeSaved(ProjectEdge edge, GraphDefinitionDTO.EdgeDTO dto) {

            BranchingChoice choice = branchingChoiceRepository.findByProjectEdge_Id(edge.getId());
            if (choice == null) {
                choice = new BranchingChoice();
                choice.setProjectEdge(edge);
            }

            Map<String, Object> properties = dto.getProperties();
            choice.setText(text(properties, "text"));
            choice.setDisplayOrder(integer(properties, "displayOrder"));

            Map<String, Object> condition = BranchingStoryRules.optionalObject(properties, "condition");
            if (condition == null) {
                choice.setConditionVariable(null);
                choice.setConditionOperator(null);
                choice.setConditionValue(null);
            } else {
                choice.setConditionVariable(variablesByName.get(text(condition, "variable")));
                choice.setConditionOperator(BranchingStoryRules.readEnum(BranchingComparisonOperator.class, condition.get("operator"), "condition operator"));
                choice.setConditionValue(integer(condition, "value"));
            }

            Map<String, Object> effect = BranchingStoryRules.optionalObject(properties, "effect");
            if (effect == null) {
                choice.setEffectVariable(null);
                choice.setEffectOperator(null);
                choice.setEffectValue(null);
            } else {
                choice.setEffectVariable(variablesByName.get(text(effect, "variable")));
                choice.setEffectOperator(BranchingStoryRules.readEnum(BranchingEffectOperator.class, effect.get("operator"), "effect operator"));
                choice.setEffectValue(integer(effect, "value"));
            }

            branchingChoiceRepository.save(choice);
        }

        // A choice row points to its edge, so it is deleted first
        @Override
        public void beforeEdgeDelete(ProjectEdge edge) {

            BranchingChoice choice = branchingChoiceRepository.findByProjectEdge_Id(edge.getId());
            if (choice != null) {
                branchingChoiceRepository.delete(choice);
                branchingChoiceRepository.flush();
            }
        }

        // A scene row points to its node, so it is deleted first
        @Override
        public void beforeNodeDelete(ProjectNode node) {

            BranchingNode scene = branchingNodeRepository.findByProjectNode_Id(node.getId());
            if (scene != null) {
                branchingNodeRepository.delete(scene);
                branchingNodeRepository.flush();
            }
        }

        // Variables no longer listed are removed; every remaining choice already points to listed variables
        @Override
        public void afterApply(ProjectOutput output, GraphDefinitionDTO graph) {

            Set<String> listed = new HashSet<>();
            for (Map<String, Object> item : BranchingStoryRules.readVariables(graph)) {
                listed.add(text(item, "name"));
            }

            for (BranchingVariable variable : new ArrayList<>(variablesByName.values())) {
                if (!listed.contains(variable.getName())) {
                    branchingVariableRepository.delete(variable);
                }
            }
        }
    }

    private CharacterProfile findCharacter(Integer characterId, Integer projectId) {

        if (characterId == null) {
            return null;
        }

        CharacterProfile character = characterProfileRepository.findCharacterProfileById(characterId);
        if (character == null || character.getProjectNode() == null
                || !projectId.equals(character.getProjectNode().getProjectOutput().getProject().getId())) {
            throw new ApiException("Speaker character " + characterId + " was not found in this project");
        }

        return character;
    }

    private StoryEvent findStoryEvent(Integer storyEventId, Integer projectId) {

        if (storyEventId == null) {
            return null;
        }

        StoryEvent event = storyEventRepository.findStoryEventById(storyEventId);
        if (event == null || event.getProjectNode() == null
                || !projectId.equals(event.getProjectNode().getProjectOutput().getProject().getId())) {
            throw new ApiException("Story event " + storyEventId + " was not found in this project");
        }

        return event;
    }
}
