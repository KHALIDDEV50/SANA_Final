package com.example.sana.Service.CreativityDomainServices;

import com.example.sana.API.ApiException;
import com.example.sana.Service.DiagramServices.DiagramWriteGuard;
import com.example.sana.DTO.CreativityDomainDTOs.WorldDTO;
import com.example.sana.Model.CreativityDomainModels.World;
import com.example.sana.Model.ProjectNode;
import com.example.sana.Model.ProjectOutput;
import com.example.sana.Repository.CreativityDomainRepository.WorldRepository;
import com.example.sana.Repository.ProjectNodeRepository;
import com.example.sana.Repository.ProjectOutputRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class WorldService {

    private final WorldRepository worldRepository;
    private final ProjectNodeRepository projectNodeRepository;
    private final ProjectOutputRepository projectOutputRepository;

    public List<World> getAllWorlds() {
        return worldRepository.findAll();
    }

    public World getWorldById(Integer id) {
        World world = worldRepository.findWorldById(id);

        if (world == null) {
            throw new ApiException("World not found");
        }

        return world;
    }

    @Transactional
    public void addWorld(Integer outputId, WorldDTO dto) {

        ProjectOutput output = projectOutputRepository.findProjectOutputById(outputId);
        DiagramWriteGuard.rejectUnversionedWrite(output);

        if (output == null) {
            throw new ApiException("Project output not found");
        }

        // Create ProjectNode
        ProjectNode node = new ProjectNode();

        node.setType("WORLD");
        node.setPositionX(dto.getPositionX());
        node.setPositionY(dto.getPositionY());
        node.setProjectOutput(output);
        node.setCreatedAt(LocalDate.now());
        node.setUpdatedAt(LocalDate.now());

        // Create World
        World world = new World();

        world.setName(dto.getName());
        world.setDescription(dto.getDescription());
        world.setHistory(dto.getHistory());
        world.setLore(dto.getLore());
        world.setAtmosphere(dto.getAtmosphere());
        world.setNotes(dto.getNotes());
        world.setCreatedAt(LocalDate.now());
        world.setUpdatedAt(LocalDate.now());

        // Link both sides
        world.setProjectNode(node);
        node.setWorld(world);

        // Saves node and world together using CascadeType.ALL
        projectNodeRepository.save(node);
    }

    @Transactional
    public void updateWorld(Integer id, WorldDTO dto) {

        World world = worldRepository.findWorldById(id);
        DiagramWriteGuard.rejectUnversionedWrite(world == null || world.getProjectNode() == null ? null : world.getProjectNode().getProjectOutput());

        if (world == null) {
            throw new ApiException("World not found");
        }

        ProjectNode node = world.getProjectNode();

        if (node == null) {
            throw new ApiException("Project node not found");
        }

        // Update World
        world.setName(dto.getName());
        world.setDescription(dto.getDescription());
        world.setHistory(dto.getHistory());
        world.setLore(dto.getLore());
        world.setAtmosphere(dto.getAtmosphere());
        world.setNotes(dto.getNotes());
        world.setUpdatedAt(LocalDate.now());

        // Update existing ProjectNode
        node.setPositionX(dto.getPositionX());
        node.setPositionY(dto.getPositionY());
        node.setUpdatedAt(LocalDate.now());

        worldRepository.save(world);
        projectNodeRepository.save(node);
    }

    @Transactional
    public void deleteWorld(Integer id) {

        World world = worldRepository.findWorldById(id);
        DiagramWriteGuard.rejectUnversionedWrite(world == null || world.getProjectNode() == null ? null : world.getProjectNode().getProjectOutput());

        if (world == null) {
            throw new ApiException("World not found");
        }

        ProjectNode node = world.getProjectNode();

        if (node == null) {
            worldRepository.delete(world);
            return;
        }

        projectNodeRepository.delete(node);
    }
}

