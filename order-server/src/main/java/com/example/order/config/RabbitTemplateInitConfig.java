package com.example.order.config;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;

@Configuration
public class RabbitTemplateInitConfig {
    private final RabbitTemplate rabbitTemplate;
    private final RabbitTemplate.ConfirmCallback confirmCallback;
    private final RabbitTemplate.ReturnsCallback returnsCallback;

    public RabbitTemplateInitConfig(
            RabbitTemplate rabbitTemplate,
            RabbitTemplate.ConfirmCallback confirmCallback,
            RabbitTemplate.ReturnsCallback returnsCallback) {
        this.rabbitTemplate = rabbitTemplate;
        this.confirmCallback = confirmCallback;
        this.returnsCallback = returnsCallback;
    }

    @PostConstruct
    public void init() {
        rabbitTemplate.setConfirmCallback(confirmCallback);
        rabbitTemplate.setReturnsCallback(returnsCallback);
    }
}
