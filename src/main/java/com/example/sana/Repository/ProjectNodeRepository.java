package com.example.sana.Repository;

import com.example.sana.Model.ProjectNode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;

@Repository
public interface ProjectNodeRepository extends JpaRepository<ProjectNode, Integer> {

    ProjectNode findProjectNodeById(Integer id);


    // Lock a canvas node before applying an AI-approved update
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT n FROM ProjectNode n WHERE n.id = :nodeId")
    Optional<ProjectNode> findByIdForUpdate(@Param("nodeId") Integer nodeId);


}
