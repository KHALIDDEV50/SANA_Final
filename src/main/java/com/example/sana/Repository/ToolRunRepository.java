package com.example.sana.Repository;

import com.example.sana.Model.ToolRun;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Modifying;

public interface ToolRunRepository extends JpaRepository<ToolRun, Integer> {

    ToolRun findToolRunById(Integer id);

    // Remove a project's tool runs before the project is deleted
    @Modifying
    @Query("DELETE FROM ToolRun t WHERE t.project.id = :projectId")
    void deleteAllByProjectId(@Param("projectId") Integer projectId);
}
