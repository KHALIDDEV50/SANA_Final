package com.example.sana.Controller;

import com.example.sana.API.ApiResponse;
import com.example.sana.Model.User;
import com.example.sana.Service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    // Create an account; the password field is "passwordHash" and is hashed before saving
    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody User user) {
        authService.register(user);
        return ResponseEntity.status(200).body(new ApiResponse("User registered successfully"));
    }

    // The logged-in user; the frontend calls this to check the email and password
    @GetMapping("/me")
    public ResponseEntity<?> me(@AuthenticationPrincipal User user) {
        return ResponseEntity.status(200).body(user);
    }

    // Logout is handled by Spring Security at POST /api/v1/auth/logout
}
