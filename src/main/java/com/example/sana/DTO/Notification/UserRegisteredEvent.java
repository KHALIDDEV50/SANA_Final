package com.example.sana.DTO.Notification;

// NOTIFICATION ADDITION: Contains only the data needed for a welcome email.
public record UserRegisteredEvent(
        Integer userId,
        String name,
        String email
) {
}