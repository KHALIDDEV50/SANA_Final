package com.example.sana.Repository;

import com.example.sana.Model.ProjectRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;

@Repository
public interface ProjectRecordRepository extends JpaRepository<ProjectRecord, Integer> {
    ProjectRecord findProjectRecordById(Integer id);


    // Lock a project record before applying an AI-approved update
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM ProjectRecord r WHERE r.id = :recordId")
    Optional<ProjectRecord> findByIdForUpdate(@Param("recordId") Integer recordId);

}
