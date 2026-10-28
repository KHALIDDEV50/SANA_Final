package com.example.sana.Controller.CreativityDomainController;

import com.example.sana.Service.CreativityDomainServices.OpenLibraryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/creativity/books")
@RequiredArgsConstructor
public class OpenLibraryController {

    private final OpenLibraryService openLibraryService;

    // Search for books to inspire creative writing and world-building.
    @GetMapping("/search")
    public ResponseEntity<?> searchBooks(@RequestParam String keyword, @RequestParam(required = false) Integer maxResults, @RequestParam(required = false) Integer startIndex) {
        return ResponseEntity.status(200).body(openLibraryService.searchBooks(keyword, maxResults, startIndex));
    }

    // Get detailed information about a selected book.
    @GetMapping("/get/{workId}")
    public ResponseEntity<?> getBookById(@PathVariable String workId) {
        return ResponseEntity.status(200).body(openLibraryService.getBookById(workId));
    }
}
