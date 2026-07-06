package com.app.backend.messaging;

import com.app.backend.config.RabbitConfig;
import com.app.backend.entity.Property;
import com.app.backend.repository.PropertyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * One-off migration: enqueues an image-processing job for every existing property's media, so
 * listings created before the pipeline get their variants generated. Publishes directly (no
 * afterCommit — there is no transaction to tie to) and is idempotent, so re-running is harmless.
 *
 * <p>Enable with {@code app.backfill-images=true} (env {@code APP_BACKFILL_IMAGES=true}) for a single
 * run, then turn it off — otherwise it re-enqueues on every restart. Requires a worker to be
 * consuming the queue. Safe to delete once the catalogue is backfilled.
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "app.backfill-images", havingValue = "true")
@RequiredArgsConstructor
public class ImageBackfillRunner implements ApplicationRunner {

    private final PropertyRepository propertyRepository;
    private final RabbitTemplate rabbitTemplate;

    @Override
    public void run(ApplicationArguments args) {
        List<Property> properties = propertyRepository.findAll();
        int jobs = 0;
        for (Property property : properties) {
            for (String url : property.allMediaUrls()) {
                rabbitTemplate.convertAndSend(RabbitConfig.EXCHANGE, RabbitConfig.ROUTING_KEY,
                        new ImageProcessingMessage(property.getId(), url));
                jobs++;
            }
        }
        log.info("Image backfill: enqueued {} job(s) across {} property/properties", jobs, properties.size());
    }
}
