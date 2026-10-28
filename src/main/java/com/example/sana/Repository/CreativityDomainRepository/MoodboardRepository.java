package com.example.sana.Repository.CreativityDomainRepository;

import com.example.sana.Model.ProjectNode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface MoodboardRepository extends JpaRepository<ProjectNode, Integer> {

    // Scalar projections prevent fetching the ProjectOutput -> ProjectNode -> ProjectEdge graph.
    @Query("SELECT o.id, o.project.id, o.title FROM ProjectOutput o WHERE o.id = :outputId AND o.outputType = 'MOODBOARD'")
    List<Object[]> findBoard(@Param("outputId") Integer outputId);

    @Query("SELECT o.id, o.project.id, o.title FROM ProjectOutput o WHERE o.project.id = :projectId " +
            "AND o.outputType = 'MOODBOARD' ORDER BY o.id DESC")
    List<Object[]> findBoardsByProject(@Param("projectId") Integer projectId);

    @Query("SELECT n.id, n.type, n.dataJson, n.positionX, n.positionY FROM ProjectNode n " +
            "WHERE n.projectOutput.id = :outputId AND n.type IN ('MOODBOARD_IMAGE', 'MOODBOARD_LABEL') ORDER BY n.id")
    List<Object[]> findItems(@Param("outputId") Integer outputId);

    @Query("SELECT n.id, n.type, n.dataJson, n.positionX, n.positionY FROM ProjectNode n " +
            "WHERE n.id = :itemId AND n.projectOutput.id = :outputId " +
            "AND n.type IN ('MOODBOARD_IMAGE', 'MOODBOARD_LABEL')")
    List<Object[]> findItem(@Param("outputId") Integer outputId, @Param("itemId") Integer itemId);

    // Update only the node's coordinates without loading any related JPA entities.
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("UPDATE ProjectNode n SET n.positionX = :x, n.positionY = :y, n.updatedAt = :today " +
            "WHERE n.id = :itemId AND n.projectOutput.id = :outputId " +
            "AND n.type IN ('MOODBOARD_IMAGE', 'MOODBOARD_LABEL')")
    int moveItem(@Param("outputId") Integer outputId, @Param("itemId") Integer itemId,
                 @Param("x") Double x, @Param("y") Double y, @Param("today") LocalDate today);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("UPDATE ProjectNode n SET n.dataJson = :data, n.updatedAt = :today " +
            "WHERE n.id = :itemId AND n.projectOutput.id = :outputId AND n.type = 'MOODBOARD_LABEL'")
    int updateLabel(@Param("outputId") Integer outputId, @Param("itemId") Integer itemId,
                    @Param("data") String data, @Param("today") LocalDate today);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("DELETE FROM ProjectNode n WHERE n.id = :itemId AND n.projectOutput.id = :outputId " +
            "AND n.type IN ('MOODBOARD_IMAGE', 'MOODBOARD_LABEL')")
    int deleteItem(@Param("outputId") Integer outputId, @Param("itemId") Integer itemId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("UPDATE ProjectOutput o SET o.title = :title, o.updatedAt = :now, o.version = o.version + 1 WHERE o.id = :outputId AND o.outputType = 'MOODBOARD'")
    int renameBoard(@Param("outputId") Integer outputId, @Param("title") String title, @Param("now") LocalDateTime now);
}
