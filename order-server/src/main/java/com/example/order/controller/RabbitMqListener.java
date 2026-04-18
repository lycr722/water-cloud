package com.example.order.controller;

import com.example.order.config.MqConstants;
import com.example.order.dto.OrderEventMessage;
import com.example.order.service.OrderMessagePublisher;
import com.example.order.service.OrderService;
import com.example.common.tools.Result;
import com.rabbitmq.client.Channel;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

@Component
public class RabbitMqListener {
    private final StringRedisTemplate stringRedisTemplate;
    private final OrderMessagePublisher orderMessagePublisher;
    private final OrderService orderService;

    public RabbitMqListener(StringRedisTemplate stringRedisTemplate, OrderMessagePublisher orderMessagePublisher, OrderService orderService) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.orderMessagePublisher = orderMessagePublisher;
        this.orderService = orderService;
    }

    @RabbitListener(queues = MqConstants.COMMAND_QUEUE)
    public void processOrderCommand(OrderEventMessage message, Channel channel, Message rawMessage) throws IOException {
        long deliveryTag = rawMessage.getMessageProperties().getDeliveryTag();
        try {
            String processKey = "mq:processed:" + message.getMessageId();
            Boolean firstConsume = stringRedisTemplate.opsForValue().setIfAbsent(processKey, "1", 24L, TimeUnit.HOURS);
            if (Boolean.FALSE.equals(firstConsume)) {
                channel.basicAck(deliveryTag, false);
                return;
            }
            // 处理业务逻辑
            if ("ORDER_ACCEPTED".equals(message.getEventType())) {
                orderService.acceptOrderByBuilding(message.getBuilding(), message.getWorkerId());
            }
            // 可以添加其他eventType的处理，如ORDER_FINISHED等

            if (message.getUserId() != null) {
                orderMessagePublisher.publishEvent(MqConstants.EVENT_KEY_USER + ".changed", message);
            }
            if (message.getWorkerId() != null) {
                orderMessagePublisher.publishEvent(MqConstants.EVENT_KEY_WORKER + ".changed", message);
            }
            channel.basicAck(deliveryTag, false);
        } catch (Exception ex) {
            channel.basicNack(deliveryTag, false, false);
        }
    }

    @RabbitListener(queues = MqConstants.USER_NOTIFY_QUEUE)
    public void listenUserNotify(OrderEventMessage message, Channel channel, Message rawMessage) throws IOException {
        System.out.println("通知中心发送给用户, eventType=" + message.getEventType() + ", orderId=" + message.getOrderId());
        channel.basicAck(rawMessage.getMessageProperties().getDeliveryTag(), false);
    }

    @RabbitListener(queues = MqConstants.WORKER_NOTIFY_QUEUE)
    public void listenWorkerNotify(OrderEventMessage message, Channel channel, Message rawMessage) throws IOException {
        System.out.println("通知中心发送给送水员, eventType=" + message.getEventType() + ", orderId=" + message.getOrderId());
        channel.basicAck(rawMessage.getMessageProperties().getDeliveryTag(), false);
    }

    @RabbitListener(queues = MqConstants.COMMAND_DLQ)
    public void listenDlq(OrderEventMessage message, Channel channel, Message rawMessage) throws IOException {
        System.err.println("进入死信队列, messageId=" + message.getMessageId() + ", eventType=" + message.getEventType());
        channel.basicAck(rawMessage.getMessageProperties().getDeliveryTag(), false);
    }
}
