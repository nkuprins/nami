package com.app.backend.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {

    public static final String EXCHANGE = "media.exchange";
    public static final String ROUTING_KEY = "image.process";
    public static final String QUEUE = "image.process.q";

    public static final String DLX = "media.dlx";
    public static final String DLQ = "image.process.dlq";

    @Bean
    DirectExchange mediaExchange() {
        return new DirectExchange(EXCHANGE);
    }

    @Bean
    Queue imageProcessQueue() {
        return QueueBuilder.durable(QUEUE)
                .deadLetterExchange(DLX)
                .deadLetterRoutingKey(ROUTING_KEY)
                .build();
    }

    @Bean
    Binding imageProcessBinding() {
        return BindingBuilder.bind(imageProcessQueue()).to(mediaExchange()).with(ROUTING_KEY);
    }

    @Bean
    DirectExchange mediaDlx() {
        return new DirectExchange(DLX);
    }

    @Bean
    Queue imageProcessDlq() {
        return QueueBuilder.durable(DLQ).build();
    }

    @Bean
    Binding imageProcessDlqBinding() {
        return BindingBuilder.bind(imageProcessDlq()).to(mediaDlx()).with(ROUTING_KEY);
    }

    @Bean
    MessageConverter jsonMessageConverter() {
        return new JacksonJsonMessageConverter();
    }
}
