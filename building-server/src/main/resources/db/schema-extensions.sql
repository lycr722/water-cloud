-- 水品字典表（按需执行）
CREATE TABLE IF NOT EXISTS t_water_product (
    product_id   VARCHAR(64)  NOT NULL PRIMARY KEY COMMENT '商品/水品ID',
    name         VARCHAR(128) NOT NULL,
    status       TINYINT      NOT NULL DEFAULT 1 COMMENT '1上架 0下架',
    gmt_create   DATETIME     DEFAULT CURRENT_TIMESTAMP,
    gmt_modified DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO t_water_product (product_id, name, status)
VALUES ('DEFAULT_WATER', '默认桶装水', 1)
ON DUPLICATE KEY UPDATE name = VALUES(name), status = VALUES(status);
