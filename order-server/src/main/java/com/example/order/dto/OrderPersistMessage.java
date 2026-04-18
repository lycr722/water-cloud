package com.example.order.dto;

import com.example.order.entity.Order;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderPersistMessage {
    private String messageId;
    private Order order;
}
