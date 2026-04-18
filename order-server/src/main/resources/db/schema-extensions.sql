-- 订单扩展字段（按需执行）
ALTER TABLE t_order ADD COLUMN product_id VARCHAR(64) NULL COMMENT '水品/商品ID' AFTER quantity;
ALTER TABLE t_order ADD COLUMN station_id VARCHAR(64) NULL COMMENT '水站ID' AFTER product_id;
