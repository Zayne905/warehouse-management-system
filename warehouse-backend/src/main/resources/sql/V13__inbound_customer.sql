-- V13: 入库单增加客户字段
USE warehouse_db;

ALTER TABLE inbound_order
    ADD COLUMN customer_id   BIGINT       DEFAULT NULL COMMENT '客户ID' AFTER supplier_name,
    ADD COLUMN customer_name VARCHAR(100) DEFAULT NULL COMMENT '客户名称(冗余)' AFTER customer_id;
