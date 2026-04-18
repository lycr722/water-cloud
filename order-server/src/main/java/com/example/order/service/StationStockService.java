package com.example.order.service;

import org.redisson.api.RScript;
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Collections;

/**
 * 水站库存扣减：Lua 保证查询与扣减原子性。
 */
@Service
public class StationStockService {
    private static final String LUA = ""
            + "local k=KEYS[1];"
            + "local q=tonumber(ARGV[1]);"
            + "local cur=redis.call('GET',k);"
            + "if cur==false then redis.call('SET',k,'100000'); cur='100000'; end;"
            + "cur=tonumber(cur);"
            + "if cur < q then return -1 end;"
            + "return redis.call('DECRBY',k,q);";

    private final RedissonClient redissonClient;
    private final StringRedisTemplate stringRedisTemplate;

    public StationStockService(RedissonClient redissonClient, StringRedisTemplate stringRedisTemplate) {
        this.redissonClient = redissonClient;
        this.stringRedisTemplate = stringRedisTemplate;
    }

    public boolean tryDeduct(String stationId, String productId, int quantity) {
        if (quantity <= 0) {
            return true;
        }
        String key = stockKey(stationId, productId);
        Long result = redissonClient.getScript().eval(
                RScript.Mode.READ_WRITE,
                LUA,
                RScript.ReturnType.INTEGER,
                Collections.singletonList(key),
                String.valueOf(quantity));
        return result != null && result >= 0;
    }

    /**
     * 下单失败时回滚库存（与 Lua 扣减对应）。
     */
    public void restore(String stationId, String productId, int quantity) {
        if (quantity <= 0) {
            return;
        }
        stringRedisTemplate.opsForValue().increment(stockKey(stationId, productId), quantity);
    }

    public static String stockKey(String stationId, String productId) {
        return "stock:station:" + stationId + ":product:" + productId;
    }
}
