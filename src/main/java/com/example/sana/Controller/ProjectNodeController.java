package com.example.sana.Controller;

import com.example.sana.Service.OwnershipService;
import com.example.sana.Model.User;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import com.example.sana.API.ApiResponse;
import com.example.sana.Model.ProjectNode;
import com.example.sana.Service.ProjectNodeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/node")
@RequiredArgsConstructor
public class ProjectNodeController {

    private final ProjectNodeService projectNodeService;
    private final OwnershipService ownershipService;

    @GetMapping("/get")
    public ResponseEntity<List<ProjectNode>> getAllNodes() {
        return ResponseEntity.status(200).body(projectNodeService.getAllNodes());
    }

    @PostMapping("/add/{outputId}")
    public ResponseEntity<ApiResponse> addNode(@AuthenticationPrincipal User user, @PathVariable Integer outputId, @Valid @RequestBody ProjectNode projectNode) {
        ownershipService.checkOutput(user, outputId);

        projectNodeService.addNode(outputId, projectNode);
        return ResponseEntity.status(200).body(new ApiResponse("Node added successfully"));
    }

    @PutMapping("/update/{projectNodeId}")
    public ResponseEntity<ApiResponse> updateNode(@AuthenticationPrincipal User user, @PathVariable Integer projectNodeId, @Valid @RequestBody ProjectNode projectNode) {
        ownershipService.checkNode(user, projectNodeId);

        projectNodeService.updateNode(projectNodeId, projectNode);
        return ResponseEntity.status(200).body(new ApiResponse("Node updated successfully"));
    }

    @DeleteMapping("/delete/{nodeId}")
    public ResponseEntity<ApiResponse> deleteNode(@AuthenticationPrincipal User user, @PathVariable Integer nodeId) {
        ownershipService.checkNode(user, nodeId);

        projectNodeService.deleteNode(nodeId);
        return ResponseEntity.status(200).body(new ApiResponse("Node deleted successfully"));
    }
}

