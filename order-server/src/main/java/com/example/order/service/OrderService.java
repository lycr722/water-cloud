package com.example.order.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.common.tools.Result;
import com.example.order.dto.BuildingOrder;
import com.example.order.entity.Order;

import java.util.List;


public interface OrderService extends IService<Order> {
    Result createOrder(Order order);

    /**
     * 异步队列落库消费端调用，完成与用户下单相同的校验、库存与入库逻辑。
     */
    Result createOrderFromPersist(Order order);

    /**
     * 送水员按楼栋接单：订单级分布式锁 + 状态更新。
     */
    Result acceptOrderByBuilding(String building, String workerId);

    Order selectOrderByOrderId(String orderId);

    List<Order> selectOrder(String building,Integer delivery);

    /**
     * 用户根据id查看自己的订单
     * delivery为空查询所有，delivery不为空查询对应状态的订单
     * @param userId
     * @param delivery
     * @return
     */
    Result selectOrderByUserId(String userId,Integer delivery);

    List<BuildingOrder> selectBuildingOrderNotAccept(String lng,String lat);

    List<BuildingOrder> selectBuildingOrderNotDelivery(String workerId,String lng,String lat);

    /**
     * 送水员查看订单
     * 都为空查询所有，任一不为空查询特定
     * @param building
     * @param delivery
     * @return
     */
    List<Order> selectOrderByWorker(String workerId,String building, Integer delivery);

    /**
     * 送水员查询今日订单
     * @return
     */
    Result selectOrderToday(String workerId,Integer delivery);

    /**
     * 送水员查看水桶数
     * 都为空查询所有，任一不为空查询特定
     * @param building
     * @param delivery
     * @return
     */
    Integer selectQuantity(String workerId,String building, Integer delivery);

    /**
     * 送水员查看今日售出且送达水桶数
     * @return
     */
    Integer selectQuantityToday(String workerId);


    /**
     * 送水员查询今日营业额(已送达)
     * @return
     */
    Double selectTodayAmount(String workerId);

    Double selectWeeklyAmount(String workerId);

    Double selectMonthlyAmount(String workerId);

    /**
     * 送水员查询总营业额(已送达)
     * @return
     */
    Double selectTotalAmount(String workerId);


    /**
     * 修改订单
     * @param order
     * @return
     */
    Integer updateOrder(Order order);
}
