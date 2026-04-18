package com.example.order.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.common.tools.DistanceUtils;
import com.example.common.tools.Result;
import com.example.feign.clients.BuildingClient;
import com.example.feign.clients.UserClient;
import com.example.feign.entity.Building;
import com.example.feign.entity.User;
import com.example.order.dto.BuildingOrder;
import com.example.order.entity.Order;
import com.example.common.constant.WaterConstant;
import com.example.common.constant.WaterMessage;
import com.example.order.mapper.OrderMapper;
import com.example.order.service.OrderService;
import com.example.order.service.ProductGuardService;
import com.example.order.service.StationStockService;
import com.example.common.tools.UuidTool;
import com.netflix.hystrix.contrib.javanica.annotation.DefaultProperties;
import com.netflix.hystrix.contrib.javanica.annotation.HystrixCommand;
import com.netflix.hystrix.contrib.javanica.annotation.HystrixProperty;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import org.redisson.api.RLock;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
@DefaultProperties(defaultFallback = "defaultFallOutMethod")
public class OrderServiceImpl extends ServiceImpl<OrderMapper, Order> implements OrderService {
    @Autowired
    private OrderMapper orderMapper;
    @Autowired
    private UserClient userClient;
    @Autowired
    private BuildingClient buildingClient;
    @Autowired
    private ProductGuardService productGuardService;
    @Autowired
    private StationStockService stationStockService;
    @Autowired
    private RedissonClient redissonClient;

    private static final String waitTime="2000";
    public Result defaultFallOutMethod()
    {
        return new Result(false, WaterMessage.FALL_BACK,500);
    }

    /**
     * 通过传入Order里的open_id(用户id)拿到用户注册时的buiding和dormi宿舍信息
     * 添加当前时间作为订单创建时间
     * 添加uuid设置的orderId
     * @param order
     * @return
     */
    @Override
    @HystrixCommand(
            threadPoolKey = "orderCommandPool",
            threadPoolProperties = {
                    @HystrixProperty(name = "coreSize", value = "20"),
                    @HystrixProperty(name = "maxQueueSize", value = "200")
            },
            commandProperties = {
                    @HystrixProperty(name = "execution.isolation.strategy", value = "THREAD"),
                    @HystrixProperty(name = "execution.isolation.thread.timeoutInMilliseconds", value = "10000")
            }
    )
    public Result createOrder(Order order) {
        if (order.getOrderId() == null || order.getOrderId().isEmpty()) {
            order.setOrderId(UuidTool.getUUID32());
        }
        return createOrderCore(order);
    }

    @Override
    public Result createOrderFromPersist(Order order) {
        return createOrderCore(order);
    }

    /**
     * 创建订单核心：用户校验、水品校验、Lua 扣减水站库存、落库。
     */
    private Result createOrderCore(Order order) {
        Result<User> result = userClient.selectById(order.getUserId());
        if (!result.isFlag()) {
            return result;
        }
        User user = result.getData();
        if (order.getBuilding() == null || order.getBuilding().isEmpty()) {
            order.setBuilding(user.getBuilding());
        }
        if (order.getDormi() == null || order.getDormi().isEmpty()) {
            order.setDormi(user.getDormi());
        }
        if (order.getProductId() == null || order.getProductId().isEmpty()) {
            order.setProductId("DEFAULT_WATER");
        }
        if (order.getStationId() == null || order.getStationId().isEmpty()) {
            order.setStationId("default");
        }
        if (!productGuardService.isOrderable(order.getProductId())) {
            return new Result(false, "商品不存在或已下架", 400);
        }
        int qty = order.getQuantity() == null ? 0 : order.getQuantity();
        if (!stationStockService.tryDeduct(order.getStationId(), order.getProductId(), qty)) {
            return new Result(false, "水站库存不足", 400);
        }
        Date nowtime = new Date();
        order.setTime(nowtime);
        try {
            int insert = orderMapper.insert(order);
            if (insert != 1) {
                stationStockService.restore(order.getStationId(), order.getProductId(), qty);
                return new Result(false);
            }
            return new Result(true);
        } catch (RuntimeException ex) {
            stationStockService.restore(order.getStationId(), order.getProductId(), qty);
            throw ex;
        }
    }

    @Override
    public Result acceptOrderByBuilding(String building, String workerId) {
        List<Order> orderList = selectOrder(building, 0);
        List<Order> updated = new ArrayList<>();
        for (Order order : orderList) {
            RLock lock = redissonClient.getLock("order:lock:" + order.getOrderId());
            try {
                if (lock.tryLock(30, -1, TimeUnit.SECONDS)) {
                    order.setWorkerId(workerId);
                    order.setStateDelivery(WaterConstant.ORDER_ACCEPT_STATE);
                    if (updateOrder(order) == 1) {
                        updated.add(order);
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                if (lock.isHeldByCurrentThread()) {
                    lock.unlock();
                }
            }
        }
        if (!updated.isEmpty()) {
            return new Result(true, updated);
        }
        return new Result(false, WaterMessage.ORDER_ACCEPT_FAILD);
    }

    @Override
    public Order selectOrderByOrderId(String orderId) {
        LambdaQueryWrapper<Order> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Order::getOrderId,orderId);
        return orderMapper.selectOne(queryWrapper);
    }

    @Override
    public List<Order> selectOrder(String building,Integer delivery) {
        LambdaQueryWrapper<Order> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Order::getBuilding,building)
                .eq(Order::getStateDelivery,delivery);
        return orderMapper.selectList(queryWrapper);
    }

    /**
     * 用户查看订单：通过用户id查询订单
     * delivery为空查询所有，delivery不为空查询对应状态的订单
     * @param userId
     * @param delivery
     * @return
     *
     * TODO 服务熔断：
     * circuitBreaker.enabled: 设置熔断器是否启用，值为 true 表示启用熔断器。
     * circuitBreaker.requestVolumeThreshold: 设置熔断器在滚动时间窗口中，必须满足的请求数量阈值，达到该阈值才会进行熔断策略的判断。在这里设置为 10，表示在滚动时间窗口内至少有 10 个请求才会触发熔断策略的判断。
     * circuitBreaker.sleepWindowInMilliseconds: 设置熔断器进入半开状态的时间窗口长度，当熔断器开启后，在该时间窗口结束后，会进入半开状态，允许部分请求通过以尝试恢复依赖服务。
     * circuitBreaker.errorThresholdPercentage: 设置错误百分比阈值，当在滚动时间窗口内的请求错误百分比超过该阈值时，熔断器将会触发熔断。
     */
    @Override
    @HystrixCommand(commandProperties = {
            @HystrixProperty(name ="circuitBreaker.enabled",value ="true"),
            @HystrixProperty(name ="circuitBreaker.requestVolumeThreshold",value ="10"),
            @HystrixProperty(name ="circuitBreaker.sleepWindowInMilliseconds",value ="10000"),
            @HystrixProperty(name ="circuitBreaker.errorThresholdPercentage",value ="60"),
            })
    public Result selectOrderByUserId(String userId,Integer delivery) {
        if (userId==null||userId.isEmpty())
        {
            throw new RuntimeException("用户id不能为空");
        }
        LambdaQueryWrapper<Order> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Order::getUserId, userId);
        if (delivery != null) {
            queryWrapper.eq(Order::getStateDelivery, delivery);
        }
        List<Order> orderList = orderMapper.selectList(queryWrapper);
        return orderList != null ? new Result(true, orderList)
                : new Result(false);
    }

    /**
     * 送水员查看订单
     * 都为空查询所有，任一不为空查询特定
     * @param building
     * @param delivery
     * @return
     */
    @Override
    public List<Order> selectOrderByWorker(String workerId,String building, Integer delivery) {
        LambdaQueryWrapper<Order> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Order::getWorkerId, workerId);
        if (building != null) {
            queryWrapper.eq(Order::getBuilding, building);
        }
        if (delivery != null) {
            queryWrapper.eq(Order::getStateDelivery, delivery);
        }
        return orderMapper.selectList(queryWrapper);
    }

    /**
     * 送水员查看今日的所有订单
     * @return
     */
    @Override
    @HystrixCommand(commandProperties = {@HystrixProperty(
            name = "execution.isolation.thread.timeoutInMilliseconds",value = waitTime)})
    public Result selectOrderToday(String workerId,Integer delivery){
        QueryWrapper<Order> queryWrapper = new QueryWrapper<>();
        queryWrapper.apply(true, "TO_DAYS(NOW())-TO_DAYS(time) = 0")
                .eq("worker_id",workerId);
        if (delivery!=null)
        {
            queryWrapper.eq("state_delivery",delivery);
        }
        List<Order> orderList = orderMapper.selectList(queryWrapper);
        return orderList != null ? new Result(true, orderList)
                : new Result(false);
    }

    /**
     * TODO 返回前端一个按宿舍楼分开的二级列表，使用DTO
     * @return
     */
    @Override
    public List<BuildingOrder> selectBuildingOrderNotAccept(String lng,String lat) {
        List<BuildingOrder> buildingOrderList = new ArrayList<>();

        Result<List<Building>> result = buildingClient.selectAllBuildings();
        List<Building> buildingList = result.getData();
        for (Building building : buildingList) {

            LambdaQueryWrapper<Order> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(Order::getBuilding,building.getName())
                    .eq(Order::getStateDelivery,0);
            List<Order> orderList = orderMapper.selectList(queryWrapper);
            if(orderList!=null && !orderList.isEmpty())
            {
                BuildingOrder buildingOrder = new BuildingOrder();
                buildingOrder.setBuilding(building.getName());
                buildingOrder.setOrder(orderList);

                Integer totalQuantity=0;
                for (Order order : orderList) {
                    totalQuantity+=order.getQuantity();
                }
                buildingOrder.setTotalQuantity(totalQuantity);

                if (building.getLng()!=null && building.getLat()!=null)
                {
                    Integer distance = DistanceUtils.getDistanceStr(lng,lat,
                            building.getLng(),building.getLat());
                    buildingOrder.setDistance(distance);
                }
                buildingOrderList.add(buildingOrder);
            }
        }
        return buildingOrderList;
    }

    @Override
    public List<BuildingOrder> selectBuildingOrderNotDelivery(String workerId,String lng,String lat) {
        List<BuildingOrder> buildingOrderList = new ArrayList<>();

        Result<List<Building>> result = buildingClient.selectAllBuildings();
        List<Building> buildingList = result.getData();
        for (Building building : buildingList) {

            LambdaQueryWrapper<Order> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(Order::getBuilding,building.getName())
                    .eq(Order::getStateDelivery,1)
                    .eq(Order::getWorkerId,workerId);
            List<Order> orderList = orderMapper.selectList(queryWrapper);
            if(orderList!=null && !orderList.isEmpty())
            {
                BuildingOrder buildingOrder = new BuildingOrder();
                buildingOrder.setBuilding(building.getName());
                buildingOrder.setOrder(orderList);

                Integer totalQuantity=0;
                for (Order order : orderList) {
                    totalQuantity+=order.getQuantity();
                }
                buildingOrder.setTotalQuantity(totalQuantity);

                if (building.getLng()!=null && building.getLat()!=null)
                {
                    Integer distance = DistanceUtils.getDistanceStr(lng,lat,
                            building.getLng(),building.getLat());
                    buildingOrder.setDistance(distance);
                }
                buildingOrderList.add(buildingOrder);
            }
        }
        return buildingOrderList;
    }

    /**
     * 送水员查看水桶数
     * 都为空查询所有，任一不为空查询特定
     * @param building
     * @param delivery
     * @return
     */
    @Override
    public Integer selectQuantity(String workerId, String building, Integer delivery) {
        QueryWrapper<Order> queryWrapper = new QueryWrapper<>();
        queryWrapper.select("sum(quantity) as sumQuantity")
                .eq("worker_id", workerId);

        if (building != null) {
            queryWrapper.eq("building", building);
        }

        if (delivery != null) {
            queryWrapper.eq("state_delivery", delivery);
        }

        List<Map<String, Object>> maxMap = orderMapper.selectMaps(queryWrapper);
        if (maxMap == null || maxMap.isEmpty() || maxMap.get(0) == null) {
            return 0;
        } else {
            BigDecimal bigDecimal = (BigDecimal) maxMap.get(0).get("sumQuantity");
            return bigDecimal != null ? bigDecimal.intValue() : 0;
        }
    }


    /**
     * 查询今日售出且送达桶数
     * @return
     */
    @Override
    public Integer selectQuantityToday(String workerId) {
        QueryWrapper<Order> queryWrapper = new QueryWrapper<>();
        queryWrapper.select("sum(quantity) as sumQuantity")
                .apply(true, "TO_DAYS(NOW())-TO_DAYS(time) = 0")
                .eq("state_delivery",WaterConstant.ORDER_DELIVERY_STATE)
                .eq("worker_id",workerId);

        List<Map<String, Object>> maxMap = orderMapper.selectMaps(queryWrapper);
        if(maxMap.get(0)==null||maxMap==null)
        {
            return 0;
        }
        else {
            BigDecimal bigDecimal= (BigDecimal) maxMap.get(0).get("sumQuantity");
            return Integer.parseInt(bigDecimal.toString());
        }
    }

    /**
     * 送水员查看今日营业额(已送达)
     * @return
     */
    @Override
    public Double selectTodayAmount(String workerId) {
        QueryWrapper<Order> queryWrapper = new QueryWrapper<>();
        queryWrapper.select("sum(quantity) as sumQuantity")
                .apply(true, "TO_DAYS(NOW())-TO_DAYS(time) = 0")
                .eq("state_delivery",WaterConstant.ORDER_DELIVERY_STATE)
                .eq("worker_id",workerId);
        List<Map<String, Object>> maxMap = orderMapper.selectMaps(queryWrapper);

        if(maxMap.get(0)==null||maxMap==null)
        {
            return 0.0;
        }
        else {
            BigDecimal bigDecimal = (BigDecimal) maxMap.get(0).get("sumQuantity");
            Integer totalquantity = Integer.parseInt(bigDecimal.toString());
            Double totalAmount = totalquantity* WaterConstant.WATER_PRICE;
            return totalAmount;
        }
    }

    @Override
    public Double selectWeeklyAmount(String workerId) {

        Integer weeklyOrderQuantity = orderMapper.getWeeklyOrderQuantity(workerId);
        Double weeklyAmount = weeklyOrderQuantity*WaterConstant.WATER_PRICE;
        return weeklyAmount;
    }

    @Override
    public Double selectMonthlyAmount(String workerId) {
        Integer monthlyOrderQuantity = orderMapper.getMonthlyOrderQuantity(workerId);
        Double MonthlyAmount = monthlyOrderQuantity*WaterConstant.WATER_PRICE;
        return MonthlyAmount;
    }

    /**
     * 送水员查看总营业额(已送达)
     * @return
     */
    @Override
    public Double selectTotalAmount(String workerId) {
        QueryWrapper<Order> queryWrapper = new QueryWrapper<>();
        queryWrapper.select("sum(quantity) as sumQuantity")
                .eq("state_delivery",WaterConstant.ORDER_DELIVERY_STATE)
                .eq("worker_id",workerId);
        List<Map<String, Object>> maxMap = orderMapper.selectMaps(queryWrapper);

        if(maxMap.get(0)==null||maxMap==null)
        {
            return 0.0;
        }
        else {
            BigDecimal bigDecimal = (BigDecimal) maxMap.get(0).get("sumQuantity");
            Integer totalquantity = Integer.parseInt(bigDecimal.toString());
            Double totalAmount = totalquantity* WaterConstant.WATER_PRICE;
            return totalAmount;
        }
    }

    @Override
    public Integer updateOrder(Order order) {
        int updateById = orderMapper.updateById(order);
        return updateById;
    }

}
