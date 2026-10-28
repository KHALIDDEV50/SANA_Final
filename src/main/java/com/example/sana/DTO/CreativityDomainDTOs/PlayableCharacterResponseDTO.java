
package com.example.sana.DTO.CreativityDomainDTOs;

public record PlayableCharacterResponseDTO(
        Integer id,
        String name,
        String role,
        String weaponName,
        String weaponType,
        String strengths,
        String weaknesses,
        Integer attack,
        Integer defense,
        Integer speed,
        Integer rangeStat,
        Integer stamina,
        Integer special,
        Integer characterProfileId,
        String characterProfileName
) {}
