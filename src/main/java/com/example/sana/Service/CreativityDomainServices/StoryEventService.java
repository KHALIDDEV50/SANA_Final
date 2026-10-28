package com.example.sana.Service.CreativityDomainServices;

import com.example.sana.API.ApiException;
import com.example.sana.Service.DiagramServices.DiagramWriteGuard;
import com.example.sana.DTO.CreativityDomainDTOs.StoryEventDTO;
import com.example.sana.DTO.CreativityDomainDTOs.StoryEventTimelineDTO;
import com.example.sana.Model.CreativityDomainModels.StoryEvent;
import com.example.sana.Model.ProjectNode;
import com.example.sana.Model.ProjectOutput;
import com.example.sana.Repository.CreativityDomainRepository.StoryEventRepository;
import com.example.sana.Repository.ProjectNodeRepository;
import com.example.sana.Repository.ProjectOutputRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class StoryEventService {

    private final StoryEventRepository storyEventRepository;
    private final ProjectNodeRepository projectNodeRepository;
    private final ProjectOutputRepository projectOutputRepository;


    // Get all story events
    public List<StoryEvent> getAllStoryEvents() {
        return storyEventRepository.findAll();
    }


    // Get a specific story event by ID
    public StoryEvent getStoryEventById(Integer id) {

        StoryEvent storyEvent = storyEventRepository.findStoryEventById(id);

        if (storyEvent == null) {
            throw new ApiException("Story event not found");
        }

        return storyEvent;
    }


    // Create a StoryEvent and its ProjectNode together
    @Transactional
    public void addStoryEvent(Integer outputId, StoryEventDTO dto) {

        ProjectOutput output = projectOutputRepository.findProjectOutputById(outputId);
        DiagramWriteGuard.rejectUnversionedWrite(output);

        if (output == null) {
            throw new ApiException("Project output not found");
        }

        // Create ProjectNode
        ProjectNode node = new ProjectNode();

        node.setType("STORY_EVENT");
        node.setPositionX(dto.getPositionX());
        node.setPositionY(dto.getPositionY());
        node.setProjectOutput(output);
        node.setCreatedAt(LocalDate.now());
        node.setUpdatedAt(LocalDate.now());

        // Create StoryEvent
        StoryEvent storyEvent = new StoryEvent();

        storyEvent.setTitle(dto.getTitle());
        storyEvent.setDescription(dto.getDescription());
        storyEvent.setEventType(dto.getEventType());

        // Save narrative and chronological orders
        storyEvent.setPresentationOrder(dto.getPresentationOrder());
        storyEvent.setChronologicalOrder(dto.getChronologicalOrder());

        storyEvent.setInWorldTime(dto.getInWorldTime());
        storyEvent.setNotes(dto.getNotes());
        storyEvent.setCreatedAt(LocalDate.now());
        storyEvent.setUpdatedAt(LocalDate.now());

        // Link both sides
        storyEvent.setProjectNode(node);
        node.setStoryEvent(storyEvent);

        // Save Node and StoryEvent using CascadeType.ALL
        projectNodeRepository.save(node);
    }


    // Update StoryEvent and its existing ProjectNode
    @Transactional
    public void updateStoryEvent(Integer id, StoryEventDTO dto) {

        StoryEvent storyEvent = storyEventRepository.findStoryEventById(id);
        DiagramWriteGuard.rejectUnversionedWrite(storyEvent == null || storyEvent.getProjectNode() == null ? null : storyEvent.getProjectNode().getProjectOutput());

        if (storyEvent == null) {
            throw new ApiException("Story event not found");
        }

        ProjectNode node = storyEvent.getProjectNode();

        if (node == null) {
            throw new ApiException("Project node not found");
        }

        // Update StoryEvent details
        storyEvent.setTitle(dto.getTitle());
        storyEvent.setDescription(dto.getDescription());
        storyEvent.setEventType(dto.getEventType());

        // Update both ordering fields
        storyEvent.setPresentationOrder(dto.getPresentationOrder());
        storyEvent.setChronologicalOrder(dto.getChronologicalOrder());

        storyEvent.setInWorldTime(dto.getInWorldTime());
        storyEvent.setNotes(dto.getNotes());
        storyEvent.setUpdatedAt(LocalDate.now());

        // Update Node position
        node.setPositionX(dto.getPositionX());
        node.setPositionY(dto.getPositionY());
        node.setUpdatedAt(LocalDate.now());

        storyEventRepository.save(storyEvent);
        projectNodeRepository.save(node);
    }



    // Get StoryEvents ordered by their narrative order
    // This is the order in which events appear in the story
    @Transactional(readOnly = true)
    public List<StoryEventTimelineDTO> getTimelineByPresentationOrder(Integer outputId) {

        ProjectOutput output = projectOutputRepository.findProjectOutputById(outputId);

        if (output == null) {
            throw new ApiException("Project output not found");
        }

        List<StoryEvent> events = storyEventRepository.findTimelineByPresentationOrder(outputId);

        return events.stream()
                .map(event -> new StoryEventTimelineDTO(
                        event.getId(),
                        event.getProjectNode().getId(),
                        event.getTitle(),
                        event.getDescription(),
                        event.getEventType(),
                        event.getPresentationOrder(),
                        event.getChronologicalOrder(),
                        event.getInWorldTime()
                ))
                .toList();
    }


    // Get StoryEvents ordered by their actual chronological order
    // Events without chronologicalOrder appear at the end
    @Transactional(readOnly = true)
    public List<StoryEventTimelineDTO> getTimelineByChronologicalOrder(Integer outputId) {

        ProjectOutput output = projectOutputRepository.findProjectOutputById(outputId);

        if (output == null) {
            throw new ApiException("Project output not found");
        }

        List<StoryEvent> events = storyEventRepository.findTimelineByChronologicalOrder(outputId);

        return events.stream()
                .map(event -> new StoryEventTimelineDTO(
                        event.getId(),
                        event.getProjectNode().getId(),
                        event.getTitle(),
                        event.getDescription(),
                        event.getEventType(),
                        event.getPresentationOrder(),
                        event.getChronologicalOrder(),
                        event.getInWorldTime()
                ))
                .toList();
    }



    // Delete StoryEvent and its associated ProjectNode
    @Transactional
    public void deleteStoryEvent(Integer id) {

        StoryEvent storyEvent = storyEventRepository.findStoryEventById(id);
        DiagramWriteGuard.rejectUnversionedWrite(storyEvent == null || storyEvent.getProjectNode() == null ? null : storyEvent.getProjectNode().getProjectOutput());

        if (storyEvent == null) {
            throw new ApiException("Story event not found");
        }

        ProjectNode node = storyEvent.getProjectNode();

        if (node == null) {
            storyEventRepository.delete(storyEvent);
            return;
        }

        projectNodeRepository.delete(node);
    }
}
