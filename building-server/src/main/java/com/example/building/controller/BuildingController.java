package com.example.building.controller;

import com.example.building.dto.BuildingMap;
import com.example.building.entity.Building;
import com.example.building.entity.WaterProduct;
import com.example.building.service.BuildingDictCacheService;
import com.example.building.service.BuildingService;
import com.example.building.service.WaterProductService;
import com.example.common.tools.Result;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.List;

@RestController
@RequestMapping("/building")
public class BuildingController {

    @Resource
    private BuildingService buildingService;
    @Resource
    private WaterProductService waterProductService;
    @Resource
    private BuildingDictCacheService buildingDictCacheService;

    @Operation(summary = "查询所有building，渲染到学生注册宿舍楼选择框")
    @GetMapping("/all")
    public Result<List<Building>> selectAllBuildings(){
        List<Building> buildingList = buildingService.selectAllBuildings();
        return buildingList!=null?new Result(true,buildingList)
                :new Result(false);
    }

    @Operation(summary = "查询宿舍楼地图，距离及宿舍楼信息")
    @GetMapping("/map")
    public Result selectBuildingMap(@RequestParam("workerId") String workerId,
                                    @RequestParam("lng") String lng,
                                    @RequestParam("lat") String lat){
        List<BuildingMap> buildingMapList = buildingService.getAllBuildingMap(workerId, lng, lat);
        return buildingMapList!=null?new Result(true,buildingMapList)
                :new Result(false);
    }

    @Operation(summary = "水品字典列表（带缓存预热与逻辑过期）")
    @GetMapping("/product/list")
    public Result<java.util.List<com.example.feign.entity.WaterProduct>> listWaterProducts() {
        java.util.List<WaterProduct> list = buildingDictCacheService.getProductsOrLoad(
                () -> waterProductService.listAllFromDb());
        java.util.List<com.example.feign.entity.WaterProduct> out = new java.util.ArrayList<>();
        for (WaterProduct p : list) {
            out.add(new com.example.feign.entity.WaterProduct(p.getProductId(), p.getName(), p.getStatus()));
        }
        return new Result<>(true, out);
    }
}
