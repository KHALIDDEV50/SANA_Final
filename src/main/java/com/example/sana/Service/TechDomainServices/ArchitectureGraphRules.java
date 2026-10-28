package com.example.sana.Service.TechDomainServices;

import com.example.sana.API.ApiException;
import com.example.sana.Enums.TechDomainEnums.TechnicalOutputType;
import com.example.sana.Service.DiagramServices.BasicDiagramRules;
import com.example.sana.Service.DiagramServices.GraphRuleChecks;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

// Rules for the TECH Architecture Diagram, used by manual editing and by AI proposals.
@Component
public class ArchitectureGraphRules extends BasicDiagramRules {

    public ArchitectureGraphRules() {
        super("TECH",
                TechnicalOutputType.ARCHITECTURE.name(),
                List.of("APPLICATION", "CLIENT", "COMPONENT", "DATABASE", "EXTERNAL_SYSTEM", "QUEUE", "SERVICE"),
                List.of("CONNECTS_TO", "DATA_FLOW", "DEPENDENCY"),
                "Node properties.label is required. DATA_FLOW needs a DATABASE or QUEUE endpoint. Edges require distinct endpoints. New IDs must be null.");
    }

    // Architecture nodes may also name their technology
    @Override
    public void validateNode(String type, Map<String, Object> properties, Double positionX, Double positionY) {

        super.validateNode(type, properties, positionX, positionY);

        GraphRuleChecks.optionalText(properties, "technology", 255);
    }

    // A data flow must read from or write to a store
    @Override
    public void validateEdge(String type, String label, String sourceType, String targetType, Map<String, Object> properties) {

        super.validateEdge(type, label, sourceType, targetType, properties);

        List<String> stores = List.of("DATABASE", "QUEUE");
        if ("DATA_FLOW".equals(type) && !stores.contains(sourceType) && !stores.contains(targetType)) {
            throw new ApiException("DATA_FLOW requires a DATABASE or QUEUE endpoint");
        }

        GraphRuleChecks.optionalText(properties, "protocol", 255);
    }
}
