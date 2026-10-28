package com.example.sana.Repository.CreativityDomainRepository;


import com.example.sana.Model.CreativityDomainModels.WorldRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import java.util.Optional;

import java.util.List;

@Repository
public interface WorldRuleRepository extends JpaRepository<WorldRule, Integer> {

    WorldRule findWorldRuleById(Integer id);



    // Search world rules by name, description or scope
    @Query("""
            SELECT r FROM WorldRule r
            JOIN r.projectNode node
            WHERE node.projectOutput.id = :outputId
              AND (
                  LOWER(r.name) LIKE LOWER(CONCAT('%', :keyword, '%'))
                  OR LOWER(r.description) LIKE LOWER(CONCAT('%', :keyword, '%'))
                  OR LOWER(r.scope) LIKE LOWER(CONCAT('%', :keyword, '%'))
              )
            """)
    List<WorldRule> searchInOutput(@Param("outputId") Integer outputId, @Param("keyword") String keyword);


    // Lock the WorldRule while applying an approved AI update.
    // The calling service must have an active transaction.
    //
    // This prevents simultaneous AI approvals from updating
    // the same WorldRule without coordination.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM WorldRule r WHERE r.id = :ruleId")
    Optional<WorldRule> findByIdForUpdate(@Param("ruleId") Integer ruleId);



}

