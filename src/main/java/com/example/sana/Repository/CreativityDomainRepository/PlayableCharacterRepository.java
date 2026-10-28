package com.example.sana.Repository.CreativityDomainRepository;

import com.example.sana.Model.CreativityDomainModels.PlayableCharacter;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PlayableCharacterRepository extends JpaRepository<PlayableCharacter, Integer> {

    // Get characters belonging to one ProjectOutput
    List<PlayableCharacter> findByProjectOutput_IdOrderByIdAsc(
            Integer outputId);

    // Find a character only inside its own output
    PlayableCharacter findByIdAndProjectOutput_Id(
            Integer id, Integer outputId);

    // Used before an AI proposal deletes a character that playable characters still reference
    boolean existsByCharacterProfile_Id(Integer characterId);
}
