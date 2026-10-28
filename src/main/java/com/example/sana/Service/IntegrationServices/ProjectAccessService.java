package com.example.sana.Service.IntegrationServices;

import com.example.sana.Model.Project;
import com.example.sana.Repository.ProjectRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

// Project lookup and transactional locking only. User authorization is deferred.
@Service
@RequiredArgsConstructor
public class ProjectAccessService {
    private final ProjectRepository projectRepository;

    public Project project(Integer projectId, boolean lock) {
        return (lock ? projectRepository.findByIdForUpdate(projectId) : projectRepository.findById(projectId))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Project not found"));
    }
}
