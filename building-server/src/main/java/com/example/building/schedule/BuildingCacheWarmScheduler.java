package com.example.building.schedule;

import com.example.building.mapper.BuildingMapper;
import com.example.building.service.BuildingDictCacheService;
import com.example.building.service.WaterProductService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class BuildingCacheWarmScheduler {
    private final BuildingMapper buildingMapper;
    private final WaterProductService waterProductService;
    private final BuildingDictCacheService buildingDictCacheService;

    public BuildingCacheWarmScheduler(
            BuildingMapper buildingMapper,
            WaterProductService waterProductService,
            BuildingDictCacheService buildingDictCacheService) {
        this.buildingMapper = buildingMapper;
        this.waterProductService = waterProductService;
        this.buildingDictCacheService = buildingDictCacheService;
    }

    @Scheduled(cron = "${cache.warm.cron}")
    public void warmDictCaches() {
        buildingDictCacheService.writeBuildings(buildingMapper.selectList(null));
        buildingDictCacheService.writeProducts(waterProductService.listAllFromDb());
    }
}
