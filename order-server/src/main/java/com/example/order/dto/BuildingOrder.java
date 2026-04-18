package com.example.order.dto;

import com.example.order.entity.Order;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BuildingOrder {
    private String building;
    private int distance;
    private List<Order> order;
    private Integer totalQuantity;
    private boolean showSubList = false;
}
