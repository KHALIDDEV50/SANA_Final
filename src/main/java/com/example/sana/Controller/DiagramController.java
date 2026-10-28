package com.example.sana.Controller;

import com.example.sana.Service.OwnershipService;
import com.example.sana.Model.User;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import com.example.sana.API.ApiResponse;
import com.example.sana.DTO.DiagramDTOs.DiagramEdgeRequestDTO;
import com.example.sana.DTO.DiagramDTOs.DiagramGraphResponseDTO;
import com.example.sana.DTO.DiagramDTOs.DiagramNodeRequestDTO;
import com.example.sana.DTO.DiagramDTOs.DiagramRequestDTO;
import com.example.sana.DTO.DiagramDTOs.DiagramTypeDTO;
import com.example.sana.Model.ProjectEdge;
import com.example.sana.Model.ProjectNode;
import com.example.sana.Model.ProjectOutput;
import com.example.sana.Service.DiagramServices.DiagramService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// Manual editing for every supported diagram type: Mind Map, the Business diagrams, and Architecture.
// Every change needs the current graphRevision as expectedRevision.
@RestController
@RequestMapping("/api/v1/diagram")
@RequiredArgsConstructor
public class DiagramController {

    private final DiagramService diagramService;
    private final OwnershipService ownershipService;

    // List the supported diagram types with their node and edge types
    @GetMapping("/types")
    public ResponseEntity<List<DiagramTypeDTO>> getDiagramTypes() {
        return ResponseEntity.status(200).body(diagramService.getDiagramTypes());
    }

    // Get the diagrams of a project, optionally filtered by type
    @GetMapping("/get/project/{projectId}")
    public ResponseEntity<List<ProjectOutput>> getProjectDiagrams(@AuthenticationPrincipal User user, @PathVariable Integer projectId,
                                                                  @RequestParam(required = false) String diagramType) {
        ownershipService.checkProject(user, projectId);

        return ResponseEntity.status(200).body(diagramService.getProjectDiagrams(projectId, diagramType));
    }

    // Get a diagram by ID
    @GetMapping("/get/{outputId}")
    public ResponseEntity<ProjectOutput> getDiagram(@AuthenticationPrincipal User user, @PathVariable Integer outputId) {
        ownershipService.checkOutput(user, outputId);

        return ResponseEntity.status(200).body(diagramService.getDiagram(outputId, null));
    }

    // Get the full graph of a diagram with its current revision
    @GetMapping("/graph/get/{outputId}")
    public ResponseEntity<DiagramGraphResponseDTO> getGraph(@AuthenticationPrincipal User user, @PathVariable Integer outputId) {
        ownershipService.checkOutput(user, outputId);

        return ResponseEntity.status(200).body(diagramService.getGraph(outputId, null));
    }

    // Create an empty diagram of the given diagramType
    @PostMapping("/add/{projectId}")
    public ResponseEntity<ApiResponse> addDiagram(@AuthenticationPrincipal User user, @PathVariable Integer projectId, @Valid @RequestBody DiagramRequestDTO dto) {
        ownershipService.checkProject(user, projectId);

        diagramService.addDiagram(projectId, dto);

        return ResponseEntity.status(200).body(new ApiResponse("Diagram added successfully"));
    }

    // Rename a diagram
    @PutMapping("/update/{outputId}")
    public ResponseEntity<ApiResponse> updateDiagram(@AuthenticationPrincipal User user, @PathVariable Integer outputId, @Valid @RequestBody DiagramRequestDTO dto,
                                                     @RequestParam Integer expectedRevision) {
        ownershipService.checkOutput(user, outputId);

        diagramService.updateDiagram(outputId, dto, expectedRevision, null);

        return ResponseEntity.status(200).body(new ApiResponse("Diagram updated successfully"));
    }

    // Delete a diagram
    @DeleteMapping("/delete/{outputId}")
    public ResponseEntity<ApiResponse> deleteDiagram(@AuthenticationPrincipal User user, @PathVariable Integer outputId, @RequestParam Integer expectedRevision) {
        ownershipService.checkOutput(user, outputId);

        diagramService.deleteDiagram(outputId, expectedRevision, null);

        return ResponseEntity.status(200).body(new ApiResponse("Diagram deleted successfully"));
    }

    // Get a node
    @GetMapping("/node/get/{outputId}/{nodeId}")
    public ResponseEntity<ProjectNode> getNode(@AuthenticationPrincipal User user, @PathVariable Integer outputId, @PathVariable Integer nodeId) {
        ownershipService.checkOutput(user, outputId);

        return ResponseEntity.status(200).body(diagramService.getNode(outputId, nodeId, null));
    }

    // Add a node
    @PostMapping("/node/add/{outputId}")
    public ResponseEntity<ApiResponse> addNode(@AuthenticationPrincipal User user, @PathVariable Integer outputId, @Valid @RequestBody DiagramNodeRequestDTO dto,
                                               @RequestParam Integer expectedRevision) {
        ownershipService.checkOutput(user, outputId);

        diagramService.addNode(outputId, dto, expectedRevision, null);

        return ResponseEntity.status(200).body(new ApiResponse("Node added successfully"));
    }

    // Update a node
    @PutMapping("/node/update/{outputId}/{nodeId}")
    public ResponseEntity<ApiResponse> updateNode(@AuthenticationPrincipal User user, @PathVariable Integer outputId, @PathVariable Integer nodeId,
                                                  @Valid @RequestBody DiagramNodeRequestDTO dto, @RequestParam Integer expectedRevision) {
        ownershipService.checkOutput(user, outputId);

        diagramService.updateNode(outputId, nodeId, dto, expectedRevision, null);

        return ResponseEntity.status(200).body(new ApiResponse("Node updated successfully"));
    }

    // Delete a node and its connected edges
    @DeleteMapping("/node/delete/{outputId}/{nodeId}")
    public ResponseEntity<ApiResponse> deleteNode(@AuthenticationPrincipal User user, @PathVariable Integer outputId, @PathVariable Integer nodeId,
                                                  @RequestParam Integer expectedRevision) {
        ownershipService.checkOutput(user, outputId);

        diagramService.deleteNode(outputId, nodeId, expectedRevision, null);

        return ResponseEntity.status(200).body(new ApiResponse("Node deleted successfully"));
    }

    // Get an edge
    @GetMapping("/edge/get/{outputId}/{edgeId}")
    public ResponseEntity<ProjectEdge> getEdge(@AuthenticationPrincipal User user, @PathVariable Integer outputId, @PathVariable Integer edgeId) {
        ownershipService.checkOutput(user, outputId);

        return ResponseEntity.status(200).body(diagramService.getEdge(outputId, edgeId, null));
    }

    // Add an edge
    @PostMapping("/edge/add/{outputId}")
    public ResponseEntity<ApiResponse> addEdge(@AuthenticationPrincipal User user, @PathVariable Integer outputId, @Valid @RequestBody DiagramEdgeRequestDTO dto,
                                               @RequestParam Integer expectedRevision) {
        ownershipService.checkOutput(user, outputId);

        diagramService.addEdge(outputId, dto, expectedRevision, null);

        return ResponseEntity.status(200).body(new ApiResponse("Edge added successfully"));
    }

    // Update an edge
    @PutMapping("/edge/update/{outputId}/{edgeId}")
    public ResponseEntity<ApiResponse> updateEdge(@AuthenticationPrincipal User user, @PathVariable Integer outputId, @PathVariable Integer edgeId,
                                                  @Valid @RequestBody DiagramEdgeRequestDTO dto, @RequestParam Integer expectedRevision) {
        ownershipService.checkOutput(user, outputId);

        diagramService.updateEdge(outputId, edgeId, dto, expectedRevision, null);

        return ResponseEntity.status(200).body(new ApiResponse("Edge updated successfully"));
    }

    // Delete an edge
    @DeleteMapping("/edge/delete/{outputId}/{edgeId}")
    public ResponseEntity<ApiResponse> deleteEdge(@AuthenticationPrincipal User user, @PathVariable Integer outputId, @PathVariable Integer edgeId,
                                                  @RequestParam Integer expectedRevision) {
        ownershipService.checkOutput(user, outputId);

        diagramService.deleteEdge(outputId, edgeId, expectedRevision, null);

        return ResponseEntity.status(200).body(new ApiResponse("Edge deleted successfully"));
    }
}
