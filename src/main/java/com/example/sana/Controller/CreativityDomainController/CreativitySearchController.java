
package com.example.sana.Controller.CreativityDomainController;

import com.example.sana.Service.OwnershipService;
import com.example.sana.Model.User;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import com.example.sana.Service.CreativityDomainServices.CreativitySearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/creativity")
@RequiredArgsConstructor
public class CreativitySearchController {

    private final CreativitySearchService creativitySearchService;
    private final OwnershipService ownershipService;

    // Search creativity elements inside a specific ProjectOutput
    // outputId = ProjectOutput ID
    // keyword = optional search text
    // type = optional filter (ALL, CHARACTER, WORLD,
    // LOCATION, WORLD_RULE, STORY_EVENT)
    //
    // Returns IDs, Node IDs, titles and positions
    // so the frontend can locate the Node on the canvas

    @GetMapping("/search/{outputId}")
    public ResponseEntity<?> search(@AuthenticationPrincipal User user, 
            @PathVariable Integer outputId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String type) {
        ownershipService.checkOutput(user, outputId);

        return ResponseEntity.status(200).body(creativitySearchService.search(outputId, keyword, type));
    }
}
