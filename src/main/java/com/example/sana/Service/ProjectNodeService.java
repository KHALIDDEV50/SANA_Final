package com.example.sana.Service;

import com.example.sana.API.ApiException;
import com.example.sana.Service.DiagramServices.DiagramWriteGuard;
import com.example.sana.Model.ProjectOutput;
import com.example.sana.Model.ProjectNode;
import com.example.sana.Repository.ProjectNodeRepository;
import com.example.sana.Repository.ProjectOutputRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ProjectNodeService {

    private final ProjectNodeRepository projectNodeRepository;
    private final ProjectOutputRepository projectOutputRepository;

    public List<ProjectNode> getAllNodes(){
        return projectNodeRepository.findAll();
    }

    public void addNode(Integer outputId, ProjectNode projectNode){
        ProjectOutput output = projectOutputRepository.findProjectOutputById(outputId);
        if (output == null){
            throw new ApiException("Project output not found");
        }
        DiagramWriteGuard.rejectUnversionedWrite(output);
        projectNode.setId(null);
        projectNode.setProjectOutput(output);
        projectNode.setOutgoingEdges(null);
        projectNode.setIncomingEdges(null);
        projectNode.setCreatedAt(LocalDate.now());
        projectNode.setUpdatedAt(LocalDate.now());
        projectNodeRepository.save(projectNode);
    }


    public void updateNode(Integer projectNodeId, ProjectNode projectNode){
        ProjectNode node = projectNodeRepository.findProjectNodeById(projectNodeId);
        if (node == null){
            throw new ApiException("node not found");
        }

        DiagramWriteGuard.rejectUnversionedWrite(node.getProjectOutput());
        node.setUpdatedAt(LocalDate.now());
        node.setType(projectNode.getType());
        node.setDataJson(projectNode.getDataJson());
        node.setPositionX(projectNode.getPositionX());
        node.setPositionY(projectNode.getPositionY());
        projectNodeRepository.save(node);
    }

    public void deleteNode(Integer nodeId){
        ProjectNode node = projectNodeRepository.findProjectNodeById(nodeId);
        if (node == null){
            throw new ApiException("node not found");
        }
        DiagramWriteGuard.rejectUnversionedWrite(node.getProjectOutput());
        projectNodeRepository.delete(node);
    }


}
