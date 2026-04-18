package com.example.order.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.order.entity.Order;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface OrderMapper extends BaseMapper<Order> {

    @Select("SELECT SUM(quantity) FROM water_order.t_order WHERE time >= DATE_SUB(CURDATE(), INTERVAL 1 WEEK)" +
            "AND worker_id = #{workerId} AND state_delivery = 2")
    Integer getWeeklyOrderQuantity(@Param("workerId") String workerId);

    @Select("SELECT SUM(quantity) FROM water_order.t_order WHERE time >= DATE_SUB(CURDATE(), INTERVAL 1 MONTH)" +
            "AND worker_id = #{workerId} AND state_delivery = 2")
    Integer getMonthlyOrderQuantity(@Param("workerId") String workerId);
}
