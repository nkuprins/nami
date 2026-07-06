package com.app.backend.messaging.worker;

import com.app.backend.config.RabbitConfig;
import com.app.backend.messaging.ImageProcessingMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("worker")
@RequiredArgsConstructor
public class ImageProcessingListener {

    private final ImageProcessingService imageProcessingService;

    @RabbitListener(queues = RabbitConfig.QUEUE)
    public void onMessage(ImageProcessingMessage message) {
        imageProcessingService.process(message.cdnUrl());
    }
}
