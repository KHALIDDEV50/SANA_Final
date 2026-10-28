
package com.example.sana.Repository.CreativityDomainRepository;

import com.example.sana.Model.CreativityDomainModels.StoryEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import java.util.Optional;

import java.util.List;

@Repository
public interface StoryEventRepository extends JpaRepository<StoryEvent, Integer> {

    StoryEvent findStoryEventById(Integer id);


    // Get events in narrative/presentation order
    @Query("""
            SELECT e FROM StoryEvent e
            JOIN e.projectNode node
            WHERE node.projectOutput.id = :outputId
            ORDER BY e.presentationOrder ASC, e.id ASC
            """)
    List<StoryEvent> findTimelineByPresentationOrder(
            @Param("outputId") Integer outputId);


    // Get events in actual chronological order
    // Events without chronologicalOrder appear last
    // Equal orders fall back to presentationOrder, then ID
    @Query("""
            SELECT e FROM StoryEvent e
            JOIN e.projectNode node
            WHERE node.projectOutput.id = :outputId
            ORDER BY
                CASE WHEN e.chronologicalOrder IS NULL THEN 1 ELSE 0 END ASC,
                e.chronologicalOrder ASC,
                e.presentationOrder ASC,
                e.id ASC
            """)
    List<StoryEvent> findTimelineByChronologicalOrder(
            @Param("outputId") Integer outputId);



    // Search story events inside a specific ProjectOutput
    @Query("""
            SELECT e FROM StoryEvent e
            JOIN e.projectNode node
            WHERE node.projectOutput.id = :outputId
              AND (
                  LOWER(e.title) LIKE LOWER(CONCAT('%', :keyword, '%'))
                  OR LOWER(e.description) LIKE LOWER(CONCAT('%', :keyword, '%'))
                  OR LOWER(e.eventType) LIKE LOWER(CONCAT('%', :keyword, '%'))
                  OR LOWER(e.inWorldTime) LIKE LOWER(CONCAT('%', :keyword, '%'))
              )
            """)
    List<StoryEvent> searchInOutput(
            @Param("outputId") Integer outputId,
            @Param("keyword") String keyword);


    // Lock the StoryEvent row while applying an approved AI update.
    //
    // The calling service must already have an active transaction.
    // This coordinates concurrent AI approval requests for the
    // same story event and helps protect the saved snapshot check.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT e FROM StoryEvent e WHERE e.id = :eventId")
    Optional<StoryEvent> findByIdForUpdate(
            @Param("eventId") Integer eventId);




    // Story events of every output in a project, offered to the AI for branching scenes
    List<StoryEvent> findByProjectNode_ProjectOutput_Project_IdOrderByIdAsc(Integer projectId);
}
