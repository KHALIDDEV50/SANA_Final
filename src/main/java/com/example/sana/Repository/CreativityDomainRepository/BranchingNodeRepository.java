package com.example.sana.Repository.CreativityDomainRepository;

import com.example.sana.Model.CreativityDomainModels.BranchingNode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

@Repository
public interface BranchingNodeRepository extends JpaRepository<BranchingNode, Integer> {

    // Get all branching scenes inside a specific output
    List<BranchingNode> findByProjectNode_ProjectOutput_IdOrderByIdAsc(
            Integer outputId);

    // Get one branching scene within the selected output
    BranchingNode findByIdAndProjectNode_ProjectOutput_Id(
            Integer nodeId, Integer outputId);

    // Find the branching scene connected to a ProjectNode
    BranchingNode findByProjectNode_Id(Integer projectNodeId);


    // Load only the fields required to export branching scenes.
    // Scalar queries avoid fetching the complete JPA entity graph.
    @Query("SELECT n.id, n.title, n.content, n.type, pn.id, s.name " +
            "FROM BranchingNode n JOIN n.projectNode pn LEFT JOIN n.speaker s " +
            "WHERE pn.projectOutput.id = :outputId ORDER BY n.id")
    List<Object[]> findYarnExportNodes(@Param("outputId") Integer outputId);


    // Load only the scene fields needed by the branching map.
    // Avoid loading the full ProjectNode relationship graph.
    @Query("SELECT n.id, pn.id, n.title, n.content, n.type, pn.positionX, pn.positionY " +
            "FROM BranchingNode n JOIN n.projectNode pn " +
            "WHERE pn.projectOutput.id = :outputId ORDER BY n.id")
    List<Object[]> findMapNodes(@Param("outputId") Integer outputId);



    // Used before an AI proposal deletes a character or story event that scenes still reference
    boolean existsBySpeaker_Id(Integer characterId);

    boolean existsByRelatedStoryEvent_Id(Integer storyEventId);
}
