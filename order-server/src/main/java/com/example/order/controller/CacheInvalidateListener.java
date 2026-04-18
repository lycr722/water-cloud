package com.example.order.controller;

import com.example.order.config.MqConstants;
import com.example.order.service.ProductGuardService;
import com.rabbitmq.client.Channel;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * 接收 Binlog 触发的缓存失效广播，重建布隆与下架集合。
 */
@Component
public class CacheInvalidateListener {
    private final ProductGuardService productGuardService;

    public CacheInvalidateListener(ProductGuardService productGuardService) {
        this.productGuardService = productGuardService;
    }

    @RabbitListener(queues = MqConstants.CACHE_INVALIDATE_QUEUE_ORDER)
    public void onInvalidate(String body, Channel channel, Message rawMessage) throws IOException {
        try {
            productGuardService.rebuildFromRemote();
            channel.basicAck(rawMessage.getMessageProperties().getDeliveryTag(), false);
        } catch (Exception e) {
            channel.basicNack(rawMessage.getMessageProperties().getDeliveryTag(), false, true);
        }
    }
}
