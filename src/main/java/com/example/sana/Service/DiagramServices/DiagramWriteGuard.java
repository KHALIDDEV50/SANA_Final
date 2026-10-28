package com.example.sana.Service.DiagramServices;

import com.example.sana.API.ApiException;
import com.example.sana.Model.ProjectOutput;

import java.util.List;

// Managed diagrams are edited only through the revision-checked diagram endpoints and AI proposals.
// Legacy and domain writers call this guard so they cannot bypass the revision check.
public final class DiagramWriteGuard {

    // Every DiagramRules diagram type without a DiagramGraphAdapter must be listed here;
    // DiagramGraphService checks this at startup. Creativity types are not listed because
    // their own endpoints keep editing them.
    public static final List<String> MANAGED_DIAGRAM_TYPES = List.of(
            "ARCHITECTURE",
            "ERD",
            "CLASS_DIAGRAM",
            "USE_CASE",
            "MIND_MAP",
            "BUSINESS_MODEL_CANVAS",
            "TARGET_AUDIENCE",
            "SWOT",
            "COMPETITOR_ANALYSIS",
            "RISK_ANALYSIS"
    );

    private DiagramWriteGuard() {
    }

    // Reject a generic write to a managed diagram
    public static void rejectUnversionedWrite(ProjectOutput output) {

        if (output != null && MANAGED_DIAGRAM_TYPES.contains(output.getOutputType())) {
            throw new ApiException("Use the diagram endpoints with expectedRevision to modify this diagram");
        }
    }
}
