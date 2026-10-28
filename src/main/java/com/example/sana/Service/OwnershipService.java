package com.example.sana.Service;

import com.example.sana.API.ApiException;
import com.example.sana.Model.CreativityDomainModels.CharacterProfile;
import com.example.sana.Model.CreativityDomainModels.Location;
import com.example.sana.Model.CreativityDomainModels.StoryEvent;
import com.example.sana.Model.CreativityDomainModels.World;
import com.example.sana.Model.CreativityDomainModels.WorldRule;
import com.example.sana.Model.Project;
import com.example.sana.Model.ProjectEdge;
import com.example.sana.Model.ProjectNode;
import com.example.sana.Model.ProjectOutput;
import com.example.sana.Model.ProjectRecord;
import com.example.sana.Model.ProjectUnderstanding;
import com.example.sana.Model.User;
import com.example.sana.Repository.CreativityDomainRepository.CharacterProfileRepository;
import com.example.sana.Repository.CreativityDomainRepository.LocationRepository;
import com.example.sana.Repository.CreativityDomainRepository.StoryEventRepository;
import com.example.sana.Repository.CreativityDomainRepository.WorldRepository;
import com.example.sana.Repository.CreativityDomainRepository.WorldRuleRepository;
import com.example.sana.Repository.ProjectEdgeRepository;
import com.example.sana.Repository.ProjectNodeRepository;
import com.example.sana.Repository.ProjectOutputRepository;
import com.example.sana.Repository.ProjectRecordRepository;
import com.example.sana.Repository.ProjectRepository;
import com.example.sana.Repository.ProjectUnderstandingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

// Checks that the data in a request belongs to the logged-in user.
// Controllers call one of these with @AuthenticationPrincipal User user before doing the work.
// Everything belongs to a project, so each check walks up to the project's owner. Admins may access everything.
@Service
@RequiredArgsConstructor
public class OwnershipService {

    private final ProjectRepository projectRepository;
    private final ProjectOutputRepository projectOutputRepository;
    private final ProjectNodeRepository projectNodeRepository;
    private final ProjectEdgeRepository projectEdgeRepository;
    private final ProjectRecordRepository projectRecordRepository;
    private final ProjectUnderstandingRepository projectUnderstandingRepository;
    private final CharacterProfileRepository characterProfileRepository;
    private final WorldRepository worldRepository;
    private final LocationRepository locationRepository;
    private final WorldRuleRepository worldRuleRepository;
    private final StoryEventRepository storyEventRepository;

    // The user can only act for themselves
    public void checkUser(User user, Integer userId) {

        if (!isAdmin(user) && !user.getId().equals(userId)) {
            throw new ApiException("You do not have access to this user");
        }
    }

    public void checkProject(User user, Integer projectId) {

        Project project = projectRepository.findProjectById(projectId);

        if (project == null) {
            throw new ApiException("Project not found");
        }

        checkOwner(user, project);
    }

    public void checkOutput(User user, Integer outputId) {

        ProjectOutput output = projectOutputRepository.findProjectOutputById(outputId);

        if (output == null) {
            throw new ApiException("Project output not found");
        }

        checkOwner(user, output.getProject());
    }

    public void checkNode(User user, Integer nodeId) {

        ProjectNode node = projectNodeRepository.findProjectNodeById(nodeId);

        if (node == null) {
            throw new ApiException("Project node not found");
        }

        checkNodeOwner(user, node);
    }

    public void checkEdge(User user, Integer edgeId) {

        ProjectEdge edge = projectEdgeRepository.findProjectEdgeById(edgeId);

        if (edge == null) {
            throw new ApiException("Project edge not found");
        }

        checkOwner(user, edge.getProjectOutput() == null ? null : edge.getProjectOutput().getProject());
    }

    public void checkRecord(User user, Integer recordId) {

        ProjectRecord record = projectRecordRepository.findProjectRecordById(recordId);

        if (record == null) {
            throw new ApiException("Project record not found");
        }

        checkOwner(user, record.getProject());
    }

    public void checkUnderstanding(User user, Integer understandingId) {

        ProjectUnderstanding understanding = projectUnderstandingRepository.findProjectUnderstandingById(understandingId);

        if (understanding == null) {
            throw new ApiException("Project understanding not found");
        }

        checkOwner(user, understanding.getProject());
    }

    public void checkCharacterProfile(User user, Integer characterProfileId) {

        CharacterProfile character = characterProfileRepository.findCharacterProfileById(characterProfileId);

        if (character == null) {
            throw new ApiException("Character profile not found");
        }

        checkNodeOwner(user, character.getProjectNode());
    }

    public void checkWorld(User user, Integer worldId) {

        World world = worldRepository.findWorldById(worldId);

        if (world == null) {
            throw new ApiException("World not found");
        }

        checkNodeOwner(user, world.getProjectNode());
    }

    public void checkLocation(User user, Integer locationId) {

        Location location = locationRepository.findLocationById(locationId);

        if (location == null) {
            throw new ApiException("Location not found");
        }

        checkNodeOwner(user, location.getProjectNode());
    }

    public void checkWorldRule(User user, Integer worldRuleId) {

        WorldRule worldRule = worldRuleRepository.findWorldRuleById(worldRuleId);

        if (worldRule == null) {
            throw new ApiException("World rule not found");
        }

        checkNodeOwner(user, worldRule.getProjectNode());
    }

    public void checkStoryEvent(User user, Integer storyEventId) {

        StoryEvent storyEvent = storyEventRepository.findStoryEventById(storyEventId);

        if (storyEvent == null) {
            throw new ApiException("Story event not found");
        }

        checkNodeOwner(user, storyEvent.getProjectNode());
    }

    private void checkNodeOwner(User user, ProjectNode node) {

        boolean hasProject = node != null && node.getProjectOutput() != null;

        checkOwner(user, hasProject ? node.getProjectOutput().getProject() : null);
    }

    private void checkOwner(User user, Project project) {

        if (isAdmin(user)) {
            return;
        }

        if (project == null || project.getOwner() == null || !project.getOwner().getId().equals(user.getId())) {
            throw new ApiException("You do not have access to this project");
        }
    }

    private boolean isAdmin(User user) {
        return "ADMIN".equals(user.getRole());
    }
}
