package com.app.backend.messaging;

import com.app.backend.config.RabbitConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.List;
import java.util.UUID;

/**
 * Publishes one image-processing job per photo — but only <em>after</em> the surrounding transaction
 * commits, so a rolled-back save never triggers processing. This is best-effort by design: a lost
 * publish leaves the photo serving at full resolution (the frontend falls back to the original), which
 * is cosmetic, not data loss. Re-run via the reprocess endpoint if needed.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ImageProcessingPublisher {

    private final RabbitTemplate rabbitTemplate;

    public void enqueue(UUID propertyId, List<String> photoUrls) {
        if (photoUrls.isEmpty()) return;
        List<String> urls = List.copyOf(photoUrls);
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                // Best-effort: the save already committed, so a broker outage must not fail the request.
                // A dropped publish just leaves the photo at full resolution until it is reprocessed.
                try {
                    for (String url : urls) {
                        rabbitTemplate.convertAndSend(RabbitConfig.EXCHANGE, RabbitConfig.ROUTING_KEY,
                                new ImageProcessingMessage(propertyId, url));
                    }
                } catch (Exception e) {
                    log.warn("Failed to publish image processing jobs for property {}: {}",
                            propertyId, e.getMessage());
                }
            }
        });
    }
}
