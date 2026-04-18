package com.example.building.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.building.dto.BuildingMap;
import com.example.building.entity.Building;

import java.util.List;

public interface BuildingService extends IService<Building> {

    //查询所有building，渲染到学生注册宿舍楼选择框
    List<Building> selectAllBuildings();

    List<BuildingMap> getAllBuildingMap(String workerId,String lng,String lat);
}
