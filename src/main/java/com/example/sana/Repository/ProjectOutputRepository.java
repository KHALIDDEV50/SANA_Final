package com.example.sana.Repository;

import com.example.sana.Model.ProjectOutput;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

@Repository
public interface ProjectOutputRepository extends JpaRepository<ProjectOutput, Integer> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT o FROM ProjectOutput o WHERE o.id = :outputId")
    Optional<ProjectOutput> findByIdForUpdate(@Param("outputId") Integer outputId);

    ProjectOutput findProjectOutputById(Integer id);

    List<ProjectOutput> findAllByProject_Id(Integer projectId);
}
