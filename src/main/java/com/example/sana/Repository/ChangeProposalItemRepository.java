package com.example.sana.Repository;

import com.example.sana.Model.ChangeProposalItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Modifying;

import java.util.List;

public interface ChangeProposalItemRepository extends JpaRepository<ChangeProposalItem, Integer> {

    ChangeProposalItem findChangeProposalItemById(Integer id);

    List<ChangeProposalItem> findByChangeProposalId(Integer proposalId);

    // Remove the changes of a project's AI proposals before the project is deleted
    @Modifying
    @Query("DELETE FROM ChangeProposalItem i WHERE i.changeProposal.id IN (SELECT p.id FROM ChangeProposal p WHERE p.project.id = :projectId)")
    void deleteAllByProjectId(@Param("projectId") Integer projectId);
}
