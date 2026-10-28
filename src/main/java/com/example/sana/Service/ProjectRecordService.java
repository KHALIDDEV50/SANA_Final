package com.example.sana.Service;

import com.example.sana.API.ApiException;
import com.example.sana.Model.Project;
import com.example.sana.Model.ProjectRecord;
import com.example.sana.Repository.ProjectRecordRepository;
import com.example.sana.Repository.ProjectRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ProjectRecordService {

    private final ProjectRecordRepository projectRecordRepository;
    private final ProjectRepository projectRepository;

    public List<ProjectRecord> getAllProjectRecord(){
        return projectRecordRepository.findAll();
    }

    public void addProjectRecord(Integer projectId, ProjectRecord projectRecord){
        Project project = projectRepository.findProjectById(projectId);
        if (project == null){
            throw new ApiException("project not found");
        }
        projectRecord.setId(null);
        projectRecord.setProject(project);
        projectRecord.setCreatedAt(LocalDate.now());
        projectRecordRepository.save(projectRecord);
    }

    public void updateProjectRecord(Integer projectRecordId, ProjectRecord projectRecord){
        ProjectRecord record = projectRecordRepository.findProjectRecordById(projectRecordId);
        if (record == null){
            throw new ApiException("project record not found");
        }
        record.setUpdatedAt(LocalDate.now());
        record.setTitle(projectRecord.getTitle());
        record.setContent(projectRecord.getContent());
        record.setStatus(projectRecord.getStatus());
        record.setSource(projectRecord.getSource());
        projectRecordRepository.save(record);
    }

    public void deleteProjectRecord(Integer projectRecordId){
        ProjectRecord record = projectRecordRepository.findProjectRecordById(projectRecordId);
        if (record == null){
            throw new ApiException("project record not found");
        }

        projectRecordRepository.delete(record);
    }
}
