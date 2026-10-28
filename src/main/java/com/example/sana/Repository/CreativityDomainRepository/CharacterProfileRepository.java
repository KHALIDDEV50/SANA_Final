package com.example.sana.Repository.CreativityDomainRepository;


import com.example.sana.Model.CreativityDomainModels.CharacterProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import java.util.Optional;

import java.util.List;

@Repository
public interface CharacterProfileRepository extends JpaRepository<CharacterProfile, Integer> {

    CharacterProfile findCharacterProfileById(Integer id);


    // Search characters by name, description, aliases or role
    // Only return characters inside the selected ProjectOutput
    @Query("""
            SELECT c FROM CharacterProfile c
            JOIN c.projectNode node
            WHERE node.projectOutput.id = :outputId
              AND (
                  LOWER(c.name) LIKE LOWER(CONCAT('%', :keyword, '%'))
                  OR LOWER(c.shortDescription) LIKE LOWER(CONCAT('%', :keyword, '%'))
                  OR LOWER(c.aliases) LIKE LOWER(CONCAT('%', :keyword, '%'))
                  OR LOWER(c.role) LIKE LOWER(CONCAT('%', :keyword, '%'))
              )
            """)
    List<CharacterProfile> searchInOutput(
            @Param("outputId") Integer outputId,
            @Param("keyword") String keyword);


    // Lock the character while an approved AI proposal is being applied.
    // This helps prevent concurrent AI approvals from modifying
    // the same CharacterProfile at the same time.
    //
    // The lock must be acquired inside an active transaction.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM CharacterProfile c WHERE c.id = :characterId")
    Optional<CharacterProfile> findByIdForUpdate(@Param("characterId") Integer characterId);

    // Characters of every output in a project, offered to the AI for branching scene speakers
    List<CharacterProfile> findByProjectNode_ProjectOutput_Project_IdOrderByIdAsc(Integer projectId);
}

