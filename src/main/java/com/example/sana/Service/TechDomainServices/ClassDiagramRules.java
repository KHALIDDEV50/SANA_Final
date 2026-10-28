package com.example.sana.Service.TechDomainServices;

import com.example.sana.API.ApiException;
import com.example.sana.DTO.GraphDefinitionDTO;
import com.example.sana.Enums.TechDomainEnums.TechnicalOutputType;
import com.example.sana.Service.DiagramServices.BasicDiagramRules;
import com.example.sana.Service.DiagramServices.GraphRuleChecks;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

// Rules for the TECH Class Diagram: classes, interfaces and enums with their members and relationships.
@Component
public class ClassDiagramRules extends BasicDiagramRules {

    private static final List<String> CLASS_TYPES = List.of("CLASS", "ABSTRACT_CLASS");
    private static final List<String> VISIBILITIES = List.of("PUBLIC", "PROTECTED", "PACKAGE", "PRIVATE");

    public ClassDiagramRules() {
        super("TECH",
                TechnicalOutputType.CLASS_DIAGRAM.name(),
                List.of("CLASS", "ABSTRACT_CLASS", "INTERFACE", "ENUM"),
                List.of("INHERITANCE", "IMPLEMENTATION", "ASSOCIATION", "AGGREGATION", "COMPOSITION", "DEPENDENCY"),
                "UML class diagram. properties.label is the type name. properties.attributes is a list of "
                        + "{\"name\", \"type\", \"visibility\": PUBLIC|PROTECTED|PACKAGE|PRIVATE, \"static\": true|false}. "
                        + "properties.methods is a list of {\"name\", \"returnType\", \"parameters\": [{\"name\", \"type\"}], \"visibility\", "
                        + "\"static\", \"abstract\"}. ENUM nodes list their constants in properties.values. "
                        + "INHERITANCE goes from the subtype to its parent: CLASS or ABSTRACT_CLASS to CLASS or ABSTRACT_CLASS, or INTERFACE to "
                        + "INTERFACE, without cycles. IMPLEMENTATION goes from a CLASS, ABSTRACT_CLASS or ENUM to an INTERFACE. "
                        + "ASSOCIATION, AGGREGATION, COMPOSITION and DEPENDENCY may connect any two types; properties.multiplicity is optional. "
                        + "New IDs must be null.");
    }

    // Check the members of a class, interface or enum
    @Override
    public void validateNode(String type, Map<String, Object> properties, Double positionX, Double positionY) {

        super.validateNode(type, properties, positionX, positionY);

        String owner = String.valueOf(properties.get("label"));

        List<Map<String, Object>> attributes = GraphRuleChecks.objectList(properties, "attributes");
        for (Map<String, Object> attribute : attributes) {
            GraphRuleChecks.requireText(attribute.get("name"), 100, "Attribute name in " + owner);
            GraphRuleChecks.requireText(attribute.get("type"), 100, "Attribute type in " + owner);
            GraphRuleChecks.optionalChoice(attribute, "visibility", VISIBILITIES);
            GraphRuleChecks.optionalBoolean(attribute, "static");
        }
        GraphRuleChecks.requireUniqueNames(attributes, "Attribute", owner);

        for (Map<String, Object> method : GraphRuleChecks.objectList(properties, "methods")) {
            GraphRuleChecks.requireText(method.get("name"), 100, "Method name in " + owner);
            GraphRuleChecks.requireText(method.get("returnType"), 100, "Method returnType in " + owner);
            GraphRuleChecks.optionalChoice(method, "visibility", VISIBILITIES);
            GraphRuleChecks.optionalBoolean(method, "static");
            GraphRuleChecks.optionalBoolean(method, "abstract");

            for (Map<String, Object> parameter : GraphRuleChecks.objectList(method, "parameters")) {
                GraphRuleChecks.requireText(parameter.get("name"), 100, "Parameter name in " + owner);
                GraphRuleChecks.requireText(parameter.get("type"), 100, "Parameter type in " + owner);
            }
        }

        Object values = properties.get("values");
        if (values != null) {
            if (!"ENUM".equals(type)) {
                throw new ApiException("Only ENUM nodes can have values");
            }
            if (!(values instanceof List<?> list) || list.size() > 200) {
                throw new ApiException("Enum values must be a list of names");
            }
            for (Object value : list) {
                GraphRuleChecks.requireText(value, 100, "Enum value in " + owner);
            }
        }
    }

    // INHERITANCE and IMPLEMENTATION only connect compatible kinds of types
    @Override
    public void validateEdge(String type, String label, String sourceType, String targetType, Map<String, Object> properties) {

        super.validateEdge(type, label, sourceType, targetType, properties);

        if ("INHERITANCE".equals(type)) {
            boolean classes = CLASS_TYPES.contains(sourceType) && CLASS_TYPES.contains(targetType);
            boolean interfaces = "INTERFACE".equals(sourceType) && "INTERFACE".equals(targetType);

            if (!classes && !interfaces) {
                throw new ApiException("INHERITANCE connects two classes or two interfaces");
            }
        }

        if ("IMPLEMENTATION".equals(type) && ("INTERFACE".equals(sourceType) || !"INTERFACE".equals(targetType))) {
            throw new ApiException("IMPLEMENTATION goes from a class or enum to an INTERFACE");
        }

        GraphRuleChecks.optionalText(properties, "multiplicity", 50);
    }

    // A type cannot inherit from itself through a chain of parents
    @Override
    public void validateGraph(GraphDefinitionDTO graph) {

        Map<String, Set<String>> parents = new HashMap<>();

        for (GraphDefinitionDTO.EdgeDTO edge : graph.getEdges()) {
            if ("INHERITANCE".equals(edge.getType())) {
                parents.computeIfAbsent(edge.getSourceKey(), key -> new HashSet<>()).add(edge.getTargetKey());
            }
        }

        Set<String> finished = new HashSet<>();
        for (String key : parents.keySet()) {
            checkNoCycle(key, parents, new HashSet<>(), finished);
        }
    }

    private void checkNoCycle(String key, Map<String, Set<String>> parents, Set<String> path, Set<String> finished) {

        if (finished.contains(key)) {
            return;
        }

        if (!path.add(key)) {
            throw new ApiException("Inheritance cycles are not allowed");
        }

        for (String parent : parents.getOrDefault(key, Set.of())) {
            checkNoCycle(parent, parents, path, finished);
        }

        path.remove(key);
        finished.add(key);
    }
}
