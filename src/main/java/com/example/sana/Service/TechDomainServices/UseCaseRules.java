package com.example.sana.Service.TechDomainServices;

import com.example.sana.API.ApiException;
import com.example.sana.DTO.GraphDefinitionDTO;
import com.example.sana.Enums.TechDomainEnums.TechnicalOutputType;
import com.example.sana.Service.DiagramServices.BasicDiagramRules;
import com.example.sana.Service.DiagramServices.GraphRuleChecks;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

// Rules for the TECH Use Case Diagram: actors, use cases and how they relate.
@Component
public class UseCaseRules extends BasicDiagramRules {

    public UseCaseRules() {
        super("TECH",
                TechnicalOutputType.USE_CASE.name(),
                List.of("ACTOR", "USE_CASE"),
                List.of("ASSOCIATION", "INCLUDE", "EXTEND", "GENERALIZATION"),
                "UML use case diagram. ACTOR nodes are users or external systems; USE_CASE nodes are goals the system supports. "
                        + "properties.label is the name and properties.description is optional. The system name goes in the graph's "
                        + "properties.systemName. ASSOCIATION goes from an ACTOR to a USE_CASE. INCLUDE and EXTEND go from one USE_CASE to "
                        + "another. GENERALIZATION goes from a specialized ACTOR to a general ACTOR, or between two USE_CASEs. New IDs must be null.");
    }

    // Each relationship only connects the kinds of nodes UML allows
    @Override
    public void validateEdge(String type, String label, String sourceType, String targetType, Map<String, Object> properties) {

        super.validateEdge(type, label, sourceType, targetType, properties);

        switch (type) {
            case "ASSOCIATION" -> {
                if (!"ACTOR".equals(sourceType) || !"USE_CASE".equals(targetType)) {
                    throw new ApiException("ASSOCIATION goes from an ACTOR to a USE_CASE");
                }
            }
            case "INCLUDE", "EXTEND" -> {
                if (!"USE_CASE".equals(sourceType) || !"USE_CASE".equals(targetType)) {
                    throw new ApiException(type + " connects two use cases");
                }
            }
            case "GENERALIZATION" -> {
                if (!sourceType.equals(targetType)) {
                    throw new ApiException("GENERALIZATION connects two actors or two use cases");
                }
            }
            default -> throw new ApiException("Unsupported use case edge type");
        }
    }

    // The optional system name must be short text
    @Override
    public void validateGraph(GraphDefinitionDTO graph) {
        GraphRuleChecks.optionalText(graph.getProperties(), "systemName", 255);
    }
}
