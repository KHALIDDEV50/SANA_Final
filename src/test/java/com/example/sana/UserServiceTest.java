package com.example.sana;

import com.example.sana.API.ApiException;
import com.example.sana.Model.Project;
import com.example.sana.Model.User;
import com.example.sana.Repository.UserRepository;
import com.example.sana.Service.UserService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.ArrayList;
import java.util.List;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class UserServiceTest {

    @InjectMocks
    UserService userService;
    @Mock
    UserRepository userRepository;
    @Mock
    PasswordEncoder passwordEncoder;

    User user1, user2;
    List<User> users;

    @BeforeEach
    void setUp() {
        user1 = user(1, "Abdullah", "abdullah@gmail.com");
        user2 = user(2, "Razan", "razan@gmail.com");

        users = new ArrayList<>();
        users.add(user1);
        users.add(user2);
    }

    private User user(Integer id, String name, String email) {
        User user = new User();
        user.setId(id);
        user.setName(name);
        user.setEmail(email);
        user.setPasswordHash("12345678");
        user.setRole("USER");
        return user;
    }

    @Test
    public void getAllUsersTest() {
        when(userRepository.findAll()).thenReturn(users);

        List<User> userList = userService.getAllUsers();
        Assertions.assertEquals(2, userList.size());

        verify(userRepository, times(1)).findAll();
    }

    @Test
    public void addUserTest() {
        when(userRepository.findUserByEmail(user2.getEmail())).thenReturn(null);
        when(passwordEncoder.encode("12345678")).thenReturn("{bcrypt}hashed");

        userService.addUser(user2);
        Assertions.assertEquals("{bcrypt}hashed", user2.getPasswordHash());
        Assertions.assertEquals("USER", user2.getRole());

        verify(userRepository, times(1)).findUserByEmail(user2.getEmail());
        verify(passwordEncoder, times(1)).encode("12345678");
        verify(userRepository, times(1)).save(user2);
    }

    @Test
    public void addUserDuplicateEmailTest() {
        when(userRepository.findUserByEmail(user1.getEmail())).thenReturn(user1);

        ApiException exception = Assertions.assertThrows(ApiException.class, () -> userService.addUser(user1));
        Assertions.assertEquals("Email already exists", exception.getMessage());

        verify(userRepository, times(1)).findUserByEmail(user1.getEmail());
        verify(userRepository, never()).save(user1);
    }

    @Test
    public void updateUserTest() {
        when(userRepository.findUserById(user1.getId())).thenReturn(user1);
        when(userRepository.findUserByEmail(user2.getEmail())).thenReturn(null);
        when(passwordEncoder.encode("12345678")).thenReturn("{bcrypt}hashed");

        userService.updateUser(user1.getId(), user2);
        Assertions.assertEquals("Razan", user1.getName());
        Assertions.assertEquals("{bcrypt}hashed", user1.getPasswordHash());

        verify(userRepository, times(1)).findUserById(user1.getId());
        verify(userRepository, times(1)).save(user1);
    }

    @Test
    public void deleteUserTest() {
        when(userRepository.findUserById(user2.getId())).thenReturn(user2);

        userService.deleteUser(user2.getId());

        verify(userRepository, times(1)).findUserById(user2.getId());
        verify(userRepository, times(1)).delete(user2);
    }

    @Test
    public void deleteUserWithProjectsTest() {
        user1.getProjects().add(new Project());
        when(userRepository.findUserById(user1.getId())).thenReturn(user1);

        ApiException exception = Assertions.assertThrows(ApiException.class, () -> userService.deleteUser(user1.getId()));
        Assertions.assertEquals("Cannot delete user because they own projects", exception.getMessage());

        verify(userRepository, times(1)).findUserById(user1.getId());
        verify(userRepository, never()).delete(user1);
    }
}
