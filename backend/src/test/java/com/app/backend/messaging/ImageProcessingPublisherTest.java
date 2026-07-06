package com.app.backend.messaging;

import com.app.backend.config.RabbitConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class ImageProcessingPublisherTest {

    @Mock private RabbitTemplate rabbitTemplate;

    @InjectMocks
    private ImageProcessingPublisher publisher;

    @AfterEach
    void tearDown() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    private void triggerAfterCommit() {
        new ArrayList<>(TransactionSynchronizationManager.getSynchronizations())
                .forEach(TransactionSynchronization::afterCommit);
    }

    @Test
    void enqueue_empty_publishesNothing() {
        TransactionSynchronizationManager.initSynchronization();

        publisher.enqueue(UUID.randomUUID(), List.of());
        triggerAfterCommit();

        verifyNoInteractions(rabbitTemplate);
    }

    @Test
    void enqueue_publishesOneMessagePerPhoto_afterCommit() {
        TransactionSynchronizationManager.initSynchronization();
        UUID propertyId = UUID.randomUUID();

        publisher.enqueue(propertyId, List.of("a.jpg", "b.jpg"));
        // Nothing is published until the transaction commits.
        verifyNoInteractions(rabbitTemplate);

        triggerAfterCommit();

        verify(rabbitTemplate).convertAndSend(eq(RabbitConfig.EXCHANGE), eq(RabbitConfig.ROUTING_KEY),
                eq((Object) new ImageProcessingMessage(propertyId, "a.jpg")));
        verify(rabbitTemplate).convertAndSend(eq(RabbitConfig.EXCHANGE), eq(RabbitConfig.ROUTING_KEY),
                eq((Object) new ImageProcessingMessage(propertyId, "b.jpg")));
    }
}
