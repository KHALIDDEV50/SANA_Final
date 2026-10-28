package com.example.sana.Controller.CreativityDomainController;

import com.example.sana.Service.OwnershipService;
import com.example.sana.Model.User;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import com.example.sana.API.ApiResponse;
import com.example.sana.DTO.CreativityDomainDTOs.MoodboardDTO;
import com.example.sana.Service.CreativityDomainServices.MoodboardService;
import com.example.sana.Service.CreativityDomainServices.OpenverseService;
import com.example.sana.Service.CreativityDomainServices.MoodboardImageService;
import com.example.sana.Service.CreativityDomainServices.PixabayImageStorage;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/creativity/moodboard")
@RequiredArgsConstructor
public class MoodboardController {

    private final MoodboardService moodboardService;
    private final OpenverseService openverseService; // Legacy image details only; never searched by new UI/AI.
    private final MoodboardImageService imageService;
    private final PixabayImageStorage pixabayStorage;
    private final OwnershipService ownershipService;

    // Unified search across four sources; Openverse is not queried.
    @GetMapping("/images/search")
    public ResponseEntity<?> searchImages(@RequestParam String keyword,
                                          @RequestParam(required = false) Integer page,
                                          @RequestParam(required = false) Integer pageSize) {
        return ResponseEntity.status(200).body(imageService.searchImages(keyword, page, pageSize));
    }

    // Serve Pixabay media copied to persistent storage after saving (Pixabay forbids permanent hotlinks).
    @GetMapping("/images/pixabay-assets/{id}")
    public ResponseEntity<byte[]> savedPixabayAsset(@PathVariable String id) {
        byte[] bytes = pixabayStorage.read(id);
        return ResponseEntity.status(200).contentType(pixabayStorage.mimeType(bytes)).body(bytes);
    }

    // Kept solely to avoid breaking old integrations; the UI and AI do not use Openverse.
    // Fetch complete image attribution before selecting an image.
    @GetMapping("/images/{openverseId}")
    public ResponseEntity<?> imageDetails(@PathVariable String openverseId) {
        return ResponseEntity.status(200).body(openverseService.getImage(openverseId));
    }

    // Create one board inside an existing SANA Project.
    @PostMapping("/add/{projectId}")
    public ResponseEntity<?> createBoard(@AuthenticationPrincipal User user, @PathVariable Integer projectId, @Valid @RequestBody MoodboardDTO.CreateBoard request) {
        ownershipService.checkProject(user, projectId);

        return ResponseEntity.status(200).body(moodboardService.createBoard(projectId, request));
    }

    @GetMapping("/get/project/{projectId}")
    public ResponseEntity<?> projectBoards(@AuthenticationPrincipal User user, @PathVariable Integer projectId) {
        ownershipService.checkProject(user, projectId);

        return ResponseEntity.status(200).body(moodboardService.getProjectBoards(projectId));
    }

    @GetMapping("/get/{outputId}")
    public ResponseEntity<?> getBoard(@AuthenticationPrincipal User user, @PathVariable Integer outputId) {
        ownershipService.checkOutput(user, outputId);

        return ResponseEntity.status(200).body(moodboardService.getBoard(outputId));
    }

    @PutMapping("/update/{outputId}/title")
    public ResponseEntity<?> renameBoard(@AuthenticationPrincipal User user, @PathVariable Integer outputId, @Valid @RequestBody MoodboardDTO.RenameBoard request) {
        ownershipService.checkOutput(user, outputId);

        return ResponseEntity.status(200).body(moodboardService.renameBoard(outputId, request));
    }

    // Add a verified image from one of the four active providers as a standalone ProjectNode.
    @PostMapping("/add/{outputId}/images")
    public ResponseEntity<?> addImage(@AuthenticationPrincipal User user, @PathVariable Integer outputId, @Valid @RequestBody MoodboardDTO.AddImage request) {
        ownershipService.checkOutput(user, outputId);

        return ResponseEntity.status(200).body(moodboardService.addImage(outputId, request));
    }

    // Add an independent, freely movable text label.
    @PostMapping("/add/{outputId}/labels")
    public ResponseEntity<?> addLabel(@AuthenticationPrincipal User user, @PathVariable Integer outputId, @Valid @RequestBody MoodboardDTO.AddLabel request) {
        ownershipService.checkOutput(user, outputId);

        return ResponseEntity.status(200).body(moodboardService.addLabel(outputId, request));
    }

    // Save X/Y after drag-and-drop for either image or label.
    @PutMapping("/update/{outputId}/items/{itemId}/position")
    public ResponseEntity<?> moveItem(@AuthenticationPrincipal User user, @PathVariable Integer outputId, @PathVariable Integer itemId,
                                      @Valid @RequestBody MoodboardDTO.MoveItem request) {
        ownershipService.checkOutput(user, outputId);

        return ResponseEntity.status(200).body(moodboardService.moveItem(outputId, itemId, request));
    }

    @PutMapping("/update/{outputId}/labels/{itemId}")
    public ResponseEntity<?> editLabel(@AuthenticationPrincipal User user, @PathVariable Integer outputId, @PathVariable Integer itemId,
                                       @Valid @RequestBody MoodboardDTO.EditLabel request) {
        ownershipService.checkOutput(user, outputId);

        return ResponseEntity.status(200).body(moodboardService.editLabel(outputId, itemId, request));
    }

    @DeleteMapping("/delete/{outputId}/items/{itemId}")
    public ResponseEntity<?> deleteItem(@AuthenticationPrincipal User user, @PathVariable Integer outputId, @PathVariable Integer itemId) {
        ownershipService.checkOutput(user, outputId);

        moodboardService.deleteItem(outputId, itemId);
        return ResponseEntity.status(200).body(new ApiResponse("Moodboard item deleted successfully"));
    }
}
