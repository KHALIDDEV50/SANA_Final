package com.example.sana.Controller.TechDomainControllers;

import com.example.sana.Service.OwnershipService;
import com.example.sana.Model.User;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import com.example.sana.API.ApiResponse;
import com.example.sana.DTO.DiagramDTOs.DiagramEdgeRequestDTO;
import com.example.sana.DTO.DiagramDTOs.DiagramGraphResponseDTO;
import com.example.sana.DTO.DiagramDTOs.DiagramNodeRequestDTO;
import com.example.sana.DTO.DiagramDTOs.DiagramRequestDTO;
import com.example.sana.Enums.TechDomainEnums.TechnicalOutputType;
import com.example.sana.Model.ProjectEdge;
import com.example.sana.Model.ProjectNode;
import com.example.sana.Model.ProjectOutput;
import com.example.sana.Service.DiagramServices.DiagramService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// Architecture routes kept for existing clients. They use the shared DiagramService limited to ARCHITECTURE.
@RestController
@RequestMapping("/api/v1/tech/architecture")
@RequiredArgsConstructor
public class ArchitectureDiagramController {

    private static final String ARCHITECTURE = TechnicalOutputType.ARCHITECTURE.name();

    private final DiagramService diagramService;
    private final OwnershipService ownershipService;

    @GetMapping("/graph/get/{outputId}")
    public ResponseEntity<DiagramGraphResponseDTO> getArchitectureGraph(@AuthenticationPrincipal User user, @PathVariable Integer outputId) {
        ownershipService.checkOutput(user, outputId);

        return ResponseEntity.status(200).body(diagramService.getGraph(outputId, ARCHITECTURE));
    }

    @GetMapping("/get")
    public ResponseEntity<List<ProjectOutput>> getAllArchitectureDiagrams() {
        return ResponseEntity.status(200).body(diagramService.getDiagramsByType(ARCHITECTURE));
    }

    @GetMapping("/get/{outputId}")
    public ResponseEntity<ProjectOutput> getArchitectureDiagramById(@AuthenticationPrincipal User user, @PathVariable Integer outputId) {
        ownershipService.checkOutput(user, outputId);

        return ResponseEntity.status(200).body(diagramService.getDiagram(outputId, ARCHITECTURE));
    }

    @GetMapping("/get/project/{projectId}")
    public ResponseEntity<List<ProjectOutput>> getArchitectureDiagramsByProjectId(@AuthenticationPrincipal User user, @PathVariable Integer projectId) {
        ownershipService.checkProject(user, projectId);

        return ResponseEntity.status(200).body(diagramService.getProjectDiagrams(projectId, ARCHITECTURE));
    }

    @PostMapping("/add/{projectId}")
    public ResponseEntity<ApiResponse> addArchitectureDiagram(@AuthenticationPrincipal User user, @PathVariable Integer projectId,
                                                              @Valid @RequestBody DiagramRequestDTO dto) {
        ownershipService.checkProject(user, projectId);

        dto.setDiagramType(ARCHITECTURE);
        diagramService.addDiagram(projectId, dto);
        return ResponseEntity.status(200).body(new ApiResponse("Architecture diagram added successfully"));
    }

    @PutMapping("/update/{outputId}")
    public ResponseEntity<ApiResponse> updateArchitectureDiagram(@AuthenticationPrincipal User user, @PathVariable Integer outputId,
                                                                 @Valid @RequestBody DiagramRequestDTO dto, @RequestParam Integer expectedRevision) {
        ownershipService.checkOutput(user, outputId);

        diagramService.updateDiagram(outputId, dto, expectedRevision, ARCHITECTURE);
        return ResponseEntity.status(200).body(new ApiResponse("Architecture diagram updated successfully"));
    }

    @DeleteMapping("/delete/{outputId}")
    public ResponseEntity<ApiResponse> deleteArchitectureDiagram(@AuthenticationPrincipal User user, @PathVariable Integer outputId, @RequestParam Integer expectedRevision) {
        ownershipService.checkOutput(user, outputId);

        diagramService.deleteDiagram(outputId, expectedRevision, ARCHITECTURE);
        return ResponseEntity.status(200).body(new ApiResponse("Architecture diagram deleted successfully"));
    }

    @GetMapping("/node/get/{outputId}/{nodeId}")
    public ResponseEntity<ProjectNode> getArchitectureNodeById(@AuthenticationPrincipal User user, @PathVariable Integer outputId, @PathVariable Integer nodeId) {
        ownershipService.checkOutput(user, outputId);

        return ResponseEntity.status(200).body(diagramService.getNode(outputId, nodeId, ARCHITECTURE));
    }

    @PostMapping("/node/add/{outputId}")
    public ResponseEntity<ApiResponse> addArchitectureNode(@AuthenticationPrincipal User user, @PathVariable Integer outputId,
                                                           @Valid @RequestBody DiagramNodeRequestDTO dto, @RequestParam Integer expectedRevision) {
        ownershipService.checkOutput(user, outputId);

        diagramService.addNode(outputId, dto, expectedRevision, ARCHITECTURE);
        return ResponseEntity.status(200).body(new ApiResponse("Architecture node added successfully"));
    }

    @PutMapping("/node/update/{outputId}/{nodeId}")
    public ResponseEntity<ApiResponse> updateArchitectureNode(@AuthenticationPrincipal User user, @PathVariable Integer outputId, @PathVariable Integer nodeId,
                                                              @Valid @RequestBody DiagramNodeRequestDTO dto, @RequestParam Integer expectedRevision) {
        ownershipService.checkOutput(user, outputId);

        diagramService.updateNode(outputId, nodeId, dto, expectedRevision, ARCHITECTURE);
        return ResponseEntity.status(200).body(new ApiResponse("Architecture node updated successfully"));
    }

    @DeleteMapping("/node/delete/{outputId}/{nodeId}")
    public ResponseEntity<ApiResponse> deleteArchitectureNode(@AuthenticationPrincipal User user, @PathVariable Integer outputId, @PathVariable Integer nodeId, @RequestParam Integer expectedRevision) {
        ownershipService.checkOutput(user, outputId);

        diagramService.deleteNode(outputId, nodeId, expectedRevision, ARCHITECTURE);
        return ResponseEntity.status(200).body(new ApiResponse("Architecture node deleted successfully"));
    }

    @GetMapping("/edge/get/{outputId}/{edgeId}")
    public ResponseEntity<ProjectEdge> getArchitectureEdgeById(@AuthenticationPrincipal User user, @PathVariable Integer outputId, @PathVariable Integer edgeId) {
        ownershipService.checkOutput(user, outputId);

        return ResponseEntity.status(200).body(diagramService.getEdge(outputId, edgeId, ARCHITECTURE));
    }

    @PostMapping("/edge/add/{outputId}")
    public ResponseEntity<ApiResponse> addArchitectureEdge(@AuthenticationPrincipal User user, @PathVariable Integer outputId,
                                                           @Valid @RequestBody DiagramEdgeRequestDTO dto, @RequestParam Integer expectedRevision) {
        ownershipService.checkOutput(user, outputId);

        diagramService.addEdge(outputId, dto, expectedRevision, ARCHITECTURE);
        return ResponseEntity.status(200).body(new ApiResponse("Architecture edge added successfully"));
    }

    @PutMapping("/edge/update/{outputId}/{edgeId}")
    public ResponseEntity<ApiResponse> updateArchitectureEdge(@AuthenticationPrincipal User user, @PathVariable Integer outputId, @PathVariable Integer edgeId,
                                                              @Valid @RequestBody DiagramEdgeRequestDTO dto, @RequestParam Integer expectedRevision) {
        ownershipService.checkOutput(user, outputId);

        diagramService.updateEdge(outputId, edgeId, dto, expectedRevision, ARCHITECTURE);
        return ResponseEntity.status(200).body(new ApiResponse("Architecture edge updated successfully"));
    }

    @DeleteMapping("/edge/delete/{outputId}/{edgeId}")
    public ResponseEntity<ApiResponse> deleteArchitectureEdge(@AuthenticationPrincipal User user, @PathVariable Integer outputId, @PathVariable Integer edgeId, @RequestParam Integer expectedRevision) {
        ownershipService.checkOutput(user, outputId);

        diagramService.deleteEdge(outputId, edgeId, expectedRevision, ARCHITECTURE);
        return ResponseEntity.status(200).body(new ApiResponse("Architecture edge deleted successfully"));
    }
}
