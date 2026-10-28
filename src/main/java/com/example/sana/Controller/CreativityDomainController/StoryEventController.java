package com.example.sana.Controller.CreativityDomainController;

import com.example.sana.Service.OwnershipService;
import com.example.sana.Model.User;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import com.example.sana.API.ApiResponse;
import com.example.sana.DTO.CreativityDomainDTOs.StoryEventDTO;
import com.example.sana.Service.CreativityDomainServices.StoryEventService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/story-event")
@RequiredArgsConstructor
public class StoryEventController {

    private final StoryEventService storyEventService;
    private final OwnershipService ownershipService;


    // Get all StoryEvents in the system
    @GetMapping("/get")
    public ResponseEntity<?> getAllStoryEvents() {
        return ResponseEntity.status(200).body(storyEventService.getAllStoryEvents());
    }


    // Get a specific StoryEvent by its ID
    @GetMapping("/get/{id}")
    public ResponseEntity<?> getStoryEventById(@AuthenticationPrincipal User user, @PathVariable Integer id) {
        ownershipService.checkStoryEvent(user, id);

        return ResponseEntity.status(200).body(storyEventService.getStoryEventById(id));
    }


    // Add a new StoryEvent to an existing ProjectOutput
    // The backend automatically creates and links a ProjectNode
    // outputId = the ProjectOutput where the event will be created
    @PostMapping("/add/{outputId}")
    public ResponseEntity<?> addStoryEvent(@AuthenticationPrincipal User user, @PathVariable Integer outputId, @RequestBody @Valid StoryEventDTO dto) {
        ownershipService.checkOutput(user, outputId);

        storyEventService.addStoryEvent(outputId, dto);
        return ResponseEntity.status(200).body(new ApiResponse("Story event added successfully"));
    }


    // Update StoryEvent information and its ProjectNode position
    // Updates: title, description, eventType, presentationOrder,
    // inWorldTime, notes, positionX and positionY
    // Does NOT create a new Node or change the ProjectOutput
    @PutMapping("/update/{id}")
    public ResponseEntity<?> updateStoryEvent(@AuthenticationPrincipal User user, @PathVariable Integer id, @RequestBody @Valid StoryEventDTO dto) {
        ownershipService.checkStoryEvent(user, id);

        storyEventService.updateStoryEvent(id, dto);
        return ResponseEntity.status(200).body(new ApiResponse("Story event updated successfully"));
    }


    // Delete a StoryEvent and its associated ProjectNode
    // Connected ProjectEdges may also be deleted by cascade
    @DeleteMapping("/delete/{id}")
    public ResponseEntity<?> deleteStoryEvent(@AuthenticationPrincipal User user, @PathVariable Integer id) {
        ownershipService.checkStoryEvent(user, id);

        storyEventService.deleteStoryEvent(id);
        return ResponseEntity.status(200).body(new ApiResponse("Story event deleted successfully"));
    }

    // Get the Story Timeline in narrative order
    // Events are sorted by presentationOrder
    // outputId = ProjectOutput ID
    @GetMapping("/timeline/presentation/{outputId}")
    public ResponseEntity<?> getTimelineByPresentationOrder(@AuthenticationPrincipal User user, @PathVariable Integer outputId) {
        ownershipService.checkOutput(user, outputId);

        return ResponseEntity.status(200).body(storyEventService.getTimelineByPresentationOrder(outputId));
    }


    // Get the Story Timeline in chronological order
    // Events are sorted by chronologicalOrder
    // Events without chronologicalOrder appear last
    // outputId = ProjectOutput ID
    @GetMapping("/timeline/chronological/{outputId}")
    public ResponseEntity<?> getTimelineByChronologicalOrder(@AuthenticationPrincipal User user, @PathVariable Integer outputId) {
        ownershipService.checkOutput(user, outputId);

        return ResponseEntity.status(200).body(storyEventService.getTimelineByChronologicalOrder(outputId));
    }






}

