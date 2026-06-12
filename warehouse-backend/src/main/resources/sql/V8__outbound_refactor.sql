-- V8: 出库模块重构 - 看板状态5态 + 出库单状态3态
-- 注意：旧版看板status: 0=待入库 1=已入库 2=已出库
--       新版看板status: 0=待入库 1=在库可用 2=待出库(锁定) 3=已出库 4=封存
--       旧版出库单status: 0=待出库 1=部分出库 2=已完成 3=作废
--       新版出库单status: 0=进行中 1=已完成 2=作废

USE warehouse_db;

-- ============================================================
-- 一、kanban 表变更
-- ============================================================

-- 1. 新增出库单ID字段
ALTER TABLE kanban
    ADD COLUMN outbound_order_id BIGINT DEFAULT NULL COMMENT '锁定出库单ID(待出库时填充)',
    ADD INDEX idx_outbound_order_id (outbound_order_id);

-- 2. 迁移旧已出库数据：旧status=2(已出库) → 新status=3(已出库)
UPDATE kanban SET status = 3 WHERE status = 2;

-- 3. 更新 status 列注释
ALTER TABLE kanban
    MODIFY COLUMN status TINYINT DEFAULT 0 COMMENT '0待入库1在库可用2待出库锁定3已出库4封存';

-- ============================================================
-- 二、outbound_order 表变更
-- ============================================================

-- 1. 迁移旧数据：部分出库(1)->进行中(0), 已完成(2)->已完成(1), 作废(3)->作废(2)
UPDATE outbound_order SET status = 0 WHERE status = 1;
UPDATE outbound_order SET status = 1 WHERE status = 2;
UPDATE outbound_order SET status = 2 WHERE status = 3;

-- 2. 更新状态注释
ALTER TABLE outbound_order
    MODIFY COLUMN status TINYINT DEFAULT 0 COMMENT '0=未出库 1=部分出库 2=已出库 3=作废';

-- ============================================================
-- 三、outbound_scan 表增加库位信息
-- ============================================================

ALTER TABLE outbound_scan
    ADD COLUMN warehouse_area_id   BIGINT       DEFAULT NULL COMMENT '出库时所在的库区ID',
    ADD COLUMN warehouse_area_name VARCHAR(100) DEFAULT NULL COMMENT '出库时所在的库区名称';
