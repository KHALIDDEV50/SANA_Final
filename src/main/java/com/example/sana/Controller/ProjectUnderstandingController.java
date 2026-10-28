package com.example.sana.Controller;

import com.example.sana.Service.OwnershipService;
import com.example.sana.Model.User;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import com.example.sana.API.ApiResponse;
import com.example.sana.Model.ProjectUnderstanding;
import com.example.sana.Service.ProjectUnderstandingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/project-understanding")
@RequiredArgsConstructor
public class ProjectUnderstandingController {

    private final ProjectUnderstandingService projectUnderstandingService;
    private final OwnershipService ownershipService;

    // Get all project understandings
    @GetMapping("/get")
    public ResponseEntity<List<ProjectUnderstanding>> getAllProjectUnderstandings() {
        return ResponseEntity.status(200).body(projectUnderstandingService.getAllProjectUnderstandings());
    }

    // Get project understanding by ID
    @GetMapping("/get/{id}")
    public ResponseEntity<ProjectUnderstanding> getProjectUnderstandingById(@AuthenticationPrincipal User user, @PathVariable Integer id) {
        ownershipService.checkUnderstanding(user, id);

        return ResponseEntity.status(200).body(projectUnderstandingService.getProjectUnderstandingById(id));
    }

    // Get project understanding by project ID
    @GetMapping("/get/project/{projectId}")
    public ResponseEntity<ProjectUnderstanding> getProjectUnderstandingByProjectId(@AuthenticationPrincipal User user, @PathVariable Integer projectId) {
        ownershipService.checkProject(user, projectId);

        return ResponseEntity.status(200).body(projectUnderstandingService.getProjectUnderstandingByProjectId(projectId));
    }

    // Add project understanding
    @PostMapping("/add/{projectId}")
    public ResponseEntity<ApiResponse> addProjectUnderstanding(@AuthenticationPrincipal User user, @PathVariable Integer projectId, @Valid @RequestBody ProjectUnderstanding projectUnderstanding) {
        ownershipService.checkProject(user, projectId);

        projectUnderstandingService.addProjectUnderstanding(projectId, projectUnderstanding);

        return ResponseEntity.status(200).body(new ApiResponse("Project understanding added successfully"));
    }

    // Update project understanding
    @PutMapping("/update/{id}")
    public ResponseEntity<ApiResponse> updateProjectUnderstanding(@AuthenticationPrincipal User user, @PathVariable Integer id, @Valid @RequestBody ProjectUnderstanding projectUnderstanding) {
        ownershipService.checkUnderstanding(user, id);

        projectUnderstandingService.updateProjectUnderstanding(id, projectUnderstanding);

        return ResponseEntity.status(200).body(new ApiResponse("Project understanding updated successfully"));
    }

    // Delete project understanding
    @DeleteMapping("/delete/{id}")
    public ResponseEntity<ApiResponse> deleteProjectUnderstanding(@AuthenticationPrincipal User user, @PathVariable Integer id) {
        ownershipService.checkUnderstanding(user, id);

        projectUnderstandingService.deleteProjectUnderstanding(id);

        return ResponseEntity.status(200).body(new ApiResponse("Project understanding deleted successfully"));
    }
}