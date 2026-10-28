package com.example.sana.Service.CreativityDomainServices;

import com.example.sana.API.ApiException;
import com.example.sana.Service.DiagramServices.DiagramWriteGuard;
import com.example.sana.DTO.CreativityDomainDTOs.CharacterBalanceReportDTO;
import com.example.sana.DTO.CreativityDomainDTOs.PlayableCharacterDTO;
import com.example.sana.DTO.CreativityDomainDTOs.PlayableCharacterResponseDTO;
import com.example.sana.Model.CreativityDomainModels.*;
import com.example.sana.Model.ProjectOutput;
import com.example.sana.Repository.ProjectOutputRepository;
import com.example.sana.Repository.CreativityDomainRepository.CharacterProfileRepository;
import com.example.sana.Repository.CreativityDomainRepository.PlayableCharacterRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CharacterBalanceService {

    private final PlayableCharacterRepository playableCharacterRepository;
    private final CharacterProfileRepository characterProfileRepository;
    private final ProjectOutputRepository projectOutputRepository;


    // Get all playable characters inside a ProjectOutput
    @Transactional(readOnly = true)
    public List<PlayableCharacterResponseDTO> getAllCharacters(
            Integer outputId) {

        getValidOutput(outputId);

        return playableCharacterRepository.findByProjectOutput_IdOrderByIdAsc(outputId).stream().map(this::toResponse).toList();
    }


    // Get one playable character
    @Transactional(readOnly = true)
    public PlayableCharacterResponseDTO getCharacterById(Integer outputId, Integer characterId) {

        return toResponse(getValidCharacter(outputId, characterId));
    }


    // Create a playable character
    // Weapons and gameplay stats are optional
    @Transactional
    public void addCharacter(Integer outputId, PlayableCharacterDTO dto) {

        ProjectOutput output = getValidOutput(outputId);

        validateStats(dto);

        PlayableCharacter character = new PlayableCharacter();

        setCharacterDetails(character, dto);

        character.setProjectOutput(output);
        character.setCreatedAt(LocalDate.now());
        character.setUpdatedAt(LocalDate.now());

        playableCharacterRepository.save(character);
    }


    // Update character information without changing its story link
    @Transactional
    public void updateCharacter(Integer outputId, Integer characterId, PlayableCharacterDTO dto) {

        PlayableCharacter character = getValidCharacter(outputId, characterId);

        validateStats(dto);

        setCharacterDetails(character, dto);
        character.setUpdatedAt(LocalDate.now());

        playableCharacterRepository.save(character);
    }


    // Delete only the playable character
    // Do not delete its linked CharacterProfile
    @Transactional
    public void deleteCharacter(Integer outputId, Integer characterId) {

        PlayableCharacter character = getValidCharacter(outputId, characterId);

        playableCharacterRepository.delete(character);
    }


    // Link an existing story character from the same Project
    @Transactional
    public void linkCharacterProfile(Integer outputId, Integer playableCharacterId, Integer characterProfileId) {

        PlayableCharacter playable = getValidCharacter(outputId, playableCharacterId);

        CharacterProfile profile = characterProfileRepository.findCharacterProfileById(characterProfileId);

        if (profile == null || profile.getProjectNode() == null) {
            throw new ApiException("Character profile not found");
        }

        ProjectOutput profileOutput = profile.getProjectNode().getProjectOutput();

        if (profileOutput == null || !playable.getProjectOutput().getProject().getId().equals(profileOutput.getProject().getId())) {

            throw new ApiException("Character profile must belong to the same project");
        }

        playable.setCharacterProfile(profile);
        playable.setUpdatedAt(LocalDate.now());

        playableCharacterRepository.save(playable);
    }


    // Remove the story character reference only
    @Transactional
    public void unlinkCharacterProfile(Integer outputId, Integer playableCharacterId) {

        PlayableCharacter playable = getValidCharacter(outputId, playableCharacterId);

        playable.setCharacterProfile(null);
        playable.setUpdatedAt(LocalDate.now());

        playableCharacterRepository.save(playable);
    }


    // Compare only characters with all six gameplay stats
    // This is a preliminary statistical review, not a combat simulation
    @Transactional(readOnly = true)
    public CharacterBalanceReportDTO analyzeBalance(Integer outputId) {

        getValidOutput(outputId);

        List<PlayableCharacter> allCharacters = playableCharacterRepository.findByProjectOutput_IdOrderByIdAsc(outputId);

        List<PlayableCharacter> analyzed = allCharacters.stream().filter(this::hasCompleteStats).toList();

        List<String> notes = new ArrayList<>();
        List<CharacterBalanceReportDTO.CharacterResult> results = new ArrayList<>();

        if (analyzed.isEmpty()) {

            notes.add("No characters have gameplay stats yet.");

            return new CharacterBalanceReportDTO(outputId, allCharacters.size(), 0, null, notes, results);
        }

        // Calculate the group's average stat points
        double average = analyzed.stream().mapToInt(this::calculateTotal).average().orElse(0);

        // Compare characters against the group average
        for (PlayableCharacter character : analyzed) {

            int total = calculateTotal(character);
            List<String> warnings = new ArrayList<>();

            if (analyzed.size() > 1 && average > 0) {

                // Flag point totals more than 10% from the average
                if (Math.abs(total - average) > average * 0.10) {
                    warnings.add("Total points differ from the group average by more than 10%");
                }
            }

            // Detect statistical dominance across all six stats
            for (PlayableCharacter other : analyzed) {

                if (!other.getId().equals(character.getId()) && dominates(other, character)) {

                    warnings.add(other.getName() + " has equal or higher values in every stat" + " and a higher value in at least one stat");
                    break;
                }
            }

            results.add(new CharacterBalanceReportDTO.CharacterResult(character.getId(), character.getName(), total, round(total - average), warnings));
        }

        if (analyzed.size() == 1) {
            notes.add("Only one character has complete stats. Group balance cannot be compared yet.");
        }

        if (analyzed.size() < allCharacters.size()) {
            notes.add("Characters without gameplay stats were excluded from the analysis.");
        }

        notes.add("Equal total points do not guarantee gameplay balance. Weapon mechanics, abilities and playtesting also matter.");

        return new CharacterBalanceReportDTO(outputId, allCharacters.size(), analyzed.size(), round(average), notes, results);
    }


    // Copy editable details from the DTO to the entity
    private void setCharacterDetails(PlayableCharacter character, PlayableCharacterDTO dto) {

        character.setName(dto.getName());
        character.setRole(dto.getRole());
        character.setWeaponName(dto.getWeaponName());
        character.setWeaponType(dto.getWeaponType());
        character.setStrengths(dto.getStrengths());
        character.setWeaknesses(dto.getWeaknesses());

        character.setAttack(dto.getAttack());
        character.setDefense(dto.getDefense());
        character.setSpeed(dto.getSpeed());
        character.setRangeStat(dto.getRangeStat());
        character.setStamina(dto.getStamina());
        character.setSpecial(dto.getSpecial());
    }


    // Allow either all stats or no stats at all
    private void validateStats(PlayableCharacterDTO dto) {

        Integer[] stats = {dto.getAttack(), dto.getDefense(), dto.getSpeed(), dto.getRangeStat(), dto.getStamina(), dto.getSpecial()};

        int provided = 0;

        for (Integer stat : stats) {
            if (stat != null) {
                if (stat < 0 || stat > 10) {
                    throw new ApiException("Gameplay stats must be between 0 and 10");
                }
                provided++;
            }
        }

        if (provided != 0 && provided != 6) {
            throw new ApiException("Provide all six gameplay stats or leave all of them empty");
        }
    }


    // A complete stat profile is eligible for comparison
    private boolean hasCompleteStats(PlayableCharacter c) {

        return c.getAttack() != null
                && c.getDefense() != null
                && c.getSpeed() != null
                && c.getRangeStat() != null
                && c.getStamina() != null
                && c.getSpecial() != null;
    }


    // Calculate total allocated stat points
    private int calculateTotal(PlayableCharacter c) {

        return c.getAttack() + c.getDefense() + c.getSpeed() + c.getRangeStat() + c.getStamina() + c.getSpecial();
    }


    // Check whether one character statistically dominates another
    // This does not account for actual combat mechanics
    private boolean dominates(PlayableCharacter first, PlayableCharacter second) {

        int[] a = {first.getAttack(), first.getDefense(), first.getSpeed(), first.getRangeStat(), first.getStamina(), first.getSpecial()};

        int[] b = {second.getAttack(), second.getDefense(), second.getSpeed(), second.getRangeStat(), second.getStamina(), second.getSpecial()};

        boolean strictlyBetter = false;

        for (int i = 0; i < a.length; i++) {

            if (a[i] < b[i]) {
                return false;
            }

            if (a[i] > b[i]) {
                strictlyBetter = true;
            }
        }

        return strictlyBetter;
    }


    // Round report values to two decimal places
    private double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }


    // Convert the entity into a frontend-friendly response
    private PlayableCharacterResponseDTO toResponse(PlayableCharacter c) {

        CharacterProfile profile = c.getCharacterProfile();

        return new PlayableCharacterResponseDTO(
                c.getId(),
                c.getName(),
                c.getRole(),
                c.getWeaponName(),
                c.getWeaponType(),
                c.getStrengths(),
                c.getWeaknesses(),
                c.getAttack(),
                c.getDefense(),
                c.getSpeed(),
                c.getRangeStat(),
                c.getStamina(),
                c.getSpecial(),
                profile == null ? null : profile.getId(),
                profile == null ? null : profile.getName()
        );
    }


    // Validate the selected ProjectOutput
    private ProjectOutput getValidOutput(Integer outputId) {

        ProjectOutput output = projectOutputRepository.findProjectOutputById(outputId);

        if (output == null) {
            throw new ApiException("Project output not found");
        }

        DiagramWriteGuard.rejectUnversionedWrite(output);
        return output;
    }


    // Validate that the character belongs to this output
    private PlayableCharacter getValidCharacter(Integer outputId, Integer characterId) {

        PlayableCharacter character = playableCharacterRepository.findByIdAndProjectOutput_Id(characterId, outputId);

        if (character == null) {
            throw new ApiException("Playable character not found in this output");
        }

        DiagramWriteGuard.rejectUnversionedWrite(character.getProjectOutput());
        return character;
    }
}
