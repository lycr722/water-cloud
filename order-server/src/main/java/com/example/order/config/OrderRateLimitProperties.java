package com.example.order.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "order.ratelimit")
public class OrderRateLimitProperties {
    private Rule create = new Rule();
    private Rule accept = new Rule();

    public Rule getCreate() {
        return create;
    }

    public void setCreate(Rule create) {
        this.create = create;
    }

    public Rule getAccept() {
        return accept;
    }

    public void setAccept(Rule accept) {
        this.accept = accept;
    }

    public static class Rule {
        private long rate = 20;
        private long interval = 1;

        public long getRate() {
            return rate;
        }

        public void setRate(long rate) {
            this.rate = rate;
        }

        public long getInterval() {
            return interval;
        }

        public void setInterval(long interval) {
            this.interval = interval;
        }
    }
}
