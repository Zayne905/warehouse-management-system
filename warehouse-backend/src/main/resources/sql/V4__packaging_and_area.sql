-- V4: 零件包装容量、默认库区、入库单明细箱数

USE warehouse_db;

-- 1. part 表加包装容量和默认库区
ALTER TABLE part
    ADD COLUMN package_capacity INT DEFAULT 1 COMMENT '每箱装的零件个数',
    ADD COLUMN warehouse_area_id BIGINT DEFAULT NULL COMMENT '默认库区ID';

-- 2. inbound_order_detail 表加箱数
ALTER TABLE inbound_order_detail
    ADD COLUMN box_count INT DEFAULT 0 COMMENT '箱数';
