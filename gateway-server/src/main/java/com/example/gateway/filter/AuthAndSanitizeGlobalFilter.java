package com.example.gateway.filter;

import com.example.gateway.config.GatewayAuthProperties;
import org.springframework.core.Ordered;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

@Component
public class AuthAndSanitizeGlobalFilter implements GlobalFilter, Ordered {
    private static final Pattern SQL_INJECTION_PATTERN = Pattern.compile("('|--|/\\*|\\*/|\\b(select|update|delete|insert|drop|union|truncate)\\b)", Pattern.CASE_INSENSITIVE);
    private final ReactiveStringRedisTemplate redisTemplate;
    private final GatewayAuthProperties authProperties;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    public AuthAndSanitizeGlobalFilter(ReactiveStringRedisTemplate redisTemplate, GatewayAuthProperties authProperties) {
        this.redisTemplate = redisTemplate;
        this.authProperties = authProperties;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getPath().value();
        String rawQuery = request.getURI().getRawQuery();
        if (StringUtils.hasText(rawQuery) && SQL_INJECTION_PATTERN.matcher(rawQuery).find()) {
            return writeError(exchange.getResponse(), HttpStatus.BAD_REQUEST, "Illegal request query");
        }
        if (shouldSkipAuth(path)) {
            return chain.filter(exchange);
        }

        String token = request.getHeaders().getFirst("Authorization");
        if (!StringUtils.hasText(token)) {
            token = request.getQueryParams().getFirst("token");
        }
        if (!StringUtils.hasText(token)) {
            return writeError(exchange.getResponse(), HttpStatus.UNAUTHORIZED, "Missing token");
        }

        String normalizedToken = token.replace("Bearer ", "").trim();
        String redisKey = "user:" + normalizedToken;
        return redisTemplate.opsForValue()
                .get(redisKey)
                .flatMap(value -> {
                    if (!"exist".equals(value)) {
                        return writeError(exchange.getResponse(), HttpStatus.UNAUTHORIZED, "Invalid token");
                    }
                    return chain.filter(exchange);
                })
                .switchIfEmpty(writeError(exchange.getResponse(), HttpStatus.UNAUTHORIZED, "Token expired"));
    }

    private boolean shouldSkipAuth(String path) {
        return authProperties.getIgnorePaths().stream().anyMatch(ignore -> pathMatcher.matchStart(ignore + "**", path));
    }

    private Mono<Void> writeError(ServerHttpResponse response, HttpStatus status, String message) {
        response.setStatusCode(status);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        byte[] bytes = ("{\"code\":" + status.value() + ",\"message\":\"" + message + "\"}").getBytes(StandardCharsets.UTF_8);
        return response.writeWith(Mono.just(response.bufferFactory().wrap(bytes)));
    }

    @Override
    public int getOrder() {
        return -100;
    }
}
