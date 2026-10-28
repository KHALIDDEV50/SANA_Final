package com.example.sana.Service.IntegrationServices;

import com.example.sana.Config.N8nProperties;
import com.example.sana.DTO.IntegrationDTOs.N8nAcknowledgementDTO;
import com.example.sana.Enums.*;
import com.example.sana.Model.AIInteraction;
import com.example.sana.Repository.AIInteractionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AIDispatchService {
    private final AIInteractionRepository interactions;
    private final N8nProperties properties;

    public record Work(Integer id, String requestId, String requestJson) {}

    @Transactional
    public Work claim(Integer id) {
        AIInteraction run = interactions.findByIdForUpdate(id).orElse(null);
        LocalDateTime now = LocalDateTime.now();
        if (run == null || run.getStatus() != ExecutionStatus.PENDING || run.getDispatchStatus() != AIDispatchStatus.QUEUED
                || run.getNextDispatchAt() == null || run.getNextDispatchAt().isAfter(now)) return null;
        if (run.getDeadlineAt() == null || !run.getDeadlineAt().isAfter(now)) {
            AIRunLifecycle.fail(run, ExecutionStatus.TIMED_OUT, "CALLBACK_TIMEOUT", "AI request timed out. Submit a new request to retry."); return null;
        }
        if (run.getDispatchAttempts() >= properties.getMaxDispatchAttempts()) {
            AIRunLifecycle.fail(run, ExecutionStatus.FAILED, "DISPATCH_FAILED", "AI workflow could not be reached. Submit a new request to retry."); return null;
        }
        run.setDispatchAttempts(run.getDispatchAttempts() + 1);
        run.setDispatchStatus(AIDispatchStatus.IN_FLIGHT);
        run.setDispatchLeaseUntil(now.plusSeconds(properties.getConnectTimeoutSeconds() + properties.getReadTimeoutSeconds() + 15));
        return new Work(run.getId(), run.getRequestId(), run.getRequestJson());
    }

    @Transactional
    public void acknowledge(Integer id, N8nAcknowledgementDTO ack) {
        AIInteraction run = interactions.findByIdForUpdate(id).orElseThrow();
        if (AIRunLifecycle.terminal(run)) return; // Fast callback has already won.
        if (run.getExecutionId() != null && !run.getExecutionId().equals(ack.executionId())) {
            run.setDispatchStatus(AIDispatchStatus.UNCERTAIN); return;
        }
        run.setExecutionId(ack.executionId()); run.setAcceptedAt(LocalDateTime.now());
        run.setStatus(ExecutionStatus.RUNNING); run.setDispatchStatus(AIDispatchStatus.ACKNOWLEDGED);
        run.setDispatchLeaseUntil(null); run.setNextDispatchAt(null);
    }

    @Transactional
    public void failed(Integer id, N8nClient.DispatchFailure failure) {
        AIInteraction run = interactions.findByIdForUpdate(id).orElseThrow();
        if (AIRunLifecycle.terminal(run)) return;
        run.setDispatchLeaseUntil(null);
        if (Boolean.TRUE.equals(failure.retrySafe) && run.getDispatchAttempts() < properties.getMaxDispatchAttempts()) {
            run.setDispatchStatus(AIDispatchStatus.QUEUED);
            run.setNextDispatchAt(LocalDateTime.now().plusSeconds(properties.getRetryDelaySeconds()));
        } else if (failure.retrySafe == null) {
            run.setStatus(ExecutionStatus.RUNNING); run.setDispatchStatus(AIDispatchStatus.UNCERTAIN); run.setNextDispatchAt(null);
        } else {
            AIRunLifecycle.fail(run, ExecutionStatus.FAILED, failure.code, "AI workflow could not accept the request. Submit a new request to retry.");
        }
    }

    @Transactional
    public void expire(Integer id) {
        AIInteraction run = interactions.findByIdForUpdate(id).orElse(null);
        if (run == null || AIRunLifecycle.terminal(run)) return;
        LocalDateTime now = LocalDateTime.now();
        if (run.getDeadlineAt() != null && !run.getDeadlineAt().isAfter(now)) {
            AIRunLifecycle.fail(run, ExecutionStatus.TIMED_OUT, "CALLBACK_TIMEOUT", "AI request timed out. Submit a new request to retry.");
        } else if (run.getDispatchStatus() == AIDispatchStatus.IN_FLIGHT && run.getDispatchLeaseUntil() != null && !run.getDispatchLeaseUntil().isAfter(now)) {
            // A process may have died after sending. An expired lease is NOT permission to resend.
            run.setStatus(ExecutionStatus.RUNNING); run.setDispatchStatus(AIDispatchStatus.UNCERTAIN); run.setDispatchLeaseUntil(null);
        }
    }
}
