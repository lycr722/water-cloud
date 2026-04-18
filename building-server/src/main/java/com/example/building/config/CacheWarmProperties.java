package com.example.building.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "cache")
public class CacheWarmProperties {
    private Warm warm = new Warm();
    private long logicalTtlMs = 300_000L;
    private int physicalExtraJitterSec = 300;

    public Warm getWarm() {
        return warm;
    }

    public void setWarm(Warm warm) {
        this.warm = warm;
    }

    public long getLogicalTtlMs() {
        return logicalTtlMs;
    }

    public void setLogicalTtlMs(long logicalTtlMs) {
        this.logicalTtlMs = logicalTtlMs;
    }

    public int getPhysicalExtraJitterSec() {
        return physicalExtraJitterSec;
    }

    public void setPhysicalExtraJitterSec(int physicalExtraJitterSec) {
        this.physicalExtraJitterSec = physicalExtraJitterSec;
    }

    public static class Warm {
        private String cron = "0 0 * * * ?";

        public String getCron() {
            return cron;
        }

        public void setCron(String cron) {
            this.cron = cron;
        }
    }
}
