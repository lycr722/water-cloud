package com.example.order.service;

import com.example.common.tools.Result;
import com.example.feign.clients.BuildingClient;
import com.example.feign.entity.WaterProduct;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RSet;
import org.redisson.api.RedissonClient;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.List;

/**
 * 布隆过滤器拦截非法商品 ID；Redis Set 记录已下架商品，减轻缓存穿透。
 */
@Service
public class ProductGuardService {
    private static final String BLOOM_NAME = "bloom:product:ids";
    private static final String OFFLINE_SET = "product:offline:ids";

    private final RedissonClient redissonClient;
    private final BuildingClient buildingClient;

    public ProductGuardService(RedissonClient redissonClient, BuildingClient buildingClient) {
        this.redissonClient = redissonClient;
        this.buildingClient = buildingClient;
    }

    @PostConstruct
    public void initBloom() {
        rebuildFromRemote();
    }

    @Scheduled(cron = "${order.product.bloom-refresh-cron:0 15 * * * ?}")
    public void scheduledRebuild() {
        rebuildFromRemote();
    }

    public void rebuildFromRemote() {
        try {
            Result<List<WaterProduct>> result = buildingClient.listProducts();
            if (result == null || !result.isFlag() || result.getData() == null) {
                return;
            }
            List<WaterProduct> list = result.getData();
            RBloomFilter<String> bloom = redissonClient.getBloomFilter(BLOOM_NAME);
            bloom.delete();
            bloom.tryInit(500_000L, 0.001);
            RSet<String> offline = redissonClient.getSet(OFFLINE_SET);
            offline.clear();
            if (list.isEmpty()) {
                bloom.add("DEFAULT_WATER");
            }
            for (WaterProduct p : list) {
                if (p.getProductId() != null) {
                    bloom.add(p.getProductId());
                    if (p.getStatus() != null && p.getStatus() == 0) {
                        offline.add(p.getProductId());
                    }
                }
            }
        } catch (Exception ignored) {
        }
    }

    public boolean isOrderable(String productId) {
        if (productId == null || productId.isEmpty()) {
            return false;
        }
        RBloomFilter<String> bloom = redissonClient.getBloomFilter(BLOOM_NAME);
        if (!bloom.contains(productId)) {
            return false;
        }
        RSet<String> offline = redissonClient.getSet(OFFLINE_SET);
        return !offline.contains(productId);
    }
}
