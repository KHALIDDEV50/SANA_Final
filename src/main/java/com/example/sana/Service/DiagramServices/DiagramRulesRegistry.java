package com.example.sana.Service.DiagramServices;

import com.example.sana.API.ApiException;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class DiagramRulesRegistry {

    // Spring injects every DiagramRules bean in the application
    private final List<DiagramRules> diagramRules;

    // Fail at startup when two rules share a diagram type.
    // DiagramGraphService checks that every type is either write-guarded or has an adapter.
    @PostConstruct
    public void checkRules() {

        Set<String> diagramTypes = new HashSet<>();

        for (DiagramRules rules : diagramRules) {
            if (!diagramTypes.add(rules.getDiagramType())) {
                throw new IllegalStateException("Duplicate diagram rules for " + rules.getDiagramType());
            }
        }
    }

    // All supported diagram types, used to describe them to the frontend
    public List<DiagramRules> getAllRules() {
        return diagramRules;
    }

    // Find the rules for a diagram type in a domain, or null when it is not supported
    public DiagramRules findRules(String domain, String diagramType) {

        DiagramRules rules = findRulesByDiagramType(diagramType);

        if (rules == null || !rules.getDomain().equals(domain)) {
            return null;
        }

        return rules;
    }

    // Find the rules for a diagram type in any domain, or null when it is not supported
    public DiagramRules findRulesByDiagramType(String diagramType) {

        if (diagramType == null) {
            return null;
        }

        for (DiagramRules rules : diagramRules) {
            if (rules.getDiagramType().equals(diagramType)) {
                return rules;
            }
        }

        return null;
    }

    // Get the rules for a diagram type in a domain, or fail when it is not supported
    public DiagramRules getRules(String domain, String diagramType) {

        DiagramRules rules = findRules(domain, diagramType);

        if (rules == null) {
            throw new ApiException("Diagram type " + diagramType + " is not supported for " + domain + " projects");
        }

        return rules;
    }
}
