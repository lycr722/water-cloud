package com.example.order.controller;

import com.example.order.config.MqConstants;
import com.example.order.dto.OrderEventMessage;
import com.example.order.dto.OrderPersistMessage;
import com.example.order.entity.Order;
import com.example.order.service.OrderMessagePublisher;
import com.example.order.service.OrderService;
import com.example.common.tools.Result;
import com.rabbitmq.client.Channel;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Date;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * 异步削峰：可靠队列落库后广播领域事件。
 */
@Component
public class OrderPersistListener {
    private final OrderService orderService;
    private final OrderMessagePublisher orderMessagePublisher;
    private final StringRedisTemplate stringRedisTemplate;

    public OrderPersistListener(
            OrderService orderService,
            OrderMessagePublisher orderMessagePublisher,
            StringRedisTemplate stringRedisTemplate) {
        this.orderService = orderService;
        this.orderMessagePublisher = orderMessagePublisher;
        this.stringRedisTemplate = stringRedisTemplate;
    }

    @RabbitListener(queues = MqConstants.PERSIST_QUEUE)
    public void onPersist(OrderPersistMessage payload, Channel channel, Message rawMessage) throws IOException {
        long tag = rawMessage.getMessageProperties().getDeliveryTag();
        try {
            Order order = payload.getOrder();
            String doneKey = "order:persisted:" + order.getOrderId();
            if (Boolean.TRUE.equals(stringRedisTemplate.hasKey(doneKey))) {
                channel.basicAck(tag, false);
                return;
            }
            Result result = orderService.createOrderFromPersist(order);
            if (!result.isFlag()) {
                channel.basicNack(tag, false, true);
                return;
            }
            stringRedisTemplate.opsForValue().set(doneKey, "1", 48L, TimeUnit.HOURS);
            OrderEventMessage event = new OrderEventMessage();
            event.setMessageId(UUID.randomUUID().toString());
            event.setEventType("ORDER_CREATED");
            event.setOrderId(order.getOrderId());
            event.setUserId(order.getUserId());
            event.setWorkerId(order.getWorkerId());
            event.setBuilding(order.getBuilding());
            event.setQuantity(order.getQuantity());
            event.setEventTime(new Date());
            orderMessagePublisher.publishCommand(MqConstants.COMMAND_KEY_CREATE, event);
            channel.basicAck(tag, false);
        } catch (Exception ex) {
            channel.basicNack(tag, false, false);
        }
    }
}
