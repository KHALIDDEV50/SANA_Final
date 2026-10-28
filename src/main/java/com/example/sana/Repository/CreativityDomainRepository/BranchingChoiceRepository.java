
package com.example.sana.Repository.CreativityDomainRepository;

import com.example.sana.Model.CreativityDomainModels.BranchingChoice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

@Repository
public interface BranchingChoiceRepository extends JpaRepository<BranchingChoice, Integer> {

    // Get all choices inside a ProjectOutput
    List<BranchingChoice> findByProjectEdge_ProjectOutput_IdOrderByIdAsc(
            Integer outputId);

    // Get choices leaving a specific ProjectNode
    List<BranchingChoice> findByProjectEdge_SourceNode_IdOrderByDisplayOrderAscIdAsc(
            Integer projectNodeId);

    // Find a choice inside the selected output
    BranchingChoice findByIdAndProjectEdge_ProjectOutput_Id(
            Integer choiceId, Integer outputId);

    // Check choices connected to a specific ProjectNode
    List<BranchingChoice> findByProjectEdge_SourceNode_Id(
            Integer projectNodeId);

    List<BranchingChoice> findByProjectEdge_TargetNode_Id(
            Integer projectNodeId);


    // Check whether a variable is used in any condition or effect
    boolean existsByConditionVariable_IdOrEffectVariable_Id(Integer conditionVariableId, Integer effectVariableId);


    // Load choice directions, conditions and effects without
    // fetching the entire ProjectEdge and ProjectNode graph.
    @Query("SELECT c.id, c.text, c.displayOrder, pe.sourceNode.id, pe.targetNode.id, " +
            "cv.name, c.conditionOperator, c.conditionValue, " +
            "ev.name, c.effectOperator, c.effectValue " +
            "FROM BranchingChoice c JOIN c.projectEdge pe " +
            "LEFT JOIN c.conditionVariable cv LEFT JOIN c.effectVariable ev " +
            "WHERE pe.projectOutput.id = :outputId " +
            "ORDER BY pe.sourceNode.id, c.displayOrder, c.id")
    List<Object[]> findYarnExportChoices(@Param("outputId") Integer outputId);


    // Select IDs and choice text without loading ProjectEdge entities.
    // Join only the two branching scenes connected by each choice.
    @Query("SELECT c.id, e.id, src.id, dst.id, c.text, c.displayOrder " +
            "FROM BranchingChoice c JOIN c.projectEdge e " +
            "JOIN BranchingNode src ON src.projectNode = e.sourceNode " +
            "JOIN BranchingNode dst ON dst.projectNode = e.targetNode " +
            "WHERE e.projectOutput.id = :outputId ORDER BY c.id")
    List<Object[]> findMapChoices(@Param("outputId") Integer outputId);




    // Find the choice stored for a ProjectEdge
    BranchingChoice findByProjectEdge_Id(Integer projectEdgeId);
}
