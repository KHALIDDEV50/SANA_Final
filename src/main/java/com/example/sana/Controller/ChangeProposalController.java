package com.example.sana.Controller;

import com.example.sana.API.ApiResponse;
import com.example.sana.Model.ChangeProposal;
import com.example.sana.Service.ChangeProposalService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/change-proposal")
@RequiredArgsConstructor
public class ChangeProposalController {

    private final ChangeProposalService changeProposalService;

    @GetMapping("/get")
    public ResponseEntity<?> getAllChangeProposals() {
        return ResponseEntity.status(200).body(changeProposalService.getAllChangeProposals());
    }

    @GetMapping("/get/{proposalId}")
    public ResponseEntity<?> getChangeProposalById(@PathVariable Integer proposalId) {
        return ResponseEntity.status(200).body(changeProposalService.getChangeProposalById(proposalId));
    }

    @PostMapping("/add/{projectId}")
    public ResponseEntity<?> addChangeProposal(@PathVariable Integer projectId,
                                               @RequestParam(required = false) Integer aiInteractionId,
                                               @Valid @RequestBody ChangeProposal changeProposal) {
        changeProposalService.addChangeProposal(projectId, aiInteractionId, changeProposal);
        return ResponseEntity.status(200).body(new ApiResponse("Change proposal added successfully"));
    }

    @PutMapping("/update/{proposalId}")
    public ResponseEntity<?> updateChangeProposal(@PathVariable Integer proposalId,
                                                  @Valid @RequestBody ChangeProposal changeProposal) {
        changeProposalService.updateChangeProposal(proposalId, changeProposal);
        return ResponseEntity.status(200).body(new ApiResponse("Change proposal updated successfully"));
    }

    @DeleteMapping("/delete/{proposalId}")
    public ResponseEntity<?> deleteChangeProposal(@PathVariable Integer proposalId) {
        changeProposalService.deleteChangeProposal(proposalId);
        return ResponseEntity.status(200).body(new ApiResponse("Change proposal deleted successfully"));
    }
}
