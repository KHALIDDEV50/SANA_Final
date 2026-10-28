package com.example.sana.Repository;

import com.example.sana.Model.ChangeProposal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;

import java.util.List;

public interface ChangeProposalRepository extends JpaRepository<ChangeProposal, Integer> {

    ChangeProposal findChangeProposalById(Integer id);

    List<ChangeProposal> findByAiInteractionId(Integer aiInteractionId);


    // Lock a proposal while making an approval/rejection decision
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM ChangeProposal p WHERE p.id = :proposalId")
    Optional<ChangeProposal> findByIdForUpdate(@Param("proposalId") Integer proposalId);

    // Remove a project's AI proposals before the project is deleted
    @Modifying
    @Query("DELETE FROM ChangeProposal p WHERE p.project.id = :projectId")
    void deleteAllByProjectId(@Param("projectId") Integer projectId);
}
