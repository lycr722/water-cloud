package com.example.building.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.building.dto.BuildingMap;
import com.example.building.entity.Building;
import com.example.building.mapper.BuildingMapper;
import com.example.building.service.BuildingDictCacheService;
import com.example.building.service.BuildingService;
import com.example.common.tools.DistanceUtils;
import com.example.common.tools.Result;
import com.example.feign.clients.OrderClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.List;

@Service
public class BuildingServiceImpl extends ServiceImpl<BuildingMapper, Building> implements BuildingService {

    @Autowired
    private BuildingMapper buildingMapper;
    @Autowired
    private BuildingDictCacheService buildingDictCacheService;
    @Autowired
    private OrderClient orderClient;

    /**
     * 查询所有building，渲染到学生注册宿舍楼选择框
     * @return
     */
    @Override
    public List<Building> selectAllBuildings() {
        return buildingDictCacheService.getBuildingsOrLoad(() -> buildingMapper.selectList(null));
    }

    @Override
    public List<BuildingMap> getAllBuildingMap(String workerId,String lng,String lat) {
        List<BuildingMap> buildingMapList = new ArrayList<>();
        List<Building> buildingList = buildingDictCacheService.getBuildingsOrLoad(() -> buildingMapper.selectList(null));
        for (Building building : buildingList) {

            Result selectQuantity = orderClient.selectQuantity(workerId, building.getName(), 1);
            Integer quantity = (Integer) selectQuantity.getData();
            if (quantity!=0&&quantity!=null)
            {
                BuildingMap buildingMap = new BuildingMap();
                buildingMap.setQuantity(quantity);
                buildingMap.setId(building.getBid());
                buildingMap.setName(building.getName());
                buildingMap.setLongitude(building.getLng());
                buildingMap.setLatitude(building.getLat());
                buildingMap.setDistance(
                        DistanceUtils.getDistanceStr(lng,lat,building.getLng(),building.getLat()));
                buildingMapList.add(buildingMap);
            }
        }
        return buildingMapList;
    }
}
