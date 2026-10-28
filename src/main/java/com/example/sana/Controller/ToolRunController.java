package com.example.sana.Controller;

import com.example.sana.API.ApiResponse;
import com.example.sana.Model.ToolRun;
import com.example.sana.Service.ToolRunService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/tool-run")
@RequiredArgsConstructor
public class ToolRunController {

    private final ToolRunService toolRunService;

    @GetMapping("/get")
    public ResponseEntity<?> getAllToolRuns() {
        return ResponseEntity.status(200).body(toolRunService.getAllToolRuns());
    }

    @GetMapping("/get/{toolRunId}")
    public ResponseEntity<?> getToolRunById(@PathVariable Integer toolRunId) {
        return ResponseEntity.status(200).body(toolRunService.getToolRunById(toolRunId));
    }

    @PostMapping("/add/{userId}/{projectId}")
    public ResponseEntity<?> addToolRun(@PathVariable Integer userId, @PathVariable Integer projectId,
                                       @Valid @RequestBody ToolRun toolRun) {
        toolRunService.addToolRun(userId, projectId, toolRun);
        return ResponseEntity.status(200).body(new ApiResponse("Tool run added successfully"));
    }

    @PutMapping("/update/{toolRunId}")
    public ResponseEntity<?> updateToolRun(@PathVariable Integer toolRunId, @Valid @RequestBody ToolRun toolRun) {
        toolRunService.updateToolRun(toolRunId, toolRun);
        return ResponseEntity.status(200).body(new ApiResponse("Tool run updated successfully"));
    }

    @DeleteMapping("/delete/{toolRunId}")
    public ResponseEntity<?> deleteToolRun(@PathVariable Integer toolRunId) {
        toolRunService.deleteToolRun(toolRunId);
        return ResponseEntity.status(200).body(new ApiResponse("Tool run deleted successfully"));
    }
}
