package com.example.sana.Controller;

import com.example.sana.Service.OwnershipService;
import com.example.sana.Model.User;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import com.example.sana.API.ApiResponse;
import com.example.sana.Model.ProjectEdge;
import com.example.sana.Service.ProjectEdgeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/edge")
@RequiredArgsConstructor
public class ProjectEdgeController {

    private final ProjectEdgeService projectEdgeService;
    private final OwnershipService ownershipService;

    @GetMapping("/get")
    public ResponseEntity<List<ProjectEdge>> getAllEdges() {
        return ResponseEntity.status(200).body(projectEdgeService.getAllEdges());
    }

    @PostMapping("/add/{outputId}/{sourceNodeId}/{targetNodeId}")
    public ResponseEntity<ApiResponse> addEdge(@AuthenticationPrincipal User user, @PathVariable Integer outputId, @PathVariable Integer sourceNodeId,
                                             @PathVariable Integer targetNodeId, @Valid @RequestBody ProjectEdge projectEdge) {
        ownershipService.checkOutput(user, outputId);

        projectEdgeService.addEdge(outputId, sourceNodeId, targetNodeId, projectEdge);
        return ResponseEntity.status(200).body(new ApiResponse("Edge added successfully"));
    }

    @PutMapping("/update/{projectEdgeId}")
    public ResponseEntity<ApiResponse> updateEdge(@AuthenticationPrincipal User user, @PathVariable Integer projectEdgeId, @Valid @RequestBody ProjectEdge projectEdge) {
        ownershipService.checkEdge(user, projectEdgeId);

        projectEdgeService.updateEdge(projectEdgeId, projectEdge);
        return ResponseEntity.status(200).body(new ApiResponse("Edge updated successfully"));
    }

    @DeleteMapping("/delete/{edgeId}")
    public ResponseEntity<ApiResponse> deleteEdge(@AuthenticationPrincipal User user, @PathVariable Integer edgeId) {
        ownershipService.checkEdge(user, edgeId);

        projectEdgeService.deleteEdge(edgeId);
        return ResponseEntity.status(200).body(new ApiResponse("Edge deleted successfully"));
    }
}
