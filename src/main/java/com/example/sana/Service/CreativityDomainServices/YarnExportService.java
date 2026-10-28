package com.example.sana.Service.CreativityDomainServices;

import com.example.sana.API.ApiException;
import com.example.sana.Enums.CreativityDomainEnums.BranchingComparisonOperator;
import com.example.sana.Enums.CreativityDomainEnums.BranchingEffectOperator;
import com.example.sana.Enums.CreativityDomainEnums.BranchingNodeType;
import com.example.sana.Repository.ProjectOutputRepository;
import com.example.sana.Repository.CreativityDomainRepository.BranchingNodeRepository;
import com.example.sana.Repository.CreativityDomainRepository.BranchingChoiceRepository;
import com.example.sana.Repository.CreativityDomainRepository.BranchingVariableRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@RequiredArgsConstructor
public class YarnExportService {

    private final ProjectOutputRepository projectOutputRepository;
    private final BranchingNodeRepository branchingNodeRepository;
    private final BranchingChoiceRepository branchingChoiceRepository;
    private final BranchingVariableRepository branchingVariableRepository;

    // Export the selected game output into Yarn Spinner source.
    // startNodeId is BranchingNode.id, not ProjectNode.id.
    @Transactional(readOnly = true)
    public String exportYarn(Integer outputId, Integer startNodeId) {

        // STEP 1: Validate the selected output.
        if (outputId == null || !projectOutputRepository.existsById(outputId)) {
            throw new ApiException("Project output not found");
        }

        // Fetch only the columns needed for exporting.
        // This avoids loading large entity relationship graphs.
        List<Object[]> nodes = branchingNodeRepository.findYarnExportNodes(outputId);
        List<Object[]> choices = branchingChoiceRepository.findYarnExportChoices(outputId);
        List<Object[]> variables = branchingVariableRepository.findYarnExportVariables(outputId);

        if (nodes.isEmpty()) {
            throw new ApiException("Cannot export an empty branching narrative");
        }

        // STEP 2: Give each scene a stable Yarn node name.
        // ProjectNode IDs identify choice connections.
        // BranchingNode IDs identify the exported scene names.
        Map<Integer, String> sceneNames = new LinkedHashMap<>();
        Map<Integer, BranchingNodeType> sceneTypes = new HashMap<>();
        Set<Integer> branchingIds = new HashSet<>();

        for (Object[] node : nodes) {
            Integer branchingId = (Integer) node[0];
            Integer projectNodeId = (Integer) node[4];

            branchingIds.add(branchingId);
            sceneNames.put(projectNodeId, "Scene_" + branchingId);
            sceneTypes.put(projectNodeId, (BranchingNodeType) node[3]);
        }

        // If no starting scene is specified, use the first one.
        // The frontend can also let the user select a start scene.
        Integer selectedStart = startNodeId == null ? (Integer) nodes.get(0)[0] : startNodeId;

        if (!branchingIds.contains(selectedStart)) {
            throw new ApiException("Starting scene not found in this output");
        }

        // STEP 3: Validate variable names before generating commands.
        // Yarn variables must use supported identifiers.
        Set<String> variableNames = new HashSet<>();

        for (Object[] variable : variables) {
            String name = (String) variable[0];

            validateVariableName(name);

            if (variable[1] == null || !variableNames.add(name)) {
                throw new ApiException("Invalid or duplicate game variable: " + name);
            }
        }

        // STEP 4: Group choices by their source ProjectNode ID.
        // Check destinations and variable references before export.
        Map<Integer, List<Object[]>> choicesBySource = new HashMap<>();

        for (Object[] choice : choices) {
            Integer sourceId = (Integer) choice[3];
            Integer targetId = (Integer) choice[4];

            if (!sceneNames.containsKey(sourceId) || !sceneNames.containsKey(targetId)) {
                throw new ApiException("Choice contains a missing or invalid scene");
            }

            if (sceneTypes.get(sourceId) == BranchingNodeType.ENDING) {
                throw new ApiException("Ending scenes cannot contain outgoing choices");
            }

            // Validate the choice without changing any saved data.
            safeText((String) choice[1]);
            validateChoiceVariables(choice, variableNames);

            choicesBySource.computeIfAbsent(sourceId, id -> new ArrayList<>()).add(choice);
        }


        // STEP 5: Generate Yarn source.
        StringBuilder yarn = new StringBuilder();

        yarn.append("// Exported from SANA Branching Narrative\n");
        yarn.append("// Starting scene: Scene_").append(selectedStart).append("\n\n");

        // Create the starting Yarn node.
        yarn.append("title: Start\n");
        yarn.append("---\n");


        // Declare game variables only once, inside the starting node.
        for (Object[] variable : variables) {
            yarn.append("<<declare $").append(variable[0]).append(" = ")
                    .append(variable[1]).append(">>\n");
        }

        // Jump to the selected starting scene.
        yarn.append("<<jump Scene_").append(selectedStart).append(">>\n");
        yarn.append("===\n\n");

        // STEP 6: Export each branching scene as a Yarn node.
        for (Object[] node : nodes) {
            Integer branchingId = (Integer) node[0];
            Integer projectNodeId = (Integer) node[4];
            String content = (String) node[2];
            String speaker = (String) node[5];

            yarn.append("title: Scene_").append(branchingId).append("\n");
            yarn.append("---\n");

            // Preserve the dialogue content and optional speaker.
            appendDialogue(yarn, content, speaker);

            // ENDING scenes and scenes without choices end here.
            List<Object[]> outgoing = choicesBySource.getOrDefault(projectNodeId, List.of());

            if (outgoing.isEmpty()) {
                yarn.append("<<stop>>\n");
            } else {
                // Export all choices in the saved display order.
                for (Object[] choice : outgoing) {
                    appendChoice(yarn, choice, sceneNames);
                }
            }

            yarn.append("===\n\n");
        }

        // No save() is called; exporting never changes the database.
        return yarn.toString();
    }

    // Write narrative content as safe dialogue lines.
    // A linked speaker takes priority; otherwise use Narrator.
    private void appendDialogue(StringBuilder yarn, String content, String speaker) {
        if (content == null || content.isBlank()) {
            throw new ApiException("Cannot export a scene without content");
        }

        String speakerName = speaker == null || speaker.isBlank() ? "Narrator" : safeText(speaker);

        // Preserve separate dialogue lines in the exported story.
        // Each line is prefixed to avoid treating user text as Yarn commands.
        for (String line : content.split("\\R")) {
            if (!line.isBlank()) {
                yarn.append(speakerName).append(": ").append(safeText(line)).append("\n");
            }
        }
    }

    // Convert a saved SANA choice into a Yarn option.
    // Each option can have a condition, an effect and a jump.
    private void appendChoice(StringBuilder yarn, Object[] choice, Map<Integer, String> sceneNames) {
        String text = safeText((String) choice[1]);
        String conditionVariable = (String) choice[5];
        BranchingComparisonOperator conditionOperator = (BranchingComparisonOperator) choice[6];
        Integer conditionValue = (Integer) choice[7];

        String effectVariable = (String) choice[8];
        BranchingEffectOperator effectOperator = (BranchingEffectOperator) choice[9];
        Integer effectValue = (Integer) choice[10];
        Integer targetNodeId = (Integer) choice[4];

        // Write the displayed player choice.
        yarn.append("-> ").append(text);

        // Conditional options are enabled when their expressions pass.
        if (conditionVariable != null) {
            yarn.append(" <<if $").append(conditionVariable).append(" ")
                    .append(comparisonSymbol(conditionOperator)).append(" ")
                    .append(conditionValue).append(">>");
        }

        yarn.append("\n");

        // Apply variable changes only after the player selects the option.
        if (effectVariable != null) {
            switch (effectOperator) {
                case SET:
                    yarn.append("    <<set $").append(effectVariable).append(" to ")
                            .append(effectValue).append(">>\n");
                    break;

                case ADD:
                    yarn.append("    <<set $").append(effectVariable).append(" to $")
                            .append(effectVariable);

                    // Use subtraction for negative ADD values.
                    if (effectValue >= 0) {
                        yarn.append(" + ").append(effectValue);
                    } else {
                        yarn.append(" - ").append(Math.abs((long) effectValue));
                    }

                    yarn.append(">>\n");
                    break;

                default:
                    throw new ApiException("Unsupported choice effect");
            }
        }

        // Move the dialogue to the chosen destination scene.
        yarn.append("    <<jump ").append(sceneNames.get(targetNodeId)).append(">>\n");
    }

    // Map SANA comparison operators to Yarn expression syntax.
    private String comparisonSymbol(BranchingComparisonOperator operator) {
        if (operator == null) {
            throw new ApiException("Choice comparison operator is missing");
        }

        switch (operator) {
            case EQ:
                return "==";
            case NE:
                return "!=";
            case GT:
                return ">";
            case GTE:
                return ">=";
            case LT:
                return "<";
            case LTE:
                return "<=";
            default:
                throw new ApiException("Unsupported comparison operator");
        }
    }

    // Check that each condition/effect references a valid variable.
    // Partial conditions or effects would generate broken Yarn code.
    private void validateChoiceVariables(Object[] choice, Set<String> variableNames) {
        String conditionName = (String) choice[5];
        Object conditionOperator = choice[6];
        Object conditionValue = choice[7];

        String effectName = (String) choice[8];
        Object effectOperator = choice[9];
        Object effectValue = choice[10];

        if (conditionName == null) {
            if (conditionOperator != null || conditionValue != null) {
                throw new ApiException("Choice condition is incomplete");
            }
        } else if (!variableNames.contains(conditionName) || conditionOperator == null || conditionValue == null) {
            throw new ApiException("Choice contains an invalid condition");
        }

        if (effectName == null) {
            if (effectOperator != null || effectValue != null) {
                throw new ApiException("Choice effect is incomplete");
            }
        } else if (!variableNames.contains(effectName) || effectOperator == null || effectValue == null) {
            throw new ApiException("Choice contains an invalid effect");
        }
    }

    // Accept variable identifiers supported by Yarn Spinner.
    // We restrict names to a portable subset.
    private void validateVariableName(String name) {
        if (name == null || !name.matches("[A-Za-z][A-Za-z0-9_]*")) {
            throw new ApiException("Invalid Yarn variable name: " + name);
        }
    }

    // Prevent stored narrative text from being interpreted as
    // an executable Yarn command or expression during export.
    private String safeText(String value) {
        if (value == null || value.isBlank()) {
            throw new ApiException("Yarn dialogue text cannot be empty");
        }

        if (value.contains("<<") || value.contains(">>") || value.contains("{") || value.contains("}")) {
            throw new ApiException("Text contains Yarn syntax that requires escaping");
        }

        return value.trim().replace("\r", " ").replace("\n", " ");
    }
}
