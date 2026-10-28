package com.example.sana.Service;

import com.example.sana.API.ApiException;
import com.example.sana.Model.Project;
import com.example.sana.Model.ToolRun;
import com.example.sana.Model.User;
import com.example.sana.Repository.ProjectRepository;
import com.example.sana.Repository.ToolRunRepository;
import com.example.sana.Repository.UserRepository;
import com.example.sana.Service.Business.BusinessService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

// START: Imports added for Business tool execution.
import com.example.sana.DTO.Business.BusinessToolRequestDTO;
import com.example.sana.Enums.ExecutionStatus;
import org.springframework.util.StringUtils;
// END: Imports added for Business tool execution.

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ToolRunService {

    private final ToolRunRepository toolRunRepository;
    private final UserRepository userRepository;
    private final ProjectRepository projectRepository;
    private final BusinessService businessService;

    public List<ToolRun> getAllToolRuns() {
        return toolRunRepository.findAll();
    }

    public ToolRun getToolRunById(Integer tool_run_id) {
        return toolRunRepository.findById(tool_run_id)
                .orElseThrow(() -> new ApiException("Tool run not found"));
    }

    public void addToolRun(Integer user_id, Integer project_id, ToolRun toolRun) {
        User user = userRepository.findById(user_id)
                .orElseThrow(() -> new ApiException("User not found"));

        Project project = projectRepository.findById(project_id)
                .orElseThrow(() -> new ApiException("Project not found"));

        toolRun.setId(null);
        toolRun.setCreatedAt(null);
        toolRun.setUser(user);
        toolRun.setProject(project);

        toolRunRepository.save(toolRun);
    }

    public void updateToolRun(Integer tool_run_id, ToolRun toolRun) {
        ToolRun oldToolRun = toolRunRepository.findById(tool_run_id)
                .orElseThrow(() -> new ApiException("Tool run not found"));

        oldToolRun.setToolType(toolRun.getToolType());
        oldToolRun.setInputJson(toolRun.getInputJson());
        oldToolRun.setOutputJson(toolRun.getOutputJson());
        oldToolRun.setStatus(toolRun.getStatus());

        toolRunRepository.save(oldToolRun);
    }

    public void deleteToolRun(Integer tool_run_id) {
        ToolRun toolRun = toolRunRepository.findById(tool_run_id)
                .orElseThrow(() -> new ApiException("Tool run not found"));

        toolRunRepository.delete(toolRun);
    }

   /* // START: Business tool execution and automatic history tracking.

    // Keep database transactions short; do not wrap the AI call in a transaction.
    public String executeBusinessTool(
            Integer userId,
            BusinessToolRequestDTO request) {

        // Check that the user and project exist.
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException("User not found"));

        Project project = projectRepository.findById(request.projectId())
                .orElseThrow(() -> new ApiException("Project not found"));

        // This execution flow currently supports the project owner only.
        if (!project.getOwner().getId().equals(userId)) {
            throw new ApiException("User must be the project owner");
        }

        // Create one history record for the entire execution.
        ToolRun toolRun = new ToolRun();

        toolRun.setUser(user);
        toolRun.setProject(project);
        toolRun.setToolType(request.toolType().name());
        toolRun.setInputJson(
                StringUtils.hasText(request.inputJson())
                        ? request.inputJson()
                        : "{}"
        );
        toolRun.setStatus(ExecutionStatus.RUNNING);

        toolRun = toolRunRepository.saveAndFlush(toolRun);

        String result;

        try {
            // Internal AI correction attempts belong to this same record.
            result = businessService.executeTool(request);

        } catch (RuntimeException exception) {

            // Mark the execution as failed without storing a successful result.
            toolRun.setStatus(ExecutionStatus.FAILED);
            toolRun.setOutputJson(null);

            try {
                toolRunRepository.saveAndFlush(toolRun);

            } catch (RuntimeException persistenceException) {

                // Preserve the original error if saving the status also fails.
                exception.addSuppressed(persistenceException);

                log.error("Failed to save FAILED status. Tool run ID: {}", toolRun.getId(), persistenceException);
            }

            throw exception;
        }

        // Save the validated result before returning success.
        toolRun.setOutputJson(result);
        toolRun.setStatus(ExecutionStatus.COMPLETED);

        toolRunRepository.saveAndFlush(toolRun);

        log.info(
                "Business tool completed. Tool run ID: {}, project ID: {}, tool: {}",
                toolRun.getId(),
                request.projectId(),
                request.toolType()
        );

        return result;
    }

    // END: Business tool execution and automatic history tracking.*/
}