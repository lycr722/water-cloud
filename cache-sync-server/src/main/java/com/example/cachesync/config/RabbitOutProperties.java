package com.example.cachesync.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "cache-sync.rabbit")
public class RabbitOutProperties {
    private String invalidateExchange = "cache.invalidate.exchange";
    private String invalidateRoutingKey = "cache.invalidate";

    public String getInvalidateExchange() {
        return invalidateExchange;
    }

    public void setInvalidateExchange(String invalidateExchange) {
        this.invalidateExchange = invalidateExchange;
    }

    public String getInvalidateRoutingKey() {
        return invalidateRoutingKey;
    }

    public void setInvalidateRoutingKey(String invalidateRoutingKey) {
        this.invalidateRoutingKey = invalidateRoutingKey;
    }
}
