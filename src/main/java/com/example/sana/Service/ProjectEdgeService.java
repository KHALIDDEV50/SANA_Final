package com.example.sana.Service;

import com.example.sana.API.ApiException;
import com.example.sana.Service.DiagramServices.DiagramWriteGuard;
import com.example.sana.Model.ProjectEdge;
import com.example.sana.Model.ProjectNode;
import com.example.sana.Model.ProjectOutput;
import com.example.sana.Repository.ProjectEdgeRepository;
import com.example.sana.Repository.ProjectNodeRepository;
import com.example.sana.Repository.ProjectOutputRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ProjectEdgeService {

    private final ProjectEdgeRepository projectEdgeRepository;
    private final ProjectOutputRepository projectOutputRepository;
    private final ProjectNodeRepository projectNodeRepository;

    public List<ProjectEdge> getAllEdges(){
        return projectEdgeRepository.findAll();
    }

    public void addEdge(Integer outputId, Integer sourceNodeId, Integer targetNodeId, ProjectEdge projectEdge){
        ProjectOutput output = projectOutputRepository.findProjectOutputById(outputId);
        if (output == null){
            throw new ApiException("Project output not found");
        }
        DiagramWriteGuard.rejectUnversionedWrite(output);
        ProjectNode sourceNode = projectNodeRepository.findProjectNodeById(sourceNodeId);
        if (sourceNode == null){
            throw new ApiException("Source node not found");
        }
        ProjectNode targetNode = projectNodeRepository.findProjectNodeById(targetNodeId);
        if (targetNode == null){
            throw new ApiException("Target node not found");
        }
        if (sourceNode.getProjectOutput() == null || targetNode.getProjectOutput() == null
                || !outputId.equals(sourceNode.getProjectOutput().getId())
                || !outputId.equals(targetNode.getProjectOutput().getId())){
            throw new ApiException("Source and target nodes must belong to the edge output");
        }
        projectEdge.setId(null);
        projectEdge.setProjectOutput(output);
        projectEdge.setSourceNode(sourceNode);
        projectEdge.setTargetNode(targetNode);
        projectEdge.setCreatedAt(LocalDate.now());
        projectEdgeRepository.save(projectEdge);
    }

    public void updateEdge(Integer projectEdgeId, ProjectEdge projectEdge){
        ProjectEdge edge = projectEdgeRepository.findProjectEdgeById(projectEdgeId);
        if (edge == null){
            throw new ApiException("project edge not found");
        }

        DiagramWriteGuard.rejectUnversionedWrite(edge.getProjectOutput());
        edge.setDataJson(projectEdge.getDataJson());
        edge.setType(projectEdge.getType());
        edge.setLabel(projectEdge.getLabel());
        projectEdgeRepository.save(edge);
    }

    public void deleteEdge(Integer edgeId){
        ProjectEdge edge = projectEdgeRepository.findProjectEdgeById(edgeId);
        if (edge == null){
            throw new ApiException("project edge not found");
        }
        DiagramWriteGuard.rejectUnversionedWrite(edge.getProjectOutput());
        projectEdgeRepository.delete(edge);
    }


}
