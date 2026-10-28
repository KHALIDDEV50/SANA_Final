package com.example.sana;

import com.example.sana.API.ApiException;
import com.example.sana.Model.Project;
import com.example.sana.Model.User;
import com.example.sana.Repository.ProjectRepository;
import com.example.sana.Service.OwnershipService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class OwnershipServiceTest {

    @InjectMocks
    OwnershipService ownershipService;
    @Mock
    ProjectRepository projectRepository;

    User owner, otherUser, admin;
    Project project;

    @BeforeEach
    void setUp() {
        owner = user(1, "USER");
        otherUser = user(2, "USER");
        admin = user(3, "ADMIN");

        project = new Project();
        project.setId(10);
        project.setName("Food delivery app");
        project.setOwner(owner);
    }

    private User user(Integer id, String role) {
        User user = new User();
        user.setId(id);
        user.setEmail("user" + id + "@gmail.com");
        user.setRole(role);
        return user;
    }

    @Test
    public void checkProjectOwnerTest() {
        when(projectRepository.findProjectById(project.getId())).thenReturn(project);

        Assertions.assertDoesNotThrow(() -> ownershipService.checkProject(owner, project.getId()));

        verify(projectRepository, times(1)).findProjectById(project.getId());
    }

    @Test
    public void checkProjectOtherUserTest() {
        when(projectRepository.findProjectById(project.getId())).thenReturn(project);

        ApiException exception = Assertions.assertThrows(ApiException.class, () -> ownershipService.checkProject(otherUser, project.getId()));
        Assertions.assertEquals("You do not have access to this project", exception.getMessage());

        verify(projectRepository, times(1)).findProjectById(project.getId());
    }

    @Test
    public void checkProjectAdminTest() {
        when(projectRepository.findProjectById(project.getId())).thenReturn(project);

        Assertions.assertDoesNotThrow(() -> ownershipService.checkProject(admin, project.getId()));

        verify(projectRepository, times(1)).findProjectById(project.getId());
    }

    @Test
    public void checkProjectNotFoundTest() {
        when(projectRepository.findProjectById(99)).thenReturn(null);

        ApiException exception = Assertions.assertThrows(ApiException.class, () -> ownershipService.checkProject(owner, 99));
        Assertions.assertEquals("Project not found", exception.getMessage());

        verify(projectRepository, times(1)).findProjectById(99);
    }

    @Test
    public void checkUserTest() {
        Assertions.assertDoesNotThrow(() -> ownershipService.checkUser(owner, owner.getId()));

        ApiException exception = Assertions.assertThrows(ApiException.class, () -> ownershipService.checkUser(otherUser, owner.getId()));
        Assertions.assertEquals("You do not have access to this user", exception.getMessage());
    }
}
