package com.app.backend.messaging;

import com.app.backend.config.RabbitConfig;
import com.app.backend.entity.Property;
import com.app.backend.entity.User;
import com.app.backend.repository.PropertyRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.util.List;

import static com.app.backend.testutil.TestData.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ImageBackfillRunnerTest {

    @Mock private PropertyRepository propertyRepository;
    @Mock private RabbitTemplate rabbitTemplate;

    @InjectMocks
    private ImageBackfillRunner runner;

    @Test
    void enqueuesOneJobPerMediaUrl_acrossAllProperties() {
        User owner = user();
        Property withMedia = listingWithPhotos(owner).getProperty(); // photo1, photo2
        Property withoutMedia = listing(owner).getProperty();        // no media
        when(propertyRepository.findAll()).thenReturn(List.of(withMedia, withoutMedia));

        runner.run(null);

        verify(rabbitTemplate, times(2)).convertAndSend(
                eq(RabbitConfig.EXCHANGE), eq(RabbitConfig.ROUTING_KEY), any(ImageProcessingMessage.class));
        verify(rabbitTemplate).convertAndSend(eq(RabbitConfig.EXCHANGE), eq(RabbitConfig.ROUTING_KEY),
                eq((Object) new ImageProcessingMessage(withMedia.getId(), "https://cdn.test.local/uploads/photo1.jpg")));
        verify(rabbitTemplate).convertAndSend(eq(RabbitConfig.EXCHANGE), eq(RabbitConfig.ROUTING_KEY),
                eq((Object) new ImageProcessingMessage(withMedia.getId(), "https://cdn.test.local/uploads/photo2.jpg")));
    }
}
