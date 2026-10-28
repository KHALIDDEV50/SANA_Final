package com.example.sana.Service.IntegrationServices;

import com.example.sana.Config.N8nProperties;
import com.example.sana.Repository.AIInteractionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class AIDispatchWorker {
    private static final Logger log = LoggerFactory.getLogger(AIDispatchWorker.class);
    private final N8nProperties properties;
    private final AIInteractionRepository interactions;
    private final AIDispatchService state;
    private final N8nClient client;

    @Scheduled(fixedDelay = 1000)
    public void poll() {
        if (!properties.isEnabled() || !properties.isWorkerEnabled()) return;
        var page = PageRequest.of(0, properties.getBatchSize());
        for (Integer id : interactions.findExpired(LocalDateTime.now(), page)) safely(() -> state.expire(id));
        for (Integer id : interactions.findDispatchable(LocalDateTime.now(), page)) safely(() -> {
            var work = state.claim(id);
            if (work == null) return;
            try { state.acknowledge(id, client.dispatch(work.requestId(), work.requestJson())); }
            catch (N8nClient.DispatchFailure failure) { state.failed(id, failure); }
        });
    }

    private void safely(Runnable task) {
        try { task.run(); }
        catch (RuntimeException e) {
            // No exception text: HTTP exceptions can embed URLs, payloads, and headers.
            log.warn("AI dispatch state operation failed; its durable lease/deadline will recover it");
        }
    }
}
