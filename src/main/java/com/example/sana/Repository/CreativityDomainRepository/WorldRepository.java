package com.example.sana.Repository.CreativityDomainRepository;


import com.example.sana.Model.CreativityDomainModels.World;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import java.util.Optional;

import java.util.List;

@Repository
public interface WorldRepository extends JpaRepository<World, Integer> {
    World findWorldById(Integer id);


    // Search worlds by name, description, lore or history
    @Query("""
            SELECT w FROM World w
            JOIN w.projectNode node
            WHERE node.projectOutput.id = :outputId
              AND (
                  LOWER(w.name) LIKE LOWER(CONCAT('%', :keyword, '%'))
                  OR LOWER(w.description) LIKE LOWER(CONCAT('%', :keyword, '%'))
                  OR LOWER(w.lore) LIKE LOWER(CONCAT('%', :keyword, '%'))
                  OR LOWER(w.history) LIKE LOWER(CONCAT('%', :keyword, '%'))
              )
            """)
    List<World> searchInOutput(@Param("outputId") Integer outputId, @Param("keyword") String keyword);


    // Lock a world while applying an approved AI update.
    // This protects the World row during the approval transaction.
    //
    // The method must be called inside an active transaction.
    // It does not replace normal read methods used elsewhere.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT w FROM World w WHERE w.id = :worldId")
    Optional<World> findByIdForUpdate(@Param("worldId") Integer worldId);

}

