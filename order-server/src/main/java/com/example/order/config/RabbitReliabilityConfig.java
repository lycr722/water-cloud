package com.example.order.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class RabbitReliabilityConfig {
    @Bean
    public DirectExchange orderCommandExchange() {
        return new DirectExchange(MqConstants.COMMAND_EXCHANGE, true, false);
    }

    @Bean
    public TopicExchange orderEventExchange() {
        return new TopicExchange(MqConstants.EVENT_EXCHANGE, true, false);
    }

    @Bean
    public Queue orderCommandQueue() {
        Map<String, Object> args = new HashMap<>();
        args.put("x-dead-letter-exchange", MqConstants.COMMAND_EXCHANGE);
        args.put("x-dead-letter-routing-key", "order.command.dlq");
        return new Queue(MqConstants.COMMAND_QUEUE, true, false, false, args);
    }

    @Bean
    public Queue orderCommandDlq() {
        return new Queue(MqConstants.COMMAND_DLQ, true);
    }

    @Bean
    public Queue userNotifyQueue() {
        return new Queue(MqConstants.USER_NOTIFY_QUEUE, true);
    }

    @Bean
    public Queue workerNotifyQueue() {
        return new Queue(MqConstants.WORKER_NOTIFY_QUEUE, true);
    }

    @Bean
    public Binding commandCreateBinding(Queue orderCommandQueue, DirectExchange orderCommandExchange) {
        return BindingBuilder.bind(orderCommandQueue).to(orderCommandExchange).with(MqConstants.COMMAND_KEY_CREATE);
    }

    @Bean
    public Binding commandAcceptBinding(Queue orderCommandQueue, DirectExchange orderCommandExchange) {
        return BindingBuilder.bind(orderCommandQueue).to(orderCommandExchange).with(MqConstants.COMMAND_KEY_ACCEPT);
    }

    @Bean
    public Binding commandFinishBinding(Queue orderCommandQueue, DirectExchange orderCommandExchange) {
        return BindingBuilder.bind(orderCommandQueue).to(orderCommandExchange).with(MqConstants.COMMAND_KEY_FINISH);
    }

    @Bean
    public Binding commandDlqBinding(Queue orderCommandDlq, DirectExchange orderCommandExchange) {
        return BindingBuilder.bind(orderCommandDlq).to(orderCommandExchange).with("order.command.dlq");
    }

    @Bean
    public Binding userNotifyBinding(Queue userNotifyQueue, TopicExchange orderEventExchange) {
        return BindingBuilder.bind(userNotifyQueue).to(orderEventExchange).with("order.event.user.#");
    }

    @Bean
    public Binding workerNotifyBinding(Queue workerNotifyQueue, TopicExchange orderEventExchange) {
        return BindingBuilder.bind(workerNotifyQueue).to(orderEventExchange).with("order.event.worker.#");
    }

    @Bean
    public DirectExchange orderPersistExchange() {
        return new DirectExchange(MqConstants.PERSIST_EXCHANGE, true, false);
    }

    @Bean
    public Queue orderPersistQueue() {
        Map<String, Object> args = new HashMap<>();
        args.put("x-dead-letter-exchange", MqConstants.COMMAND_EXCHANGE);
        args.put("x-dead-letter-routing-key", "order.command.dlq");
        return new Queue(MqConstants.PERSIST_QUEUE, true, false, false, args);
    }

    @Bean
    public Binding orderPersistBinding(Queue orderPersistQueue, DirectExchange orderPersistExchange) {
        return BindingBuilder.bind(orderPersistQueue).to(orderPersistExchange).with(MqConstants.PERSIST_KEY);
    }

    @Bean
    public DirectExchange cacheInvalidateExchange() {
        return new DirectExchange(MqConstants.CACHE_INVALIDATE_EXCHANGE, true, false);
    }

    @Bean
    public Queue cacheInvalidateOrderQueue() {
        return new Queue(MqConstants.CACHE_INVALIDATE_QUEUE_ORDER, true);
    }

    @Bean
    public Binding cacheInvalidateOrderBinding(Queue cacheInvalidateOrderQueue, DirectExchange cacheInvalidateExchange) {
        return BindingBuilder.bind(cacheInvalidateOrderQueue).to(cacheInvalidateExchange).with(MqConstants.CACHE_INVALIDATE_KEY);
    }

    @Bean
    public RabbitTemplate.ConfirmCallback confirmCallback() {
        return (CorrelationData correlationData, boolean ack, String cause) -> {
            if (!ack) {
                System.err.println("消息发送到交换机失败, correlationId=" + (correlationData == null ? null : correlationData.getId()) + ", cause=" + cause);
            }
        };
    }

    @Bean
    public RabbitTemplate.ReturnsCallback returnsCallback() {
        return returned -> System.err.println(
                "消息路由失败, exchange=" + returned.getExchange()
                        + ", routingKey=" + returned.getRoutingKey()
                        + ", replyCode=" + returned.getReplyCode()
                        + ", replyText=" + returned.getReplyText()
        );
    }
}
