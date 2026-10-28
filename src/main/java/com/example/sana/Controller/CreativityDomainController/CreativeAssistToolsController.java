package com.example.sana.Controller.CreativityDomainController;

import com.example.sana.Model.User;
import com.example.sana.Service.OwnershipService;
import com.example.sana.Service.CreativityDomainServices.CreativeAssistToolsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

/** Scoped to the project owner; no change to the shared AI workflow endpoints. */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/projects/{projectId}/creativity/assist")
public class CreativeAssistToolsController {
    private final CreativeAssistToolsService tools;
    private final OwnershipService ownership;
    @PostMapping("/balance")
    public ResponseEntity<?> balance(@AuthenticationPrincipal User user,@PathVariable Integer projectId,
                                     @RequestBody(required=false) Map<String,String> input){
        ownership.checkProject(user,projectId);
        return ResponseEntity.status(202).body(tools.startBalance(projectId,user.getId(),parseOutputId(input),input==null?null:input.get("focus")));
    }
    private Integer parseOutputId(Map<String,String> input){
        if(input==null || input.get("outputId")==null)return null;
        try{return Integer.valueOf(input.get("outputId"));}
        catch(NumberFormatException ex){throw new IllegalArgumentException("Invalid story output ID");}
    }
    @PostMapping("/books")
    public ResponseEntity<?> books(@AuthenticationPrincipal User user,@PathVariable Integer projectId,
                                   @RequestBody(required=false) Map<String,String> input){
        ownership.checkProject(user,projectId);
        return ResponseEntity.status(202).body(tools.startBooks(projectId,user.getId(),input==null?null:input.get("focus")));
    }
    @GetMapping("/drafts/{draftId}")
    public ResponseEntity<?> draft(@AuthenticationPrincipal User user,@PathVariable Integer projectId,@PathVariable String draftId){
        ownership.checkProject(user,projectId);
        return ResponseEntity.status(200).body(tools.poll(projectId,user.getId(),draftId));
    }
    @PostMapping("/drafts/{draftId}/approve-balance")
    public ResponseEntity<?> approve(@AuthenticationPrincipal User user,@PathVariable Integer projectId,@PathVariable String draftId){
        ownership.checkProject(user,projectId);
        return ResponseEntity.status(200).body(tools.approveBalance(projectId,user.getId(),draftId));
    }
}
