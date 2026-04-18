package com.example.order.service;

import com.example.order.config.MqConstants;
import com.example.order.dto.OrderEventMessage;
import com.example.order.dto.OrderPersistMessage;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

@Service
public class OrderMessagePublisher {
    private final RabbitTemplate rabbitTemplate;

    public OrderMessagePublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void publishCommand(String routingKey, OrderEventMessage message) {
        CorrelationData correlationData = new CorrelationData(message.getMessageId());
        rabbitTemplate.convertAndSend(MqConstants.COMMAND_EXCHANGE, routingKey, message, correlationData);
    }

    public void publishEvent(String routingKey, OrderEventMessage message) {
        rabbitTemplate.convertAndSend(MqConstants.EVENT_EXCHANGE, routingKey, message);
    }

    public void publishPersist(OrderPersistMessage message) {
        CorrelationData correlationData = new CorrelationData(message.getMessageId());
        rabbitTemplate.convertAndSend(MqConstants.PERSIST_EXCHANGE, MqConstants.PERSIST_KEY, message, correlationData);
    }
}
