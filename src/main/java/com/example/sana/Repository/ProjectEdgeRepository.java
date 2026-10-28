package com.example.sana.Repository;

import com.example.sana.Model.ProjectEdge;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;

@Repository
public interface ProjectEdgeRepository extends JpaRepository<ProjectEdge, Integer> {
    ProjectEdge findProjectEdgeById(Integer id);


    // Lock an edge before applying an AI-approved update
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT e FROM ProjectEdge e WHERE e.id = :edgeId")
    Optional<ProjectEdge> findByIdForUpdate(@Param("edgeId") Integer edgeId);

}
