package com.example.building.service;

import com.example.building.config.CacheWarmProperties;
import com.example.building.dto.LogicalCacheEnvelope;
import com.example.building.entity.Building;
import com.example.building.entity.WaterProduct;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Supplier;

@Service
public class BuildingDictCacheService {
    private static final String KEY_BUILDINGS = "cache:dict:buildings";
    private static final String KEY_PRODUCTS = "cache:dict:products";

    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;
    private final CacheWarmProperties cacheWarmProperties;

    public BuildingDictCacheService(
            StringRedisTemplate stringRedisTemplate,
            ObjectMapper objectMapper,
            CacheWarmProperties cacheWarmProperties) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.objectMapper = objectMapper;
        this.cacheWarmProperties = cacheWarmProperties;
    }

    public List<Building> getBuildingsOrLoad(Supplier<List<Building>> dbLoader) {
        try {
            String json = stringRedisTemplate.opsForValue().get(KEY_BUILDINGS);
            if (json != null) {
                LogicalCacheEnvelope<List<Building>> env = objectMapper.readValue(
                        json, new TypeReference<LogicalCacheEnvelope<List<Building>>>() {
                        });
                long now = System.currentTimeMillis();
                if (env.getExpireAt() >= now) {
                    return env.getData();
                }
                asyncReloadBuildings(dbLoader);
                return env.getData();
            }
        } catch (Exception ignored) {
        }
        List<Building> fresh = dbLoader.get();
        writeLogicalCache(KEY_BUILDINGS, fresh);
        return fresh;
    }

    public List<WaterProduct> getProductsOrLoad(Supplier<List<WaterProduct>> dbLoader) {
        try {
            String json = stringRedisTemplate.opsForValue().get(KEY_PRODUCTS);
            if (json != null) {
                LogicalCacheEnvelope<List<WaterProduct>> env = objectMapper.readValue(
                        json, new TypeReference<LogicalCacheEnvelope<List<WaterProduct>>>() {
                        });
                long now = System.currentTimeMillis();
                if (env.getExpireAt() >= now) {
                    return env.getData();
                }
                asyncReloadProducts(dbLoader);
                return env.getData();
            }
        } catch (Exception ignored) {
        }
        List<WaterProduct> fresh = dbLoader.get();
        writeLogicalCache(KEY_PRODUCTS, fresh);
        return fresh;
    }

    @Async
    public void asyncReloadBuildings(Supplier<List<Building>> dbLoader) {
        try {
            List<Building> fresh = dbLoader.get();
            if (fresh != null) {
                writeLogicalCache(KEY_BUILDINGS, fresh);
            }
        } catch (Exception ignored) {
        }
    }

    @Async
    public void asyncReloadProducts(Supplier<List<WaterProduct>> dbLoader) {
        try {
            List<WaterProduct> fresh = dbLoader.get();
            if (fresh != null) {
                writeLogicalCache(KEY_PRODUCTS, fresh);
            }
        } catch (Exception ignored) {
        }
    }

    public void writeBuildings(List<Building> buildings) {
        writeLogicalCache(KEY_BUILDINGS, buildings);
    }

    public void writeProducts(List<WaterProduct> products) {
        writeLogicalCache(KEY_PRODUCTS, products);
    }

    private <T> void writeLogicalCache(String key, T data) {
        try {
            long logicalMs = cacheWarmProperties.getLogicalTtlMs();
            long expireAt = System.currentTimeMillis() + logicalMs;
            LogicalCacheEnvelope<T> env = new LogicalCacheEnvelope<>(expireAt, data);
            String json = objectMapper.writeValueAsString(env);
            int jitterSec = ThreadLocalRandom.current().nextInt(cacheWarmProperties.getPhysicalExtraJitterSec() + 1);
            long physicalSec = logicalMs / 1000 + jitterSec;
            stringRedisTemplate.opsForValue().set(key, json, physicalSec, java.util.concurrent.TimeUnit.SECONDS);
        } catch (Exception ignored) {
        }
    }
}
