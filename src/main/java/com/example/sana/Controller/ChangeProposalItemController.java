package com.example.sana.Controller;

import com.example.sana.API.ApiResponse;
import com.example.sana.Model.ChangeProposalItem;
import com.example.sana.Service.ChangeProposalItemService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/change-proposal-item")
@RequiredArgsConstructor
public class ChangeProposalItemController {

    private final ChangeProposalItemService changeProposalItemService;

    @GetMapping("/get")
    public ResponseEntity<?> getAllChangeProposalItems() {
        return ResponseEntity.status(200).body(changeProposalItemService.getAllChangeProposalItems());
    }

    @GetMapping("/get/{itemId}")
    public ResponseEntity<?> getChangeProposalItemById(@PathVariable Integer itemId) {
        return ResponseEntity.status(200).body(changeProposalItemService.getChangeProposalItemById(itemId));
    }

    @PostMapping("/add/{proposalId}")
    public ResponseEntity<?> addChangeProposalItem(@PathVariable Integer proposalId,
                                                   @Valid @RequestBody ChangeProposalItem changeProposalItem) {
        changeProposalItemService.addChangeProposalItem(proposalId, changeProposalItem);
        return ResponseEntity.status(200).body(new ApiResponse("Change proposal item added successfully"));
    }

    @PutMapping("/update/{itemId}")
    public ResponseEntity<?> updateChangeProposalItem(@PathVariable Integer itemId,
                                                      @Valid @RequestBody ChangeProposalItem changeProposalItem) {
        changeProposalItemService.updateChangeProposalItem(itemId, changeProposalItem);
        return ResponseEntity.status(200).body(new ApiResponse("Change proposal item updated successfully"));
    }

    @DeleteMapping("/delete/{itemId}")
    public ResponseEntity<?> deleteChangeProposalItem(@PathVariable Integer itemId) {
        changeProposalItemService.deleteChangeProposalItem(itemId);
        return ResponseEntity.status(200).body(new ApiResponse("Change proposal item deleted successfully"));
    }
}
