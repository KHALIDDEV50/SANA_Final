package com.example.sana;

import com.example.sana.API.ApiException;
import com.example.sana.Enums.ProjectStatus;
import com.example.sana.Model.Project;
import com.example.sana.Model.User;
import com.example.sana.Repository.AIInteractionRepository;
import com.example.sana.Repository.ChangeProposalItemRepository;
import com.example.sana.Repository.ChangeProposalRepository;
import com.example.sana.Repository.ProjectRepository;
import com.example.sana.Repository.ToolRunRepository;
import com.example.sana.Repository.UserRepository;
import com.example.sana.Service.Billing.BillingStore;
import com.example.sana.Service.ProjectService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ProjectServiceTest {

    @InjectMocks
    ProjectService projectService;
    @Mock
    ProjectRepository projectRepository;
    @Mock
    UserRepository userRepository;
    @Mock
    AIInteractionRepository aiInteractionRepository;
    @Mock
    ChangeProposalRepository changeProposalRepository;
    @Mock
    ChangeProposalItemRepository changeProposalItemRepository;
    @Mock
    ToolRunRepository toolRunRepository;
    @Mock
    BillingStore billingStore;

    User user;
    Project project1, project2, project3;
    List<Project> projects;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1);
        user.setName("Abdullah");
        user.setEmail("abdullah@gmail.com");
        user.setPasswordHash("12345678");
        user.setRole("USER");

        project1 = project(1, "Food delivery app", "TECH");
        project2 = project(2, "Coffee subscription", "BUSINESS");
        project3 = project(3, "The light flower", "CREATIVITY");

        projects = new ArrayList<>();
        projects.add(project1);
        projects.add(project2);
        projects.add(project3);
    }

    private Project project(Integer id, String name, String domain) {
        Project project = new Project();
        project.setId(id);
        project.setName(name);
        project.setDescription(name + " description");
        project.setDomain(domain);
        project.setStatus(ProjectStatus.DRAFT);
        project.setOwner(user);
        return project;
    }

    @Test
    public void getAllProjectsTest() {
        when(projectRepository.findAll()).thenReturn(projects);

        List<Project> projectList = projectService.getAllProjects();
        Assertions.assertEquals(3, projectList.size());

        verify(projectRepository, times(1)).findAll();
    }

    @Test
    public void getProjectByIdTest() {
        when(projectRepository.findProjectById(project1.getId())).thenReturn(project1);

        Project project = projectService.getProjectById(project1.getId());
        Assertions.assertEquals(project1, project);

        verify(projectRepository, times(1)).findProjectById(project1.getId());
    }

    @Test
    public void getProjectsByUserIdTest() {
        when(userRepository.findUserById(user.getId())).thenReturn(user);
        when(projectRepository.findAllByOwner_Id(user.getId())).thenReturn(projects);

        List<Project> projectList = projectService.getProjectsByUserId(user.getId());
        Assertions.assertEquals(projects, projectList);

        verify(userRepository, times(1)).findUserById(user.getId());
        verify(projectRepository, times(1)).findAllByOwner_Id(user.getId());
    }

    @Test
    public void addProjectTest() {
        when(userRepository.findUserById(user.getId())).thenReturn(user);

        projectService.addProject(user.getId(), project3);
        Assertions.assertEquals(user, project3.getOwner());
        Assertions.assertNotNull(project3.getCreatedAt());

        verify(userRepository, times(1)).findUserById(user.getId());
        verify(billingStore, times(1)).checkProjectQuota(user.getId());
        verify(projectRepository, times(1)).save(project3);
    }

    @Test
    public void addProjectUserNotFoundTest() {
        when(userRepository.findUserById(99)).thenReturn(null);

        ApiException exception = Assertions.assertThrows(ApiException.class, () -> projectService.addProject(99, project3));
        Assertions.assertEquals("User not found", exception.getMessage());

        verify(userRepository, times(1)).findUserById(99);
        verify(projectRepository, never()).save(project3);
    }

    @Test
    public void updateProjectTest() {
        when(projectRepository.findByIdForUpdate(project1.getId())).thenReturn(Optional.ofNullable(project1));

        projectService.updateProject(project1.getId(), project2);
        Assertions.assertEquals("Coffee subscription", project1.getName());

        verify(projectRepository, times(1)).findByIdForUpdate(project1.getId());
        verify(projectRepository, times(1)).save(project1);
    }

    @Test
    public void deleteProjectTest() {
        when(projectRepository.findProjectById(project1.getId())).thenReturn(project1);

        projectService.deleteProject(project1.getId());

        verify(projectRepository, times(1)).findProjectById(project1.getId());
        verify(changeProposalItemRepository, times(1)).deleteAllByProjectId(project1.getId());
        verify(changeProposalRepository, times(1)).deleteAllByProjectId(project1.getId());
        verify(aiInteractionRepository, times(1)).deleteAllByProjectId(project1.getId());
        verify(toolRunRepository, times(1)).deleteAllByProjectId(project1.getId());
        verify(projectRepository, times(1)).delete(project1);
    }
}
