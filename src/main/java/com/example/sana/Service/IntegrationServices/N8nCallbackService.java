package com.example.sana.Service.IntegrationServices;

import com.example.sana.API.*;
import com.example.sana.DTO.IntegrationDTOs.N8nCallbackDTO;
import com.example.sana.DTO.AIDTOs.AIWorkflowRequestDTO;
import com.example.sana.Enums.*;
import com.example.sana.Model.*;
import com.example.sana.Repository.*;
import com.example.sana.Service.DiagramServices.DiagramRules;
import com.example.sana.Service.DiagramServices.DiagramRulesRegistry;
import com.example.sana.Service.DiagramServices.GraphDiff;
import com.example.sana.Service.DiagramServices.GraphRuleChecks;
import com.example.sana.Service.DiagramServices.GraphValidator;
import com.example.sana.Service.Business.BusinessResponseValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class N8nCallbackService {
    private final AIInteractionRepository interactions;
    private final ChangeProposalRepository proposals;
    private final ChangeProposalItemRepository items;
    private final GraphValidator validator;
    private final GraphDiff diff;
    private final DiagramRulesRegistry diagramRulesRegistry;
    private final BusinessResponseValidator businessValidator;
    private final AIJson json;

    // false means a late callback was rejected after preserving a timed-out state.
    @Transactional
    public boolean receive(N8nCallbackDTO callback) {
        validateEnvelope(callback);
        AIInteraction run = interactions.findByRequestIdForUpdate(callback.requestId())
                .orElseThrow(() -> new ApiException("AI request not found"));
        if (run.getRequestJson() == null) throw new ApiException("Request has no authoritative snapshot");
        var request = json.read(run.getRequestJson(), AIWorkflowRequestDTO.class);
        if (!run.getRequestId().equals(request.getRequestId()) || !run.getProject().getId().equals(request.getProjectId())
                || !Objects.equals(run.getOutputId(), request.getOutputId()) || !Objects.equals(run.getBaseRevision(), request.getBaseRevision())
                || !run.getActionType().equals(request.getTask().name())) throw new ApiException("AI request metadata is inconsistent");
        // A result that breaks the diagram rules is the AI's mistake, not a broken callback. Record it as a failed run
        // with the reason right away, so the user does not wait for the timeout and can ask the AI to fix it.
        String invalidResult = null;
        try {
            validateResult(callback, request);
        } catch (ApiException e) {
            if (callback.status() != N8nCallbackDTO.CallbackStatus.COMPLETED) throw e;
            invalidResult = e.getMessage();
        }
        String hash = json.hash(callback);
        if (AIRunLifecycle.terminal(run)) {
            if (hash.equals(run.getCallbackHash())) return true;
            if (run.getStatus() == ExecutionStatus.TIMED_OUT) return false;
            throw new ApiConflictException("A different terminal result is already recorded");
        }
        if (run.getExecutionId() != null && !run.getExecutionId().equals(callback.executionId()))
            throw new ApiConflictException("Execution ID does not match the stored request");
        if (run.getDeadlineAt() != null && LocalDateTime.now().isAfter(run.getDeadlineAt())) {
            AIRunLifecycle.fail(run, ExecutionStatus.TIMED_OUT, "CALLBACK_TIMEOUT", "AI request timed out. Submit a new request to retry.");
            return false;
        }
        run.setExecutionId(callback.executionId());
        run.setCallbackHash(hash);
        run.setCompletedAt(LocalDateTime.now());
        run.setDispatchStatus(AIDispatchStatus.TERMINAL);
        run.setDispatchLeaseUntil(null); run.setNextDispatchAt(null);
        if (invalidResult != null) {
            String reason = "The AI result broke a diagram rule: " + invalidResult;
            AIRunLifecycle.fail(run, ExecutionStatus.FAILED, "INVALID_PROPOSAL", reason.length() > 500 ? reason.substring(0, 500) : reason);
        } else if (callback.status() == N8nCallbackDTO.CallbackStatus.FAILED) {
            // Raw provider/tool error text can contain credentials; do not persist or expose it.
            String code = Set.of("INVALID_AI_RESULT", "AI_EXECUTION_FAILED", "WORKFLOW_FAILED", "MODEL_UNAVAILABLE", "TOOL_FAILED", "EXECUTION_TIMEOUT").contains(callback.error().code())
                    ? callback.error().code() : "WORKFLOW_FAILED";
            AIRunLifecycle.fail(run, ExecutionStatus.FAILED, code, "AI workflow failed. Submit a new request to retry.");
        } else {
            run.setStatus(ExecutionStatus.COMPLETED);
            run.setModelName(callback.result().modelName());
            run.setResponseJson(json.write(callback.result()));
            if (callback.result().type() == N8nCallbackDTO.ResultType.PROPOSAL) storeProposal(run, request, callback.result().proposal());
        }
        return true;
    }

    private void validateEnvelope(N8nCallbackDTO callback) {
        if (callback == null || !Integer.valueOf(1).equals(callback.schemaVersion()) || callback.status() == null)
            throw new ApiException("Unsupported callback schema or status");
        try { if (!UUID.fromString(callback.requestId()).toString().equals(callback.requestId())) throw new IllegalArgumentException(); }
        catch (RuntimeException e) { throw new ApiException("Callback requestId must be a canonical UUID"); }
        GraphRuleChecks.requireText(callback.executionId(), 255, "Execution ID");
        if (callback.status() == N8nCallbackDTO.CallbackStatus.COMPLETED) {
            if (callback.result() == null || callback.error() != null) throw new ApiException("COMPLETED requires result and excludes error");
        } else {
            if (callback.error() == null || callback.result() != null) throw new ApiException("FAILED requires error and excludes result");
            GraphRuleChecks.requireText(callback.error().message(), 2000, "Failure message");
            if (callback.error().code() == null || !callback.error().code().matches("[A-Z][A-Z0-9_]{0,63}")) throw new ApiException("Invalid failure code");
            if (callback.error().details() != null) {
                if (callback.error().details().size() > 20) throw new ApiException("Too many failure details");
                callback.error().details().forEach(detail -> GraphRuleChecks.requireText(detail, 1000, "Failure detail"));
            }
        }
    }

    private void validateResult(N8nCallbackDTO callback, AIWorkflowRequestDTO request) {
        if (callback.status() == N8nCallbackDTO.CallbackStatus.FAILED) return;
        var result = callback.result();
        if (result.type() == null) throw new ApiException("Result type is required");
        GraphRuleChecks.requireText(result.message(), 20000, "AI message");
        GraphRuleChecks.requireText(result.modelName(), 255, "Model name");
        switch (result.type()) {
            case ADVICE -> {
                if (result.proposal() != null || result.clarification() != null) throw new ApiException("ADVICE excludes proposal and clarification");
                // Only Business tool runs promise a structured report; plain Business advice is free text
                Object tool = request.getContext().get("businessToolType");
                if ("BUSINESS".equals(request.getDomain()) && tool != null) {
                    try { businessValidator.validate(result.message(), BusinessToolType.valueOf(String.valueOf(tool))); }
                    catch (RuntimeException e) { throw new ApiException("Invalid structured business analysis result"); }
                }
            }
            case CLARIFICATION -> {
                if (result.proposal() != null || result.clarification() == null || result.clarification().suggestedAnswers() == null)
                    throw new ApiException("CLARIFICATION requires a question and suggestedAnswers array");
                GraphRuleChecks.requireText(result.clarification().question(), 2000, "Clarification question");
                if (result.clarification().suggestedAnswers().size() > 10) throw new ApiException("Too many suggested answers");
                result.clarification().suggestedAnswers().forEach(answer -> GraphRuleChecks.requireText(answer, 255, "Suggested answer"));
            }
            case PROPOSAL -> {
                // Proposals are only allowed for CREATE/EDIT on a diagram type that has rules
                DiagramRules rules = diagramRulesRegistry.findRules(request.getDomain(), request.getDiagramType());
                if (rules == null || request.getTask() == AITask.ADVISE) {
                    throw new ApiException("This request does not permit graph proposals");
                }
                if (result.clarification() != null || result.proposal() == null) throw new ApiException("PROPOSAL requires a graph and excludes clarification");
                GraphRuleChecks.requireText(result.proposal().title(), 255, "Proposal title");
                GraphRuleChecks.requireText(result.proposal().description(), 10000, "Proposal description");
                String title = request.getOutputId() == null ? result.proposal().title() : String.valueOf(request.getCurrentGraph().getProperties().get("title"));
                validator.validate(result.proposal().proposedGraph(), request.getOutputId() == null ? null : request.getCurrentGraph(), title, rules);
            }
        }
    }

    private void storeProposal(AIInteraction run, AIWorkflowRequestDTO request, N8nCallbackDTO.Proposal candidate) {
        var proposal = new ChangeProposal();
        proposal.setManagedRequestId(run.getRequestId()); proposal.setTitle(candidate.title()); proposal.setDescription(candidate.description());
        proposal.setStatus(ProposalStatus.PENDING); proposal.setBaseVersion(run.getProject().getVersion());
        proposal.setBaseRevision(run.getBaseRevision()); proposal.setOutputId(run.getOutputId());
        proposal.setProject(run.getProject()); proposal.setAiInteraction(run); proposal.setProposedGraphJson(json.write(candidate.proposedGraph()));
        proposals.save(proposal);
        for (var change : diff.compute(request.getOutputId() == null ? null : request.getCurrentGraph(), candidate.proposedGraph(), run.getOutputId())) {
            change.setChangeProposal(proposal); items.save(change);
        }
        run.setProposalId(proposal.getId());
    }
}
