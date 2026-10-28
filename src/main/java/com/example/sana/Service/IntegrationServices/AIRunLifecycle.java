package com.example.sana.Service.IntegrationServices;

import com.example.sana.Enums.*;
import com.example.sana.Model.AIInteraction;
import java.time.LocalDateTime;

public final class AIRunLifecycle {
    private AIRunLifecycle() {}
    public static boolean terminal(AIInteraction run) {
        return run.getStatus() == ExecutionStatus.COMPLETED || run.getStatus() == ExecutionStatus.FAILED || run.getStatus() == ExecutionStatus.TIMED_OUT;
    }
    public static void fail(AIInteraction run, ExecutionStatus status, String code, String safeMessage) {
        run.setStatus(status); run.setErrorCode(code); run.setErrorMessage(safeMessage);
        run.setCompletedAt(LocalDateTime.now()); run.setDispatchStatus(AIDispatchStatus.TERMINAL);
        run.setDispatchLeaseUntil(null); run.setNextDispatchAt(null);
    }
}
