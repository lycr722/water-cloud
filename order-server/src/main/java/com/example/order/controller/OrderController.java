package com.example.order.controller;

import com.example.order.dto.BuildingOrder;
import com.example.order.dto.OrderEventMessage;
import com.example.order.dto.OrderPersistMessage;
import com.example.order.entity.Order;
import com.example.common.constant.WaterMessage;
import com.example.order.config.MqConstants;
import com.example.order.service.OrderMessagePublisher;
import com.example.order.service.OrderRateLimitService;
import com.example.order.service.OrderService;
import com.example.common.tools.Result;
import com.example.common.tools.UuidTool;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.List;
import java.util.UUID;


@RestController
@RequestMapping("/order")
//@CrossOrigin
public class OrderController {
    @Autowired
    private OrderService orderService;
    @Autowired
    private OrderMessagePublisher orderMessagePublisher;
    @Autowired
    private OrderRateLimitService orderRateLimitService;

    @PostMapping("/create")
    @Operation(summary = "用户创建订单")
    public Result createOrder(@RequestBody Order order) {
        if (!orderRateLimitService.tryAcquireCreateToken(order.getUserId())) {
            return new Result(false, "请求过于频繁，请稍后重试", 429);
        }
        Result result = orderService.createOrder(order);
        // 发送消息到消息队列
        if (result.isFlag()) {
            orderMessagePublisher.publishCommand(MqConstants.COMMAND_KEY_CREATE, buildMessage(order, "ORDER_CREATED"));
            result.setMessage(WaterMessage.ORDER_CREATE_SUCCESS);
        }
        return result;
    }

    @PostMapping("/create/async")
    @Operation(summary = "异步削峰下单（消息队列落库）")
    public Result createOrderAsync(@RequestBody Order order) {
        if (!orderRateLimitService.tryAcquireCreateToken(order.getUserId())) {
            return new Result(false, "请求过于频繁，请稍后重试", 429);
        }
        if (order.getOrderId() == null || order.getOrderId().isEmpty()) {
            order.setOrderId(UuidTool.getUUID32());
        }
        String msgId = UUID.randomUUID().toString();
        orderMessagePublisher.publishPersist(new OrderPersistMessage(msgId, order));
        return new Result(true, "订单已排队处理", order.getOrderId());
    }

    @GetMapping("/select/byUser")
    @Operation(summary = "根据用户id查询所有订单")
    public Result selectOrderByUserId
            (@RequestParam("userId") String userId,
             @RequestParam(name = "delivery", required = false) Integer delivery) {

        return orderService.selectOrderByUserId(userId, delivery);
    }

    @GetMapping("/select/byWorker")
    @Operation(summary = "送水员查看订单")
    public Result selectOrderByBuildingNotDelivery
            (@RequestParam("workerId") String workerId,
             @RequestParam(name = "building", required = false) String building,
             @RequestParam(name = "delivery", required = false) Integer delivery) {

        List<Order> orderList = orderService.selectOrderByWorker(workerId, building, delivery);
        return orderList != null ? new Result(true, orderList)
                : new Result(false);
    }

    @GetMapping("/select/today")
    @Operation(summary = "送水员查询今天的所有订单")
    public Result<List<Order>> selectOrderToday(@RequestParam("workerId") String workerId,
                                   @RequestParam(name = "delivery",required = false)Integer delivery) {

        return orderService.selectOrderToday(workerId,delivery);
    }

    @GetMapping("/select/accept")
    @Operation(summary = "送水员查看所有待接单的二级订单,用于接单大厅")
    public Result selectBuildingOrderNotAccept(@RequestParam("lng") String lng,
                                               @RequestParam("lat") String lat) {
        List<BuildingOrder> buildingOrderList = orderService.selectBuildingOrderNotAccept(lng,lat);
        return buildingOrderList != null ? new Result(true, buildingOrderList)
                : new Result(false);
    }

    @GetMapping("/select/delivery")
    @Operation(summary = "送水员查看所有自己已接单未送达的二级订单,用于送单大厅")
    public Result selectBuildingOrderNotDelivery(@RequestParam("workerId") String workerId,
                                                 @RequestParam("lng") String lng,
                                                 @RequestParam("lat") String lat){
        List<BuildingOrder> buildingOrderList = orderService.selectBuildingOrderNotDelivery(
                workerId,lng,lat);
        return buildingOrderList != null ? new Result(true, buildingOrderList)
                : new Result(false);
    }

    @GetMapping("/quantity")
    @Operation(summary = "查询水桶数")
    public Result selectQuantity
            (@RequestParam("workerId") String workerId,
             @RequestParam(name = "building", required = false) String building,
             @RequestParam(name = "delivery", required = false) Integer delivery) {

        Integer buildingQuantity = orderService.selectQuantity(workerId, building, delivery);
        return buildingQuantity != null ? new Result(true, buildingQuantity)
                : new Result(false);
    }

    @GetMapping("/quantity/today")
    @Operation(summary = "查询今日售出且送达桶数")
    public Result selectQuantityToday(@RequestParam("workerId") String workerId) {
        Integer buildingQuantity = orderService.selectQuantityToday(workerId);
        return buildingQuantity != null ? new Result(true, 200,buildingQuantity)
                : new Result(false,500);
    }

    @GetMapping("/amount")
    @Operation(summary = "送水员根据time不同查询不同时间前的营业额(已送达)")
    public Result selectAmountByTime(@RequestParam("workerId") String workerId,
                                     @RequestParam(name = "time", required = false) String time) {
        if (time == null || time.isEmpty() || time.equals("total")) {
            Double totalAmount = orderService.selectTotalAmount(workerId);
            return totalAmount != null ? new Result(true, totalAmount)
                    : new Result(false);
        }
        if (time.equals("day")) {
            Double todayAmount = orderService.selectTodayAmount(workerId);
            return todayAmount != null ? new Result(true, todayAmount)
                    : new Result(false);
        }
        if (time.equals("week")) {
            Double weeklyAmount = orderService.selectWeeklyAmount(workerId);
            return weeklyAmount != null ? new Result(true, weeklyAmount)
                    : new Result(false);
        }
        if (time.equals("month")) {
            Double monthlyAmount = orderService.selectMonthlyAmount(workerId);
            return monthlyAmount != null ? new Result(true, monthlyAmount)
                    : new Result(false);
        }
        return new Result(false);
    }

    @PutMapping("/update")
    @Operation(summary = "修改订单信息")
    public Result updateOrder(@RequestBody Order order) {
        Integer flag = orderService.updateOrder(order);
        return flag == 1 ? new Result(true, WaterMessage.UPDATE_SUCCESS)
                : new Result(false, WaterMessage.UPDATE_FAILD);
    }

    @GetMapping("/accept/byBuilding")
    @Operation(summary = "送水员按宿舍楼接单")
    public Result acceptOrderByBuilding(@RequestParam("building") String building
            , @RequestParam("workerId") String workerId) {
        if (!orderRateLimitService.tryAcquireAcceptToken(workerId)) {
            return new Result(false, "抢单过于频繁，请稍后重试", 429);
        }
        Result result = orderService.acceptOrderByBuilding(building, workerId);
        if (result.isFlag() && result.getData() instanceof List) {
            @SuppressWarnings("unchecked")
            List<Order> updated = (List<Order>) result.getData();
            for (Order order : updated) {
                orderMessagePublisher.publishCommand(MqConstants.COMMAND_KEY_ACCEPT, buildMessage(order, "ORDER_ACCEPTED"));
            }
            result.setMessage(WaterMessage.ORDER_ACCEPT_SUCCESS);
            result.setData(null);
        }
        return result;
    }

    @GetMapping("/finish/byBuilding")
    @Operation(summary = "送水员按宿舍楼完成订单")
    public Result finishOrderByBuilding(@RequestParam("building") String building
            , @RequestParam("workerId") String workerId) {
        List<Order> orderList = orderService.selectOrderByWorker(workerId, building, 1);
        Integer flag = 0;
        for (Order order : orderList) {
            order.setStateDelivery(WaterConstant.ORDER_DELIVERY_STATE);
            flag = orderService.updateOrder(order);
        }

        // 发送消息到消息队列
        if (flag == 1) {
            for (Order order : orderList) {
                orderMessagePublisher.publishCommand(MqConstants.COMMAND_KEY_FINISH, buildMessage(order, "ORDER_FINISHED"));
            }
        }
        return flag == 1 ? new Result(true, WaterMessage.ORDER_FINISH_SUCCESS)
                : new Result(false, WaterMessage.ORDER_FINISH_FAILD);
    }

    @GetMapping("/accept/async")
    @Operation(summary = "异步抢单（消息队列处理）")
    public Result acceptOrderByBuildingAsync(@RequestParam("building") String building
            , @RequestParam("workerId") String workerId) {
        if (!orderRateLimitService.tryAcquireAcceptToken(workerId)) {
            return new Result(false, "抢单过于频繁，请稍后重试", 429);
        }
        OrderEventMessage message = new OrderEventMessage();
        message.setMessageId(UUID.randomUUID().toString());
        message.setEventType("ORDER_ACCEPTED");
        message.setBuilding(building);
        message.setWorkerId(workerId);
        message.setEventTime(new Date());
        orderMessagePublisher.publishCommand(MqConstants.COMMAND_KEY_ACCEPT, message);
        return new Result(true, "抢单已排队处理");
    }

    private OrderEventMessage buildMessage(Order order, String eventType) {
        OrderEventMessage message = new OrderEventMessage();
        message.setMessageId(UUID.randomUUID().toString());
        message.setEventType(eventType);
        message.setOrderId(order.getOrderId());
        message.setUserId(order.getUserId());
        message.setWorkerId(order.getWorkerId());
        message.setBuilding(order.getBuilding());
        message.setQuantity(order.getQuantity());
        message.setEventTime(new Date());
        return message;
    }
}
