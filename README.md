# Water Order Cloud

## 项目简介

Water Order Cloud 是一个基于Spring Cloud微服务架构的水订单管理系统，支持用户下单、工人抢单、楼栋管理和库存管理等核心业务。项目采用分布式设计，集成了高可用、高并发防护机制，确保系统稳定性和性能。

## 技术栈

- **框架**：Spring Boot, Spring Cloud
- **服务注册与发现**：Nacos, Eureka
- **API网关**：Spring Cloud Gateway
- **服务调用**：OpenFeign
- **熔断降级**：Hystrix
- **缓存**：Redis, Redisson
- **消息队列**：RabbitMQ
- **数据库**：MySQL
- **监控**：Hystrix Dashboard
- **构建工具**：Maven
- **其他**：Docker (可选)

## 架构概述

项目采用微服务架构，分为多个模块：

- **gateway-server**：API网关，负责请求路由、鉴权、限流和跨域处理。
- **eureka-server / eureka-server-ha**：服务注册中心，提供高可用服务发现。
- **nacos-config**：配置中心，管理分布式配置。
- **user-server**：用户服务，处理用户相关业务。
- **order-server**：订单服务，核心订单管理、下单抢单逻辑。
- **worker-server**：工人服务，处理工人抢单和派单。
- **building-server**：楼栋服务，管理楼栋字典和水品信息。
- **cache-sync-server**：缓存同步服务，监听MySQL BinLog实现缓存一致性。
- **feign-api**：Feign客户端接口定义。
- **common**：公共模块，共享工具类和实体。
- **hystrix-dashboard**：监控仪表板，查看熔断状态。

## 核心功能特性

### 多维防护
- **Gateway全局过滤器**：实现请求路由、跨域、流量清洗与鉴权，结合Nacos动态维护路由。
- **分布式限流**：使用Redisson RRateLimiter对下单与抢单接口实施限流。
- **服务隔离与熔断**：集成Hystrix线程池隔离（舱壁模式）和熔断降级，防止服务雪崩。

### 缓存优化
- **定时预热**：Spring定时任务预热高频信息（楼栋字典、水品信息）。
- **防穿透机制**：Redisson布隆过滤器拦截非法商品ID，Redis Set维护已下架商品白名单。
- **缓存雪崩防护**：随机TTL策略。
- **缓存击穿解决**：逻辑过期与异步刷新，配合Redis惰性删除。
- **最终一致性**：MySQL BinLog监听机制配合RabbitMQ异步失效缓存。

### 订单业务
- **原子性库存扣减**：Lua脚本保障水站库存绝对原子性。
- **分布式锁**：Redisson分布式锁细化至订单级，Watchdog自动续期，防止重复派单。

### 消息架构
- **异步削峰落库**：RabbitMQ对下单与抢单核心链路异步处理。
- **路由分离**：Direct/Topic交换机实现精准指令与领域事件广播。
- **高可靠传递**：Publisher Confirms + Mandatory Returns + 手动Ack + DLQ。

## 快速开始

### 环境要求
- JDK 8+
- Maven 3.6+
- MySQL 5.7+
- Redis 5.0+
- RabbitMQ 3.8+
- Nacos 2.0+ (可选，用于配置和服务发现)

### 安装步骤
1. **克隆项目**：
   ```bash
   git clone https://github.com/lycr722/water-cloud.git
   cd water-order-cloud
   ```

2. **配置数据库**：
   - 创建MySQL数据库。
   - 执行各模块的`db/schema-extensions.sql`初始化表结构。

3. **配置Redis和RabbitMQ**：
   - 启动Redis和RabbitMQ服务。
   - 更新各模块的`application.yml`中的连接配置。

4. **启动服务**：
   - 按顺序启动注册中心（eureka-server）、配置中心（nacos-config）、网关（gateway-server），然后启动业务服务（order-server、user-server等）。
   - 示例启动命令：
     ```bash
     mvn clean install
     mvn spring-boot:run -pl eureka-server
     mvn spring-boot:run -pl gateway-server
     # 依次启动其他服务
     ```

5. **访问应用**：
   - API网关：http://localhost:8080
   - Hystrix Dashboard：http://localhost:8081/hystrix

### Docker部署 (可选)
- 使用Docker Compose启动依赖服务：
  ```bash
  docker-compose up -d
  ```

## 贡献指南

欢迎贡献代码！请遵循以下步骤：
1. Fork项目。
2. 创建特性分支：`git checkout -b feature/your-feature`。
3. 提交更改：`git commit -m 'Add some feature'`。
4. 推送分支：`git push origin feature/your-feature`。
5. 提交Pull Request。

## 许可证

本项目采用MIT许可证。详见[LICENSE](LICENSE)文件。

## 联系我们

如有问题，请提交Issue或联系维护者。
