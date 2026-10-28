package com.example.sana.Service.DiagramServices;

import com.example.sana.DTO.GraphDefinitionDTO;
import com.example.sana.Model.Project;
import com.example.sana.Model.ProjectOutput;

import java.util.Map;

// For diagram types whose content lives in domain tables as well as nodes and edges (Creativity).
// The adapter reads that content into the shared graph format and writes an accepted graph back.
// These diagrams keep their own domain endpoints for manual editing, so they are not in DiagramWriteGuard.
public interface DiagramGraphAdapter {

    // Output type this adapter handles, for example STORY_MAP
    String getDiagramType();

    // Read the full diagram, including its domain data, as a graph
    GraphDefinitionDTO read(ProjectOutput output);

    // Save an accepted, validated graph and its domain data. Returns the saved ID of every graph key.
    Map<String, Integer> apply(ProjectOutput output, GraphDefinitionDTO graph);

    // Extra facts the AI agent needs for this diagram type, for example characters it can reference
    default Map<String, Object> extraContext(Project project) {
        return Map.of();
    }
}
