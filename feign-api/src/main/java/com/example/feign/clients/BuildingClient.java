package com.example.feign.clients;

import com.example.common.tools.Result;
import com.example.feign.entity.Building;
import com.example.feign.entity.WaterProduct;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@Component
@FeignClient("building-server")
public interface BuildingClient {
    @GetMapping("/building/all")
    Result<List<Building>> selectAllBuildings();

    @GetMapping("/building/product/list")
    Result<List<WaterProduct>> listProducts();
}
