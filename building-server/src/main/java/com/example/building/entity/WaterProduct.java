package com.example.building.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("t_water_product")
public class WaterProduct {
    @TableId
    private String productId;
    private String name;
    /** 1 上架 0 下架 */
    private Integer status;
}
