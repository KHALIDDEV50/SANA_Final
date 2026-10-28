package com.example.sana.Service.CreativityDomainServices;

import com.example.sana.API.ApiException;
import com.example.sana.Service.DiagramServices.DiagramWriteGuard;
import com.example.sana.DTO.CreativityDomainDTOs.WorldRuleDTO;
import com.example.sana.Model.CreativityDomainModels.World;
import com.example.sana.Model.CreativityDomainModels.WorldRule;
import com.example.sana.Model.ProjectNode;
import com.example.sana.Model.ProjectOutput;
import com.example.sana.Repository.CreativityDomainRepository.WorldRepository;
import com.example.sana.Repository.CreativityDomainRepository.WorldRuleRepository;
import com.example.sana.Repository.ProjectNodeRepository;
import com.example.sana.Repository.ProjectOutputRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class WorldRuleService {

    private final WorldRuleRepository worldRuleRepository;
    private final WorldRepository worldRepository;
    private final ProjectNodeRepository projectNodeRepository;
    private final ProjectOutputRepository projectOutputRepository;

    // Get all world rules
    public List<WorldRule> getAllWorldRules() {
        return worldRuleRepository.findAll();
    }

    // Get a specific world rule by ID
    public WorldRule getWorldRuleById(Integer id) {

        WorldRule worldRule = worldRuleRepository.findWorldRuleById(id);

        if (worldRule == null) {
            throw new ApiException("World rule not found");
        }

        return worldRule;
    }

    // Add a WorldRule and automatically create its ProjectNode
    // worldId is optional: null means an independent rule
    @Transactional
    public void addWorldRule(Integer outputId, Integer worldId, WorldRuleDTO dto) {
        ProjectOutput output = projectOutputRepository.findProjectOutputById(outputId);
        DiagramWriteGuard.rejectUnversionedWrite(output);

        if (output == null) {
            throw new ApiException("Project output not found");
        }

        World world = null;

        // Validate World only when worldId is provided
        if (worldId != null) {
            world = worldRepository.findWorldById(worldId);

            if (world == null) {
                throw new ApiException("World not found");
            }

            if (world.getProjectNode() == null || world.getProjectNode().getProjectOutput() == null || !world.getProjectNode().getProjectOutput().getId().equals(outputId)) {
                throw new ApiException("World does not belong to this project output");
            }
        }

        // Create ProjectNode
        ProjectNode node = new ProjectNode();

        node.setType("WORLD_RULE");
        node.setPositionX(dto.getPositionX());
        node.setPositionY(dto.getPositionY());
        node.setProjectOutput(output);
        node.setCreatedAt(LocalDate.now());
        node.setUpdatedAt(LocalDate.now());

        // Create WorldRule
        WorldRule worldRule = new WorldRule();

        worldRule.setName(dto.getName());
        worldRule.setDescription(dto.getDescription());
        worldRule.setScope(dto.getScope());
        worldRule.setConditions(dto.getConditions());
        worldRule.setExceptions(dto.getExceptions());
        worldRule.setConsequences(dto.getConsequences());
        worldRule.setNotes(dto.getNotes());
        worldRule.setCreatedAt(LocalDate.now());
        worldRule.setUpdatedAt(LocalDate.now());

        // Link World if provided
        worldRule.setWorld(world);

        // Link WorldRule and ProjectNode
        worldRule.setProjectNode(node);
        node.setWorldRule(worldRule);

        // CascadeType.ALL saves both entities
        projectNodeRepository.save(node);
    }

    // Update WorldRule information and existing Node position
    // Does not change the World relationship
    @Transactional
    public void updateWorldRule(Integer id, WorldRuleDTO dto) {

        WorldRule worldRule = worldRuleRepository.findWorldRuleById(id);
        DiagramWriteGuard.rejectUnversionedWrite(worldRule == null || worldRule.getProjectNode() == null ? null : worldRule.getProjectNode().getProjectOutput());

        if (worldRule == null) {
            throw new ApiException("World rule not found");
        }

        ProjectNode node = worldRule.getProjectNode();

        if (node == null) {
            throw new ApiException("Project node not found");
        }

        // Update WorldRule
        worldRule.setName(dto.getName());
        worldRule.setDescription(dto.getDescription());
        worldRule.setScope(dto.getScope());
        worldRule.setConditions(dto.getConditions());
        worldRule.setExceptions(dto.getExceptions());
        worldRule.setConsequences(dto.getConsequences());
        worldRule.setNotes(dto.getNotes());
        worldRule.setUpdatedAt(LocalDate.now());

        // Update existing ProjectNode
        node.setPositionX(dto.getPositionX());
        node.setPositionY(dto.getPositionY());
        node.setUpdatedAt(LocalDate.now());

        worldRuleRepository.save(worldRule);
        projectNodeRepository.save(node);
    }

    // Assign a World to an existing rule or change its World
    @Transactional
    public void updateWorldRuleWorld(Integer ruleId, Integer worldId) {

        WorldRule worldRule = worldRuleRepository.findWorldRuleById(ruleId);
        DiagramWriteGuard.rejectUnversionedWrite(worldRule == null || worldRule.getProjectNode() == null ? null : worldRule.getProjectNode().getProjectOutput());

        if (worldRule == null) {
            throw new ApiException("World rule not found");
        }

        World world = worldRepository.findWorldById(worldId);

        if (world == null) {
            throw new ApiException("World not found");
        }

        ProjectNode ruleNode = worldRule.getProjectNode();
        ProjectNode worldNode = world.getProjectNode();

        if (ruleNode == null || worldNode == null || ruleNode.getProjectOutput() == null || worldNode.getProjectOutput() == null || !ruleNode.getProjectOutput().getId().equals(worldNode.getProjectOutput().getId())) {
            throw new ApiException("World rule and World must belong to the same project output");
        }

        worldRule.setWorld(world);
        worldRule.setUpdatedAt(LocalDate.now());

        worldRuleRepository.save(worldRule);
    }

    // Delete WorldRule and its associated ProjectNode
    @Transactional
    public void deleteWorldRule(Integer id) {

        WorldRule worldRule = worldRuleRepository.findWorldRuleById(id);
        DiagramWriteGuard.rejectUnversionedWrite(worldRule == null || worldRule.getProjectNode() == null ? null : worldRule.getProjectNode().getProjectOutput());

        if (worldRule == null) {
            throw new ApiException("World rule not found");
        }

        ProjectNode node = worldRule.getProjectNode();

        if (node == null) {
            worldRuleRepository.delete(worldRule);
            return;
        }

        projectNodeRepository.delete(node);
    }
}

