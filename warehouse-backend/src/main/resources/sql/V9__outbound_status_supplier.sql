-- V9: 出库单状态4态 + 增加客户名称
-- 旧: 0=进行中 1=已完成 2=作废
-- 新: 0=未出库 1=部分出库 2=已出库 3=作废

USE warehouse_db;

-- 1. 迁移状态值：作废先挪到3，已完成挪到2
UPDATE outbound_order SET status = 3 WHERE status = 2;
UPDATE outbound_order SET status = 2 WHERE status = 1;
-- status=0 保持不变

-- 2. 根据明细判断哪些"进行中"的应该是"部分出库"
--    如果明细有 actualQty > 0 且不是全部满足 plannedQty，则为部分出库
UPDATE outbound_order o
    JOIN (SELECT outbound_order_id,
                 SUM(actual_qty)    AS total_actual,
                 SUM(planned_qty)   AS total_planned
          FROM outbound_order_detail
          GROUP BY outbound_order_id) d ON o.id = d.outbound_order_id
SET o.status = 1
WHERE o.status = 0
  AND d.total_actual > 0
  AND d.total_actual < d.total_planned;

-- 如果全部满足，直接标为已出库
UPDATE outbound_order o
    JOIN (SELECT outbound_order_id,
                 SUM(actual_qty)    AS total_actual,
                 SUM(planned_qty)   AS total_planned
          FROM outbound_order_detail
          GROUP BY outbound_order_id) d ON o.id = d.outbound_order_id
SET o.status = 2
WHERE o.status = 0
  AND d.total_actual > 0
  AND d.total_actual >= d.total_planned;

-- 3. 修改列注释
ALTER TABLE outbound_order
    MODIFY COLUMN status TINYINT DEFAULT 0 COMMENT '0=未出库 1=部分出库 2=已出库 3=作废';

-- 4. 增加客户名称字段
ALTER TABLE outbound_order
    ADD COLUMN customer_name VARCHAR(100) DEFAULT NULL COMMENT '客户名称' AFTER remark;

-- 5. 看板表增加出库单号字段
ALTER TABLE kanban
    ADD COLUMN outbound_order_no VARCHAR(20) DEFAULT NULL COMMENT '出库单号(冗余)' AFTER outbound_order_id;
