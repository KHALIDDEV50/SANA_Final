package com.example.sana.Service;

import com.example.sana.API.ApiException;
import com.example.sana.Model.User;
import com.example.sana.Repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    // Get all users
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    // Add a new user
    public void addUser(User user) {

        // Check  email
        User existingUser = userRepository.findUserByEmail(user.getEmail());

        if (existingUser != null) {
            throw new ApiException("Email already exists");
        }

        user.setId(null);
        user.setRole("USER");
        user.setPasswordHash(passwordEncoder.encode(user.getPasswordHash()));
        user.setCreatedAt(LocalDateTime.now());

        userRepository.save(user);
    }

    // Update User
    public void updateUser(Integer userId, User user) {

        // Check if user exists
        User oldUser = userRepository.findUserById(userId);

        if (oldUser == null) {
            throw new ApiException("User not found");
        }

        // Check if email belongs to another user
        User existingUser = userRepository.findUserByEmail(user.getEmail());

        if (existingUser != null && !existingUser.getId().equals(userId)) {
            throw new ApiException("Email already exists");
        }

        // Update user information
        oldUser.setName(user.getName());
        oldUser.setEmail(user.getEmail());
        oldUser.setPasswordHash(passwordEncoder.encode(user.getPasswordHash()));

        userRepository.save(oldUser);
    }

    // Delete an existing user
    public void deleteUser(Integer userId) {

        // Check if user exists
        User user = userRepository.findUserById(userId);

        if (user == null) {
            throw new ApiException("User not found");
        }

        // can not deleting a user who owns projects
        if (user.getProjects() != null && !user.getProjects().isEmpty()) {
            throw new ApiException("Cannot delete user because they own projects");
        }

        userRepository.delete(user);
    }
}