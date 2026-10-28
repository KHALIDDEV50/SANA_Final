package com.example.sana.Service.TechDomainServices;

import com.example.sana.API.ApiException;
import com.example.sana.Enums.TechDomainEnums.TechnicalOutputType;
import com.example.sana.Service.DiagramServices.BasicDiagramRules;
import com.example.sana.Service.DiagramServices.GraphRuleChecks;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

// Rules for the TECH Entity Relationship Diagram: entities with attributes and their relationships.
@Component
public class ErdRules extends BasicDiagramRules {

    public ErdRules() {
        super("TECH",
                TechnicalOutputType.ERD.name(),
                List.of("ENTITY"),
                List.of("ONE_TO_ONE", "ONE_TO_MANY", "MANY_TO_MANY"),
                "Entity relationship diagram. Each ENTITY node is a table; properties.label is the entity name. "
                        + "properties.attributes is a list of {\"name\", \"dataType\", \"primaryKey\": true|false, \"nullable\": true|false}; "
                        + "attribute names are unique within the entity and every entity has at least one primary key attribute. "
                        + "Edges are relationships between two different entities: ONE_TO_ONE, ONE_TO_MANY (from the one side to the many side) "
                        + "or MANY_TO_MANY. New IDs must be null.");
    }

    // An entity needs named, typed attributes with at least one primary key
    @Override
    public void validateNode(String type, Map<String, Object> properties, Double positionX, Double positionY) {

        super.validateNode(type, properties, positionX, positionY);

        String entity = String.valueOf(properties.get("label"));
        List<Map<String, Object>> attributes = GraphRuleChecks.objectList(properties, "attributes");

        boolean hasPrimaryKey = false;

        for (Map<String, Object> attribute : attributes) {
            GraphRuleChecks.requireText(attribute.get("name"), 100, "Attribute name in " + entity);
            GraphRuleChecks.requireText(attribute.get("dataType"), 50, "Attribute dataType in " + entity);
            GraphRuleChecks.optionalBoolean(attribute, "primaryKey");
            GraphRuleChecks.optionalBoolean(attribute, "nullable");

            if (Boolean.TRUE.equals(attribute.get("primaryKey"))) {
                hasPrimaryKey = true;
            }
        }

        GraphRuleChecks.requireUniqueNames(attributes, "Attribute", entity);

        if (!hasPrimaryKey) {
            throw new ApiException("Entity " + entity + " needs at least one primary key attribute");
        }
    }
}
