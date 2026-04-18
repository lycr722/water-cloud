package com.example.feign.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Building {
    private String name;

    @TableId(type = IdType.AUTO)
    private Integer bid;

    private String binfo;

    private String lng;

    private String lat;
}
