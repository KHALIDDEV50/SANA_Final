package com.example.sana.Repository;

import com.example.sana.Model.Project;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;

import java.util.List;

@Repository
public interface ProjectRepository extends JpaRepository<Project, Integer> {

    long countByOwner_Id(Integer ownerId);

    Project findProjectById(Integer id);

    List<Project> findAllByOwner_Id(Integer ownerId);


    // Lock the project while applying an approved proposal
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Project p WHERE p.id = :projectId")
    Optional<Project> findByIdForUpdate(@Param("projectId") Integer projectId);



}