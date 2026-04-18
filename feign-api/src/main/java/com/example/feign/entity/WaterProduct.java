package com.example.feign.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 水品（商品）信息，供楼栋服务与订单服务共用。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class WaterProduct {
    private String productId;
    private String name;
    /**
     * 1 上架 0 下架
     */
    private Integer status;
}
