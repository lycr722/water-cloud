package com.example.gateway.filter;

import org.redisson.api.RRateLimiter;
import org.redisson.api.RateIntervalUnit;
import org.redisson.api.RateType;
import org.redisson.api.RedissonClient;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;

@Component
public class RateLimitGlobalFilter implements GlobalFilter, Ordered {

    private final RedissonClient redissonClient;

    public RateLimitGlobalFilter(RedissonClient redissonClient) {
        this.redissonClient = redissonClient;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getPath().value();
        // 对核心接口限流，如/order/create, /order/accept
        if (path.startsWith("/order/create") || path.startsWith("/order/accept")) {
            String clientIp = exchange.getRequest().getRemoteAddress().getAddress().getHostAddress();
            String key = "gateway:ratelimit:" + clientIp;
            RRateLimiter limiter = redissonClient.getRateLimiter(key);
            limiter.trySetRate(RateType.OVERALL, 100, 1, RateIntervalUnit.MINUTES); // 每分钟100次
            if (!limiter.tryAcquire(1)) {
                return writeError(exchange.getResponse(), HttpStatus.TOO_MANY_REQUESTS, "请求过于频繁");
            }
        }
        return chain.filter(exchange);
    }

    private Mono<Void> writeError(ServerHttpResponse response, HttpStatus status, String message) {
        response.setStatusCode(status);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        byte[] bytes = ("{\"code\":" + status.value() + ",\"message\":\"" + message + "\"}").getBytes(StandardCharsets.UTF_8);
        return response.writeWith(Mono.just(response.bufferFactory().wrap(bytes)));
    }

    @Override
    public int getOrder() {
        return -200; // 在AuthAndSanitizeGlobalFilter之前
    }
}
