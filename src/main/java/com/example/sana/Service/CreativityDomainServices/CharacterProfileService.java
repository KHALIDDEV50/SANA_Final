package com.example.sana.Service.CreativityDomainServices;

import com.example.sana.API.ApiException;
import com.example.sana.Service.DiagramServices.DiagramWriteGuard;
import com.example.sana.DTO.CreativityDomainDTOs.CharacterProfileDTO;
import com.example.sana.Model.CreativityDomainModels.CharacterProfile;
import com.example.sana.Model.ProjectNode;
import com.example.sana.Model.ProjectOutput;
import com.example.sana.Repository.CreativityDomainRepository.CharacterProfileRepository;
import com.example.sana.Repository.ProjectNodeRepository;
import com.example.sana.Repository.ProjectOutputRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CharacterProfileService {

    private final CharacterProfileRepository characterProfileRepository;
    private final ProjectNodeRepository projectNodeRepository;
    private final ProjectOutputRepository projectOutputRepository;

    public List<CharacterProfile> getAllCharacterProfiles() {
        return characterProfileRepository.findAll();
    }

    public CharacterProfile getCharacterProfileById(Integer id) {
        CharacterProfile characterProfile = characterProfileRepository.findCharacterProfileById(id);

        if (characterProfile == null) {
            throw new ApiException("Character profile not found");
        }

        return characterProfile;
    }


    @Transactional
    public void addCharacterProfile(Integer outputId, CharacterProfileDTO dto) {

        ProjectOutput output = projectOutputRepository.findProjectOutputById(outputId);
        DiagramWriteGuard.rejectUnversionedWrite(output);

        if (output == null) {
            throw new ApiException("Project output not found");
        }

        // Create ProjectNode
        ProjectNode node = new ProjectNode();
        node.setType("CHARACTER");
        node.setPositionX(dto.getPositionX());
        node.setPositionY(dto.getPositionY());
        node.setProjectOutput(output);
        node.setCreatedAt(LocalDate.now());
        node.setUpdatedAt(LocalDate.now());

        // Create CharacterProfile
        CharacterProfile character = new CharacterProfile();
        character.setName(dto.getName());
        character.setShortDescription(dto.getShortDescription());
        character.setRole(dto.getRole());
        character.setAliases(dto.getAliases());
        character.setAge(dto.getAge());
        character.setAppearance(dto.getAppearance());
        character.setTraits(dto.getTraits());
        character.setStrengths(dto.getStrengths());
        character.setWeaknesses(dto.getWeaknesses());
        character.setSkills(dto.getSkills());
        character.setHistory(dto.getHistory());
        character.setGoals(dto.getGoals());
        character.setFears(dto.getFears());
        character.setInnerConflict(dto.getInnerConflict());
        character.setSecrets(dto.getSecrets());
        character.setCharacterValues(dto.getCharacterValues());
        character.setCharacterArc(dto.getCharacterArc());
        character.setWriterNotes(dto.getWriterNotes());
        character.setCreatedAt(LocalDate.now());
        character.setUpdatedAt(LocalDate.now());

        // Link both sides
        character.setProjectNode(node);
        node.setCharacterProfile(character);

        // CascadeType.ALL saves the character with its node
        projectNodeRepository.save(node);
    }



    public void updateCharacterProfile(Integer id, CharacterProfileDTO dto) {

        CharacterProfile characterProfile = characterProfileRepository.findCharacterProfileById(id);
        DiagramWriteGuard.rejectUnversionedWrite(characterProfile == null || characterProfile.getProjectNode() == null ? null : characterProfile.getProjectNode().getProjectOutput());

        if (characterProfile == null) {
            throw new ApiException("Character profile not found");
        }

        // Update CharacterProfile
        characterProfile.setName(dto.getName());
        characterProfile.setShortDescription(dto.getShortDescription());
        characterProfile.setRole(dto.getRole());
        characterProfile.setAliases(dto.getAliases());
        characterProfile.setAge(dto.getAge());
        characterProfile.setAppearance(dto.getAppearance());
        characterProfile.setTraits(dto.getTraits());
        characterProfile.setStrengths(dto.getStrengths());
        characterProfile.setWeaknesses(dto.getWeaknesses());
        characterProfile.setSkills(dto.getSkills());
        characterProfile.setHistory(dto.getHistory());
        characterProfile.setGoals(dto.getGoals());
        characterProfile.setFears(dto.getFears());
        characterProfile.setInnerConflict(dto.getInnerConflict());
        characterProfile.setSecrets(dto.getSecrets());
        characterProfile.setCharacterValues(dto.getCharacterValues());
        characterProfile.setCharacterArc(dto.getCharacterArc());
        characterProfile.setWriterNotes(dto.getWriterNotes());
        characterProfile.setUpdatedAt(LocalDate.now());

        // Update existing ProjectNode
        ProjectNode node = characterProfile.getProjectNode();

        if (node == null) {
            throw new ApiException("Project node not found");
        }

        node.setPositionX(dto.getPositionX());
        node.setPositionY(dto.getPositionY());
        node.setUpdatedAt(LocalDate.now());

        characterProfileRepository.save(characterProfile);
        projectNodeRepository.save(node);
    }


    @Transactional
    public void deleteCharacterProfile(Integer id) {

        CharacterProfile characterProfile =
                characterProfileRepository.findCharacterProfileById(id);
        DiagramWriteGuard.rejectUnversionedWrite(characterProfile == null || characterProfile.getProjectNode() == null ? null : characterProfile.getProjectNode().getProjectOutput());

        if (characterProfile == null) {
            throw new ApiException("Character profile not found");
        }

        ProjectNode node = characterProfile.getProjectNode();

        if (node == null) {
            characterProfileRepository.delete(characterProfile);
            return;
        }

        projectNodeRepository.delete(node);
    }
}


