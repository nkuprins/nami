package com.app.backend.messaging;

import com.app.backend.config.RabbitConfig;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.amqp.autoconfigure.RabbitAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.rabbitmq.RabbitMQContainer;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Proves the declared topology and JSON converter actually work end-to-end against a real broker:
 * a published {@link ImageProcessingMessage} lands on the work queue and round-trips back intact.
 */
@SpringBootTest(classes = {RabbitConfig.class, RabbitAutoConfiguration.class})
class RabbitTopologyIntegrationTest {

    static final RabbitMQContainer rabbit = new RabbitMQContainer("rabbitmq:3.13-management");

    static {
        rabbit.start();
    }

    @DynamicPropertySource
    static void rabbitProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.rabbitmq.host", rabbit::getHost);
        registry.add("spring.rabbitmq.port", rabbit::getAmqpPort);
        registry.add("spring.rabbitmq.username", rabbit::getAdminUsername);
        registry.add("spring.rabbitmq.password", rabbit::getAdminPassword);
        // Re-enable the auto RabbitAdmin (the broker-less tests turn it off) so the topology is declared.
        registry.add("spring.rabbitmq.dynamic", () -> "true");
    }

    @Autowired RabbitTemplate rabbitTemplate;

    @Test
    void publishedMessage_roundTripsThroughQueue() {
        ImageProcessingMessage sent =
                new ImageProcessingMessage(UUID.randomUUID(), "https://cdn.test.local/uploads/x/foo.jpg");

        rabbitTemplate.convertAndSend(RabbitConfig.EXCHANGE, RabbitConfig.ROUTING_KEY, sent);
        Object received = rabbitTemplate.receiveAndConvert(RabbitConfig.QUEUE, 5_000);

        assertThat(received).isEqualTo(sent);
    }
}
