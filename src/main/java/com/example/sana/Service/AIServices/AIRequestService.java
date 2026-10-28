package com.example.sana.Service.AIServices;

import com.example.sana.API.ApiConflictException;
import com.example.sana.API.ApiException;
import com.example.sana.Config.N8nProperties;
import com.example.sana.DTO.AIDTOs.AIRequestDTO;
import com.example.sana.DTO.AIDTOs.AIRunResponseDTO;
import com.example.sana.DTO.AIDTOs.AIWorkflowRequestDTO;
import com.example.sana.DTO.GraphDefinitionDTO;
import com.example.sana.DTO.IntegrationDTOs.N8nCallbackDTO;
import com.example.sana.Enums.AIDispatchStatus;
import com.example.sana.Enums.AITask;
import com.example.sana.Enums.ExecutionStatus;
import com.example.sana.Enums.ProposalStatus;
import com.example.sana.Model.AIInteraction;
import com.example.sana.Model.Project;
import com.example.sana.Model.ProjectOutput;
import com.example.sana.Model.ProjectUnderstanding;
import com.example.sana.Model.User;
import com.example.sana.Repository.AIInteractionRepository;
import com.example.sana.Repository.ChangeProposalRepository;
import com.example.sana.Repository.ProjectOutputRepository;
import com.example.sana.Repository.ProjectRepository;
import com.example.sana.Repository.ProjectUnderstandingRepository;
import com.example.sana.Service.DiagramServices.DiagramRules;
import com.example.sana.Service.DiagramServices.DiagramRulesRegistry;
import com.example.sana.Service.DiagramServices.DiagramGraphService;
import com.example.sana.Service.DiagramServices.GraphValidator;
import com.example.sana.Service.IntegrationServices.AIJson;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.json.JsonMapper;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

// Prepares and stores AI requests for every domain. The dispatch worker later sends them to n8n.
@Service
@RequiredArgsConstructor
public class AIRequestService {

    // Domains that have an agent in the n8n workflow
    private static final List<String> DOMAINS = List.of("EDUCATION", "BUSINESS", "TECH", "CREATIVITY");

    private final AIInteractionRepository aiInteractionRepository;
    private final ProjectRepository projectRepository;
    private final ProjectOutputRepository projectOutputRepository;
    private final ProjectUnderstandingRepository projectUnderstandingRepository;
    private final ChangeProposalRepository proposalRepository;
    private final DiagramGraphService diagramGraphService;
    private final GraphValidator graphValidator;
    private final DiagramRulesRegistry diagramRulesRegistry;
    private final N8nProperties n8nProperties;
    private final JsonMapper jsonMapper;
    private final AIJson aiJson;

    // Start an AI request for the project's own domain.
    // Lock order: Project -> ProjectOutput. No external calls occur in this transaction.
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public AIRunResponseDTO start(Integer projectId, AIRequestDTO request) {
        return prepare(projectId, request, null, Map.of());
    }

    /** A project-scoped moodboard CREATE task, distinct from diagram CREATE. */
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public AIRunResponseDTO startMoodboardPlan(Integer projectId, String prompt) {
        AIRequestDTO request = new AIRequestDTO();
        request.setTask(AITask.CREATE);
        request.setDiagramType("MOODBOARD_PLAN");
        request.setPrompt(prompt);
        request.setSelectedElementIds(List.of());
        return prepare(projectId, request, "CREATIVITY", Map.of());
    }

    // Start a structured Business tool analysis (advice only until the Business diagrams are added)
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public AIRunResponseDTO startBusiness(Integer projectId, AIRequestDTO request, Map<String, Object> businessContext) {

        if (!n8nProperties.isBusinessEnabled()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Business n8n workflow is not enabled yet");
        }

        if (request.getTask() != AITask.ADVISE) {
            throw new ApiException("Business migration currently supports advice only");
        }

        return prepare(projectId, request, "BUSINESS", businessContext);
    }

    // requiredDomain is null when any project domain is accepted
    private AIRunResponseDTO prepare(Integer projectId, AIRequestDTO request, String requiredDomain, Map<String, Object> extraContext) {

        if (!n8nProperties.isEnabled()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "n8n integration is disabled");
        }

        validateRequest(request);

        if (projectId == null || projectId <= 0) {
            throw new ApiException("Valid project ID is required");
        }

        Project project = projectRepository.findByIdForUpdate(projectId)
                .orElseThrow(() -> new ApiException("Project not found"));

        User user = project.getOwner();
        if (user == null) {
            throw new ApiException("Project owner is required to record the AI run");
        }

        // The domain always comes from the stored project, never from the client
        String domain = project.getDomain();
        if (domain == null || !DOMAINS.contains(domain)) {
            throw new ApiException("Project domain must be one of " + DOMAINS);
        }
        if (requiredDomain != null && !requiredDomain.equals(domain)) {
            throw new ApiException("Request domain does not match the project");
        }

        boolean moodboardPlan = request.getTask() == AITask.CREATE
                && "MOODBOARD_PLAN".equals(request.getDiagramType())
                && request.getOutputId() == null && request.getBaseRevision() == null
                && "CREATIVITY".equals(domain);
        GraphDefinitionDTO graph = new GraphDefinitionDTO(List.of(), List.of(), new LinkedHashMap<>());
        String diagramType;
        Integer revision = null;
        DiagramRules rules;
        List<String> selected = request.getSelectedElementIds() == null ? List.of() : List.copyOf(request.getSelectedElementIds());

        if (moodboardPlan) {
            // CREATE the moodboard preview plan, not a diagram or graph proposal.
            // The shared n8n contract accepts CREATE with null outputId/baseRevision.
            diagramType = "MOODBOARD_PLAN";
            rules = null;
        } else if (request.getTask() == AITask.CREATE) {

            diagramType = request.getDiagramType();

            // Older TECH clients do not send a diagram type
            if (diagramType == null && "TECH".equals(domain)) {
                diagramType = "ARCHITECTURE";
            }
            if (diagramType == null) {
                throw new ApiException("diagramType is required to create a diagram");
            }

            // Creating a diagram always needs its rules
            rules = diagramRulesRegistry.getRules(domain, diagramType);

        } else {

            ProjectOutput output = projectOutputRepository.findByIdForUpdate(request.getOutputId())
                    .orElseThrow(() -> new ApiException("Project output not found"));

            if (output.getProject() == null || !projectId.equals(output.getProject().getId())) {
                throw new ApiException("Output does not belong to this project");
            }
            if (output.getGraphRevision() == null) {
                throw new ApiException("Graph revision migration is required");
            }
            if (request.getBaseRevision() != null && !request.getBaseRevision().equals(output.getGraphRevision())) {
                throw new ApiConflictException("Diagram changed. Reload the graph before requesting AI assistance");
            }

            diagramType = output.getOutputType();
            revision = output.getGraphRevision();
            graph = diagramGraphService.read(output);

            // EDIT needs rules and a valid starting graph; ADVISE works on any graph diagram
            if (request.getTask() == AITask.EDIT) {
                rules = diagramRulesRegistry.getRules(domain, diagramType);
                graphValidator.validate(graph, graph, output.getTitle(), rules);
            } else {
                rules = diagramRulesRegistry.findRules(domain, diagramType);
            }

            Set<String> available = new HashSet<>();
            graph.getNodes().forEach(node -> available.add(node.getKey()));
            graph.getEdges().forEach(edge -> available.add(edge.getKey()));
            if (!available.containsAll(selected)) {
                throw new ApiException("Selected elements must belong to this diagram");
            }
        }

        if (request.getDiagramType() != null && !diagramType.equals(request.getDiagramType())) {
            throw new ApiException("Diagram type does not match the selected output");
        }

        // n8n agents and the Build Callback Payload node read allowedNodeTypes/allowedEdgeTypes
        Map<String, Object> context = buildContext(project);
        if (rules != null) {
            context.put("allowedNodeTypes", rules.getNodeTypes());
            context.put("allowedEdgeTypes", rules.getEdgeTypes());
            context.put("graphRulesVersion", 1);
            context.put("graphRules", rules.getRulesSummary());
        }
        if (!moodboardPlan) context.putAll(diagramGraphService.extraContext(project, diagramType));
        context.putAll(extraContext);

        String requestId = UUID.randomUUID().toString();
        AIWorkflowRequestDTO workflowRequest = new AIWorkflowRequestDTO(1, requestId, domain, request.getTask(), projectId,
                request.getOutputId(), diagramType, revision, request.getPrompt().trim(), selected,
                context, List.of(), graph);

        String storedRequest = jsonMapper.writeValueAsString(workflowRequest);
        if (storedRequest.getBytes(StandardCharsets.UTF_8).length > n8nProperties.getMaxRequestBytes()) {
            throw new ApiException("AI request context is too large");
        }

        AIInteraction run = new AIInteraction();
        run.setRequestId(requestId);
        run.setRequestJson(storedRequest);
        run.setActionType(request.getTask().name());
        run.setPromptSummary(workflowRequest.getPrompt());
        run.setOutputId(request.getOutputId());
        run.setBaseRevision(revision);
        run.setStatus(ExecutionStatus.PENDING);
        run.setDispatchStatus(AIDispatchStatus.QUEUED);
        run.setDispatchAttempts(0);
        run.setNextDispatchAt(LocalDateTime.now());
        run.setDeadlineAt(LocalDateTime.now().plusSeconds(n8nProperties.getRequestTimeoutSeconds()));
        run.setProject(project);
        run.setUser(user);
        // modelName and responseJson remain null until a real execution produces them.

        return toResponse(aiInteractionRepository.save(run));
    }

    // Runs and reports already belong to Project and User in the existing ERD.
    // Return DTOs so the frontend can restore pending runs and saved results.
    @Transactional(readOnly = true)
    public List<AIRunResponseDTO> listRuns(Integer projectId) {
        if (projectId == null || projectId <= 0) {
            throw new ApiException("Valid project ID is required");
        }
        projectRepository.findById(projectId)
                .orElseThrow(() -> new ApiException("Project not found"));
        return aiInteractionRepository
                .findTop50ByProject_IdAndRequestIdIsNotNullOrderByIdDesc(projectId)
                .stream().map(run -> toResponse(run, false)).toList();
    }

    // The AI panel's chat history: the latest 50 requests with their prompts and answers.
    // Proposal graphs and Business inputs are left out; the pending proposal is loaded on its own.
    @Transactional(readOnly = true)
    public List<AIRunResponseDTO> listChat(Integer projectId) {
        if (projectId == null || projectId <= 0) {
            throw new ApiException("Valid project ID is required");
        }
        projectRepository.findById(projectId)
                .orElseThrow(() -> new ApiException("Project not found"));
        return aiInteractionRepository
                .findTop50ByProject_IdAndRequestIdIsNotNullOrderByIdDesc(projectId)
                .stream().map(run -> {
                    AIRunResponseDTO response = toResponse(run, true);
                    N8nCallbackDTO.Result result = response.getResult();
                    if (result != null && result.proposal() != null) {
                        response.setResult(new N8nCallbackDTO.Result(result.type(), result.message(), result.modelName(),
                                result.clarification(), null));
                    }
                    response.setBusinessInput(null);
                    return response;
                }).toList();
    }

    // Get the current state of one AI request
    @Transactional(readOnly = true)
    public AIRunResponseDTO getRun(Integer projectId, UUID requestId) {

        if (projectId == null || projectId <= 0 || requestId == null) {
            throw new ApiException("Project and request IDs are required");
        }

        projectRepository.findById(projectId)
                .orElseThrow(() -> new ApiException("Project not found"));

        AIInteraction run = aiInteractionRepository.findByRequestIdAndProject_Id(requestId.toString(), projectId)
                .orElseThrow(() -> new ApiException("AI run not found"));

        return toResponse(run);
    }

    // Check the request fields that do not need the database
    private void validateRequest(AIRequestDTO request) {

        if (request == null || request.getTask() == null || request.getPrompt() == null
                || request.getPrompt().isBlank() || request.getPrompt().length() > 10000) {
            throw new ApiException("A valid AI task and prompt are required");
        }

        List<String> selected = request.getSelectedElementIds() == null ? List.of() : request.getSelectedElementIds();
        if (selected.size() > 100 || selected.stream().anyMatch(key -> key == null || !key.matches("(node|edge):[1-9][0-9]*"))
                || new HashSet<>(selected).size() != selected.size()) {
            throw new ApiException("Select at most 100 unique node:<id> or edge:<id> elements");
        }

        if (request.getTask() == AITask.CREATE) {
            if (request.getOutputId() != null || request.getBaseRevision() != null || !selected.isEmpty()) {
                throw new ApiException("CREATE must not target an existing output, revision, or selection");
            }
        } else if (request.getOutputId() == null || request.getOutputId() <= 0
                || (request.getBaseRevision() != null && request.getBaseRevision() < 0)) {
            throw new ApiException("EDIT and ADVISE require an output ID and, if supplied, a non-negative base revision");
        }
    }

    // Project facts sent to the agent. AIProposalService compares them again before accepting a proposal.
    public Map<String, Object> buildContext(Project project) {

        Map<String, Object> context = new LinkedHashMap<>();
        context.put("name", project.getName());
        context.put("description", project.getDescription());

        ProjectUnderstanding understanding = projectUnderstandingRepository.findProjectUnderstandingByProject_Id(project.getId());
        if (understanding != null) {
            context.put("goal", understanding.getProjectGoal());
            context.put("actors", readContextJson(understanding.getActorsJson(), "actors"));
            context.put("components", readContextJson(understanding.getComponentsJson(), "components"));
            context.put("constraints", readContextJson(understanding.getConstraintsJson(), "constraints"));
        }

        return context;
    }

    private Object readContextJson(String value, String field) {

        if (value == null || value.isBlank()) {
            return null;
        }

        try {
            return jsonMapper.readerFor(Object.class)
                    .with(DeserializationFeature.FAIL_ON_TRAILING_TOKENS, DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS)
                    .readValue(value);
        } catch (JacksonException e) {
            throw new ApiException("Stored project " + field + " contains invalid JSON");
        }
    }

    private AIRunResponseDTO toResponse(AIInteraction run) {
        return toResponse(run, true);
    }

    // History lists omit report/graph bodies; selecting an entry loads the full run.
    private AIRunResponseDTO toResponse(AIInteraction run, boolean includeResult) {

        AIRunResponseDTO response = new AIRunResponseDTO(run.getId(), run.getRequestId(), run.getProject().getId(),
                run.getOutputId(), run.getBaseRevision(), run.getActionType(), run.getStatus(), run.getModelName(), run.getCreatedAt());

        response.setStatusUrl("/api/projects/" + run.getProject().getId() + "/ai/requests/" + run.getRequestId());
        response.setPrompt(includeResult ? run.getPromptSummary() : null);
        if (run.getRequestJson() != null) {
            AIWorkflowRequestDTO snapshot = aiJson.read(run.getRequestJson(), AIWorkflowRequestDTO.class);
            response.setDiagramType(snapshot.getDiagramType());
            if ("BUSINESS".equals(snapshot.getDomain()) && snapshot.getContext() != null) {
                Object tool = snapshot.getContext().get("businessToolType");
                response.setBusinessToolType(tool == null ? null : tool.toString());
                if (includeResult) response.setBusinessInput(snapshot.getContext().get("businessInput"));
            }
        }
        response.setExecutionId(run.getExecutionId());
        response.setDispatchStatus(run.getDispatchStatus());
        response.setAcceptedAt(run.getAcceptedAt());
        response.setCompletedAt(run.getCompletedAt());
        response.setDeadlineAt(run.getDeadlineAt());
        response.setProposalId(run.getProposalId());

        // The database stores APPROVED, the API shows ACCEPTED
        if (run.getProposalId() != null) {
            proposalRepository.findById(run.getProposalId()).ifPresent(proposal ->
                    response.setProposalReviewStatus(proposal.getStatus() == ProposalStatus.APPROVED ? "ACCEPTED" : proposal.getStatus().name()));
        }

        if (includeResult && run.getStatus() == ExecutionStatus.COMPLETED && run.getResponseJson() != null) {
            response.setResult(aiJson.read(run.getResponseJson(), N8nCallbackDTO.Result.class));
        }

        if (run.getErrorCode() != null && (run.getStatus() == ExecutionStatus.FAILED || run.getStatus() == ExecutionStatus.TIMED_OUT)) {
            response.setError(new N8nCallbackDTO.Failure(run.getErrorCode(), run.getErrorMessage(), List.of()));
        }

        return response;
    }
}
