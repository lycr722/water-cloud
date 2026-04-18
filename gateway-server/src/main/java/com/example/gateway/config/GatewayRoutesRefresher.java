package com.example.gateway.config;

import org.springframework.cloud.context.environment.EnvironmentChangeEvent;
import org.springframework.cloud.gateway.event.RefreshRoutesEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Nacos 配置变更后刷新 Gateway 路由表（配合 spring.cloud.gateway.routes 外置到配置中心）。
 */
@Component
public class GatewayRoutesRefresher {
    private final ApplicationEventPublisher publisher;

    public GatewayRoutesRefresher(ApplicationEventPublisher publisher) {
        this.publisher = publisher;
    }

    @EventListener
    public void onEnvChange(EnvironmentChangeEvent event) {
        if (event.getKeys().stream().anyMatch(k -> k.startsWith("spring.cloud.gateway"))) {
            publisher.publishEvent(new RefreshRoutesEvent(this));
        }
    }
}
