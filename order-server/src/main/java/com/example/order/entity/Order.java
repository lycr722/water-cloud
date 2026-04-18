package com.example.order.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;

import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("t_order")
public class Order {
    @TableId
    private String orderId;

    private String dormi;

    @TableField("state_delivery")
    private Integer stateDelivery;

    private Integer quantity;

    /**
     * 水品/商品 ID，对应 t_water_product.product_id
     */
    @TableField("product_id")
    private String productId;

    /**
     * 水站 ID，库存按「站 + 商品」维度扣减
     */
    @TableField("station_id")
    private String stationId;

    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss",timezone = "GMT+8")
    private Date time;

    private String building;

    @TableField("user_id")
    private String userId;

    @TableField("worker_id")
    private String workerId;

    public Order(Integer quantity, String userId) {
        this.quantity = quantity;
        this.userId = userId;
    }
}
