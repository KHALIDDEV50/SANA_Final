package com.example.sana.Controller;

import com.example.sana.Service.OwnershipService;
import com.example.sana.Model.User;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import com.example.sana.API.ApiResponse;
import com.example.sana.Model.ProjectRecord;
import com.example.sana.Service.ProjectRecordService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/record")
@RequiredArgsConstructor
public class ProjectRecordController {

    private final ProjectRecordService projectRecordService;
    private final OwnershipService ownershipService;

    @GetMapping("/get")
    public ResponseEntity<List<ProjectRecord>> getAllProjectRecords() {
        return ResponseEntity.status(200).body(projectRecordService.getAllProjectRecord());
    }

    @PostMapping("/add/{projectId}")
    public ResponseEntity<ApiResponse> addProjectRecord(@AuthenticationPrincipal User user, @PathVariable Integer projectId, @Valid @RequestBody ProjectRecord projectRecord) {
        ownershipService.checkProject(user, projectId);

        projectRecordService.addProjectRecord(projectId, projectRecord);
        return ResponseEntity.status(200).body(new ApiResponse("Project record added successfully"));
    }

    @PutMapping("/update/{projectRecordId}")
    public ResponseEntity<ApiResponse> updateProjectRecord(@AuthenticationPrincipal User user, @PathVariable Integer projectRecordId, @Valid @RequestBody ProjectRecord projectRecord) {
        ownershipService.checkRecord(user, projectRecordId);

        projectRecordService.updateProjectRecord(projectRecordId, projectRecord);
        return ResponseEntity.status(200).body(new ApiResponse("Project record updated successfully"));
    }

    @DeleteMapping("/delete/{projectRecordId}")
    public ResponseEntity<ApiResponse> deleteProjectRecord(@AuthenticationPrincipal User user, @PathVariable Integer projectRecordId) {
        ownershipService.checkRecord(user, projectRecordId);

        projectRecordService.deleteProjectRecord(projectRecordId);
        return ResponseEntity.status(200).body(new ApiResponse("Project record deleted successfully"));
    }
}
