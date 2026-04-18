package com.example.order.service;

import com.example.order.config.OrderRateLimitProperties;
import org.redisson.api.RRateLimiter;
import org.redisson.api.RateIntervalUnit;
import org.redisson.api.RateType;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

@Service
public class OrderRateLimitService {
    private final RedissonClient redissonClient;
    private final OrderRateLimitProperties properties;

    public OrderRateLimitService(RedissonClient redissonClient, OrderRateLimitProperties properties) {
        this.redissonClient = redissonClient;
        this.properties = properties;
    }

    public boolean tryAcquireCreateToken(String userId) {
        return tryAcquire("ratelimit:order:create:" + userId, properties.getCreate().getRate(), properties.getCreate().getInterval());
    }

    public boolean tryAcquireAcceptToken(String workerId) {
        return tryAcquire("ratelimit:order:accept:" + workerId, properties.getAccept().getRate(), properties.getAccept().getInterval());
    }

    private boolean tryAcquire(String key, long rate, long intervalSeconds) {
        RRateLimiter limiter = redissonClient.getRateLimiter(key);
        limiter.trySetRate(RateType.OVERALL, rate, intervalSeconds, RateIntervalUnit.SECONDS);
        return limiter.tryAcquire(1);
    }
}
