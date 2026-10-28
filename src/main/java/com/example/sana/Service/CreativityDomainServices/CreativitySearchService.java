package com.example.sana.Service.CreativityDomainServices;

import com.example.sana.API.ApiException;
import com.example.sana.DTO.CreativityDomainDTOs.CreativitySearchResultDTO;
import com.example.sana.Enums.CreativityDomainEnums.CreativitySearchType;
import com.example.sana.Model.ProjectNode;
import com.example.sana.Repository.ProjectOutputRepository;
import com.example.sana.Repository.CreativityDomainRepository.CharacterProfileRepository;
import com.example.sana.Repository.CreativityDomainRepository.WorldRepository;
import com.example.sana.Repository.CreativityDomainRepository.LocationRepository;
import com.example.sana.Repository.CreativityDomainRepository.WorldRuleRepository;
import com.example.sana.Repository.CreativityDomainRepository.StoryEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class CreativitySearchService {

    private final ProjectOutputRepository projectOutputRepository;
    private final CharacterProfileRepository characterProfileRepository;
    private final WorldRepository worldRepository;
    private final LocationRepository locationRepository;
    private final WorldRuleRepository worldRuleRepository;
    private final StoryEventRepository storyEventRepository;


    // Search all creativity elements inside a specific ProjectOutput
    // keyword and type are optional
    @Transactional(readOnly = true)
    public List<CreativitySearchResultDTO> search(Integer outputId, String keyword, String type) {

        // Check if ProjectOutput exists
        if (projectOutputRepository.findProjectOutputById(outputId) == null) {
            throw new ApiException("Project output not found");
        }

        // If no keyword is provided, search using an empty string
        // This allows returning all elements when no keyword is specified
        String searchText = keyword == null ? "" : keyword.trim();

        // Convert the requested type from String to Enum
        // If no type is provided, search all creativity types
        CreativitySearchType selectedType;

        try {
            selectedType = (type == null || type.isBlank()) ? CreativitySearchType.ALL : CreativitySearchType.valueOf(type.trim().toUpperCase(Locale.ROOT));

        } catch (IllegalArgumentException e) {
            throw new ApiException("Invalid creativity type");
        }

        // Store results from all selected creativity types
        List<CreativitySearchResultDTO> results = new ArrayList<>();


        // Search CharacterProfiles
        // Runs when type is ALL or CHARACTER
        if (selectedType == CreativitySearchType.ALL || selectedType == CreativitySearchType.CHARACTER) {

            characterProfileRepository.searchInOutput(outputId, searchText).forEach(character -> results.add(result(
                            character.getId(),
                            character.getProjectNode(),
                            CreativitySearchType.CHARACTER,
                            character.getName(),
                            character.getShortDescription()
                    )));
        }


        // Search Worlds
        // Runs when type is ALL or WORLD
        if (selectedType == CreativitySearchType.ALL || selectedType == CreativitySearchType.WORLD) {

            worldRepository.searchInOutput(outputId, searchText).forEach(world -> results.add(result(
                            world.getId(),
                            world.getProjectNode(),
                            CreativitySearchType.WORLD,
                            world.getName(),
                            world.getDescription()
                    )));
        }


        // Search Locations
        // Runs when type is ALL or LOCATION
        if (selectedType == CreativitySearchType.ALL || selectedType == CreativitySearchType.LOCATION) {

            locationRepository.searchInOutput(outputId, searchText).forEach(location -> results.add(result(
                            location.getId(),
                            location.getProjectNode(),
                            CreativitySearchType.LOCATION,
                            location.getName(),
                            location.getNarrativeSignificance()
                    )));
        }


        // Search WorldRules
        // Runs when type is ALL or WORLD_RULE
        if (selectedType == CreativitySearchType.ALL || selectedType == CreativitySearchType.WORLD_RULE) {

            worldRuleRepository.searchInOutput(outputId, searchText)
                    .forEach(rule -> results.add(result(
                            rule.getId(),
                            rule.getProjectNode(),
                            CreativitySearchType.WORLD_RULE,
                            rule.getName(),
                            rule.getDescription()
                    )));
        }


        // Search StoryEvents
        // Runs when type is ALL or STORY_EVENT
        if (selectedType == CreativitySearchType.ALL || selectedType == CreativitySearchType.STORY_EVENT) {

            storyEventRepository.searchInOutput(outputId, searchText).forEach(event -> results.add(result(
                            event.getId(),
                            event.getProjectNode(),
                            CreativitySearchType.STORY_EVENT,
                            event.getTitle(),
                            event.getDescription()
                    )));
        }


        // Sort results alphabetically by title
        // Then sort by type and ID when titles are identical
        results.sort(Comparator.comparing(CreativitySearchResultDTO::getTitle, String.CASE_INSENSITIVE_ORDER)
                        .thenComparing(CreativitySearchResultDTO::getType)
                        .thenComparing(CreativitySearchResultDTO::getId));

        // Return one unified list to the frontend
        return results;
    }


    // Convert a creativity entity into a unified search result
    // The frontend uses nodeId to select the Node on the canvas
    // The entity ID is used to fetch the element's full details
    private CreativitySearchResultDTO result(Integer id, ProjectNode node, CreativitySearchType type, String title, String description) {

        return new CreativitySearchResultDTO(id, node.getId(), type.name(), title, description, node.getPositionX(), node.getPositionY());
    }
}
