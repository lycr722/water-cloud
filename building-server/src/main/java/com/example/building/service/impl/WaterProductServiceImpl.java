package com.example.building.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.building.entity.WaterProduct;
import com.example.building.mapper.WaterProductMapper;
import com.example.building.service.WaterProductService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class WaterProductServiceImpl extends ServiceImpl<WaterProductMapper, WaterProduct> implements WaterProductService {
    @Override
    public List<WaterProduct> listAllFromDb() {
        return list();
    }
}
