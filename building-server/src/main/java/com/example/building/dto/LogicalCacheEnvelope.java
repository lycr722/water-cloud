package com.example.building.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 逻辑过期封装：expireAt 之前视为有效；过期后仍可返回数据并由调用方触发异步刷新（防击穿）。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LogicalCacheEnvelope<T> {
    private long expireAt;
    private T data;
}
