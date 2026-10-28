package com.example.sana.Service;

import com.example.sana.API.ApiException;
import com.example.sana.Enums.RepresentationType;
import com.example.sana.Service.DiagramServices.DiagramRules;
import com.example.sana.Service.DiagramServices.DiagramRulesRegistry;
import com.example.sana.Service.DiagramServices.DiagramWriteGuard;
import com.example.sana.Model.Project;
import com.example.sana.Model.ProjectOutput;
import com.example.sana.Repository.ProjectOutputRepository;
import com.example.sana.Repository.ProjectRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ProjectOutputService {

    private final ProjectOutputRepository projectOutputRepository;
    private final ProjectRepository projectRepository;
    private final DiagramRulesRegistry diagramRulesRegistry;

    public List<ProjectOutput> getAllProjectOutputs() {
        return projectOutputRepository.findAll();
    }

    public ProjectOutput getProjectOutputById(Integer outputId) {
        ProjectOutput output = projectOutputRepository.findProjectOutputById(outputId);
        if (output == null) {
            throw new ApiException("Project output not found");
        }
        return output;
    }

    public List<ProjectOutput> getProjectOutputsByProjectId(Integer projectId) {
        if (projectRepository.findProjectById(projectId) == null) {
            throw new ApiException("Project not found");
        }
        return projectOutputRepository.findAllByProject_Id(projectId);
    }

    public void addProjectOutput(Integer projectId, ProjectOutput projectOutput) {
        Project project = projectRepository.findProjectById(projectId);
        if (project == null) {
            throw new ApiException("Project not found");
        }

        // A managed diagram type must match the project's domain and be a graph
        DiagramRules rules = diagramRulesRegistry.findRulesByDiagramType(projectOutput.getOutputType());
        if (rules != null && (!rules.getDomain().equals(project.getDomain())
                || projectOutput.getRepresentationType() != RepresentationType.GRAPH)) {
            throw new ApiException(projectOutput.getOutputType() + " outputs require a " + rules.getDomain() + " project and GRAPH representation");
        }

        projectOutput.setId(null);
        projectOutput.setVersion(null);
        projectOutput.setGraphRevision(0);
        projectOutput.setProject(project);
        projectOutput.setCreatedAt(LocalDateTime.now());
        projectOutput.setUpdatedAt(projectOutput.getCreatedAt());
        projectOutput.setNodes(new HashSet<>());
        projectOutput.setEdges(new HashSet<>());
        projectOutputRepository.save(projectOutput);
    }

    @Transactional
    public void updateProjectOutput(Integer outputId, ProjectOutput projectOutput) {
        ProjectOutput output = getProjectOutputById(outputId);
        DiagramWriteGuard.rejectUnversionedWrite(output);
        DiagramWriteGuard.rejectUnversionedWrite(projectOutput);
        output.setTitle(projectOutput.getTitle());
        output.setOutputType(projectOutput.getOutputType());
        output.setRepresentationType(projectOutput.getRepresentationType());
        output.setUpdatedAt(LocalDateTime.now());
        projectOutputRepository.save(output);
    }

    @Transactional
    public void deleteProjectOutput(Integer outputId) {
        ProjectOutput output = getProjectOutputById(outputId);
        DiagramWriteGuard.rejectUnversionedWrite(output);
        projectOutputRepository.delete(output);
    }
}
