package com.example.sana.Service;

import com.example.sana.API.ApiException;
import com.example.sana.DTO.Notification.UserRegisteredEvent;
import com.example.sana.Model.User;
import com.example.sana.Repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    // NOTIFICATION ADDITION: Publishes the registration event.
    private final ApplicationEventPublisher eventPublisher;

    // Create a normal user account with a hashed password.
    // NOTIFICATION ADDITION: Commit registration before sending the notification.
    @Transactional
    public void register(User user) {

        if (userRepository.findUserByEmail(user.getEmail()) != null) {
            throw new ApiException("Email already exists");
        }

        user.setId(null);
        user.setRole("USER");

        String hash = passwordEncoder.encode(user.getPasswordHash());
        user.setPasswordHash(hash);

        user.setCreatedAt(LocalDateTime.now());

        // NOTIFICATION ADDITION: Use the saved user's generated ID.
        User savedUser = userRepository.save(user);

        // NOTIFICATION ADDITION: The listener runs after a successful commit.
        eventPublisher.publishEvent(
                new UserRegisteredEvent(
                        savedUser.getId(),
                        savedUser.getName(),
                        savedUser.getEmail()
                )
        );
    }
}