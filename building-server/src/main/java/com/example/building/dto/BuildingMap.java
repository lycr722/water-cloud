package com.example.building.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BuildingMap {
    private Integer id;
    private String name;
    private String longitude;
    private String latitude;
    private Integer distance;
    private Integer quantity;
    private Integer height=30;
    private Integer width=30;
    private String iconPath="../../../static/merchant/location_fill.png\n";
}
