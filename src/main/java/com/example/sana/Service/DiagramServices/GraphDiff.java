package com.example.sana.Service.DiagramServices;

import com.example.sana.DTO.GraphDefinitionDTO;
import com.example.sana.Enums.*;
import com.example.sana.Model.ChangeProposalItem;
import com.example.sana.Service.IntegrationServices.AIJson;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.function.Function;

@Service
@RequiredArgsConstructor
public class GraphDiff {
    private final AIJson json;

    public List<ChangeProposalItem> compute(GraphDefinitionDTO before, GraphDefinitionDTO after, Integer outputId) {
        List<ChangeProposalItem> changes = new ArrayList<>();
        compare(before == null ? List.of() : before.getNodes(), after.getNodes(), GraphDefinitionDTO.NodeDTO::getKey,
                GraphDefinitionDTO.NodeDTO::getId, ChangeTargetType.NODE, changes);
        compare(before == null ? List.of() : before.getEdges(), after.getEdges(), GraphDefinitionDTO.EdgeDTO::getKey,
                GraphDefinitionDTO.EdgeDTO::getId, ChangeTargetType.EDGE, changes);
        Object oldProperties = before == null ? null : before.getProperties();
        if (!json.hash(oldProperties).equals(json.hash(after.getProperties())))
            changes.add(item(before == null ? ChangeOperation.CREATE : ChangeOperation.UPDATE, ChangeTargetType.OUTPUT, outputId, oldProperties, after.getProperties()));
        return changes;
    }

    private <T> void compare(List<T> before, List<T> after, Function<T, String> key, Function<T, Integer> id,
                             ChangeTargetType type, List<ChangeProposalItem> changes) {
        Map<String, T> old = new LinkedHashMap<>(); before.forEach(value -> old.put(key.apply(value), value));
        for (T value : after) {
            T previous = old.remove(key.apply(value));
            if (previous == null) changes.add(item(ChangeOperation.CREATE, type, null, null, value));
            else if (!json.hash(previous).equals(json.hash(value))) changes.add(item(ChangeOperation.UPDATE, type, id.apply(value), previous, value));
        }
        old.values().forEach(value -> changes.add(item(ChangeOperation.DELETE, type, id.apply(value), value, null)));
    }

    private ChangeProposalItem item(ChangeOperation operation, ChangeTargetType type, Integer id, Object before, Object after) {
        var item = new ChangeProposalItem(); item.setOperation(operation); item.setTargetType(type); item.setTargetId(id);
        item.setBeforeJson(before == null ? null : json.write(before)); item.setAfterJson(after == null ? null : json.write(after)); return item;
    }
}
