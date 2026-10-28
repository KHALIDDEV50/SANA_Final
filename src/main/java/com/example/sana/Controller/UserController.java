package com.example.sana.Controller;

import com.example.sana.API.ApiResponse;
import com.example.sana.Model.User;
import com.example.sana.Service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    // Get all users
    @GetMapping("/get")
    public ResponseEntity<List<User>> getAllUsers() {
        return ResponseEntity.status(200).body(userService.getAllUsers());
    }

    // Add a new user
    @PostMapping("/add")
    public ResponseEntity<ApiResponse> addUser(@Valid @RequestBody User user) {

        userService.addUser(user);

        return ResponseEntity.status(200).body(new ApiResponse("User added successfully"));
    }

    // Update an existing user
    @PutMapping("/update/{userId}")
    public ResponseEntity<ApiResponse> updateUser(@PathVariable Integer userId, @Valid @RequestBody User user) {

        userService.updateUser(userId, user);

        return ResponseEntity.status(200).body(new ApiResponse("User updated successfully"));
    }

    // Delete an existing user
    @DeleteMapping("/delete/{userId}")
    public ResponseEntity<ApiResponse> deleteUser(@PathVariable Integer userId) {

        userService.deleteUser(userId);

        return ResponseEntity.status(200).body(new ApiResponse("User deleted successfully"));
    }
}
