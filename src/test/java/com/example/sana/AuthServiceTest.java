package com.example.sana;

import com.example.sana.API.ApiException;
import com.example.sana.DTO.Notification.UserRegisteredEvent;
import com.example.sana.Model.User;
import com.example.sana.Repository.UserRepository;
import com.example.sana.Service.AuthService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AuthServiceTest {

    @InjectMocks
    AuthService authService;
    @Mock
    UserRepository userRepository;
    @Mock
    PasswordEncoder passwordEncoder;
    @Mock
    ApplicationEventPublisher eventPublisher;

    User user;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(5);
        user.setName("Abdullah");
        user.setEmail("abdullah@gmail.com");
        user.setPasswordHash("12345678");
        user.setRole("ADMIN");
    }

    @Test
    public void registerTest() {
        when(userRepository.findUserByEmail(user.getEmail())).thenReturn(null);
        when(passwordEncoder.encode("12345678")).thenReturn("{bcrypt}hashed");
        when(userRepository.save(user)).thenReturn(user);

        authService.register(user);
        Assertions.assertNull(user.getId());
        Assertions.assertEquals("USER", user.getRole());
        Assertions.assertEquals("{bcrypt}hashed", user.getPasswordHash());

        verify(userRepository, times(1)).findUserByEmail(user.getEmail());
        verify(passwordEncoder, times(1)).encode("12345678");
        verify(userRepository, times(1)).save(user);
        // The welcome email is sent through an event after the account is saved
        verify(eventPublisher, times(1)).publishEvent(any(UserRegisteredEvent.class));
    }

    @Test
    public void registerDuplicateEmailTest() {
        when(userRepository.findUserByEmail(user.getEmail())).thenReturn(user);

        ApiException exception = Assertions.assertThrows(ApiException.class, () -> authService.register(user));
        Assertions.assertEquals("Email already exists", exception.getMessage());

        verify(userRepository, times(1)).findUserByEmail(user.getEmail());
        verify(userRepository, never()).save(user);
        verify(eventPublisher, never()).publishEvent(any(UserRegisteredEvent.class));
    }
}
