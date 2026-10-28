package com.example.sana.Controller;

import com.example.sana.Service.OwnershipService;
import com.example.sana.Model.User;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import com.example.sana.API.ApiResponse;
import com.example.sana.Model.Project;
import com.example.sana.Service.ProjectService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/project")
@RequiredArgsConstructor
public class ProjectController {

    private final ProjectService projectService;
    private final OwnershipService ownershipService;

    // Get all projects
    @GetMapping("/get")
    public ResponseEntity<List<Project>> getAllProjects() {
        return ResponseEntity.status(200).body(projectService.getAllProjects());
    }

    // Get the logged-in user's projects
    @GetMapping("/get/my")
    public ResponseEntity<List<Project>> getMyProjects(@AuthenticationPrincipal User user) {
        return ResponseEntity.status(200).body(projectService.getProjectsByUserId(user.getId()));
    }

    // Get a project by ID
    @GetMapping("/get/{projectId}")
    public ResponseEntity<Project> getProjectById(@AuthenticationPrincipal User user, @PathVariable Integer projectId) {
        ownershipService.checkProject(user, projectId);

        return ResponseEntity.status(200).body(projectService.getProjectById(projectId));
    }

    // Get all projects for a specific user
    @GetMapping("/get/user/{userId}")
    public ResponseEntity<List<Project>> getProjectsByUserId(@AuthenticationPrincipal User user, @PathVariable Integer userId) {
        ownershipService.checkUser(user, userId);

        return ResponseEntity.status(200).body(projectService.getProjectsByUserId(userId));
    }

    // Add a new project
    @PostMapping("/add/{userId}")
    public ResponseEntity<ApiResponse> addProject(@AuthenticationPrincipal User user, @PathVariable Integer userId, @Valid @RequestBody Project project) {
        ownershipService.checkUser(user, userId);

        projectService.addProject(userId, project);

        return ResponseEntity.status(200).body(new ApiResponse("Project added successfully"));
    }

    // Update an existing project
    @PutMapping("/update/{projectId}")
    public ResponseEntity<ApiResponse> updateProject(@AuthenticationPrincipal User user, @PathVariable Integer projectId, @Valid @RequestBody Project project) {
        ownershipService.checkProject(user, projectId);

        projectService.updateProject(projectId, project);

        return ResponseEntity.status(200).body(new ApiResponse("Project updated successfully"));
    }

    // Delete an existing project
    @DeleteMapping("/delete/{projectId}")
    public ResponseEntity<ApiResponse> deleteProject(@AuthenticationPrincipal User user, @PathVariable Integer projectId) {
        ownershipService.checkProject(user, projectId);

        projectService.deleteProject(projectId);

        return ResponseEntity.status(200).body(new ApiResponse("Project deleted successfully"));
    }
}