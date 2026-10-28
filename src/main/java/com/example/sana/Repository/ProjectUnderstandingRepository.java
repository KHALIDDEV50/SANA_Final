package com.example.sana.Repository;

import com.example.sana.Model.ProjectUnderstanding;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProjectUnderstandingRepository
        extends JpaRepository<ProjectUnderstanding, Integer> {

    ProjectUnderstanding findProjectUnderstandingById(Integer id);

    ProjectUnderstanding findProjectUnderstandingByProject_Id(Integer projectId);
}