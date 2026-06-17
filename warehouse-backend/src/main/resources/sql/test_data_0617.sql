-- ============================================================
-- 测试数据脚本：清除旧数据 + 生成入库/出库测试用例
-- ============================================================
USE warehouse_db;

-- ============================================================
-- 一、清除业务数据（保留主数据：supplier, part, warehouse_area）
-- ============================================================
DELETE FROM outbound_scan;
DELETE FROM outbound_order_detail;
DELETE FROM outbound_order;
DELETE FROM scan_record;
DELETE FROM inbound_order_detail;
DELETE FROM inbound_order;
DELETE FROM kanban;
DELETE FROM repack_relation;
DELETE FROM repack_order_detail;
DELETE FROM repack_order;

-- 重置自增ID
ALTER TABLE inbound_order AUTO_INCREMENT = 1;
ALTER TABLE inbound_order_detail AUTO_INCREMENT = 1;
ALTER TABLE scan_record AUTO_INCREMENT = 1;
ALTER TABLE outbound_order AUTO_INCREMENT = 1;
ALTER TABLE outbound_order_detail AUTO_INCREMENT = 1;
ALTER TABLE outbound_scan AUTO_INCREMENT = 1;
ALTER TABLE kanban AUTO_INCREMENT = 1;
ALTER TABLE repack_order AUTO_INCREMENT = 1;
ALTER TABLE repack_order_detail AUTO_INCREMENT = 1;
ALTER TABLE repack_relation AUTO_INCREMENT = 1;

-- ============================================================
-- 二、入库测试数据
-- ============================================================

-- 入库单1：深圳电子 → P001(电阻器) + P002(电容器) + P003(M3螺丝)
-- P001: plannedQty=750, packageCapacity=200 → 箱数 3.75 → 4箱(200+200+200+150尾箱)
-- P002: plannedQty=1200, packageCapacity=500 → 箱数 2.4 → 3箱(500+500+200尾箱)
-- P003: plannedQty=700, packageCapacity=300 → 箱数 2.33 → 3箱(300+300+100尾箱)
INSERT INTO inbound_order (id, order_no, supplier_id, supplier_name, order_number, status, remark, create_time)
VALUES (1, 'R20260617001', 1, '深圳电子科技有限公司', 'PO-2026-0617', 2, '测试入库单-带尾箱看板', '2026-06-17 08:00:00');

INSERT INTO inbound_order_detail (inbound_order_id, part_id, part_code, part_name, unit, planned_qty, actual_qty, warehouse_area_id, box_count, line_no)
VALUES
(1, 1, 'P001', '电阻器 10KΩ', '个', 750, 750, 1, 3.75, 1),
(1, 2, 'P002', '电容器 100μF', '个', 1200, 1200, 1, 2.40, 2),
(1, 3, 'P003', 'M3螺丝', '个', 700, 700, 2, 2.33, 3);

-- P001 看板: 4箱 (200,200,200,150尾箱)
INSERT INTO kanban (kanban_no, inbound_order_id, inbound_order_no, part_id, part_code, part_name, supplier_name, quantity, original_qty, box_seq, warehouse_area_id, warehouse_area_name, status, create_time)
VALUES
('R-2026-06-17-R20260617001-P001C-0', 1, 'R20260617001', 1, 'P001', '电阻器 10KΩ', '深圳电子科技有限公司', 200, 200, 0, 1, 'A区-电子零件', 1, '2026-06-17 08:00:00'),
('R-2026-06-17-R20260617001-P001C-1', 1, 'R20260617001', 1, 'P001', '电阻器 10KΩ', '深圳电子科技有限公司', 200, 200, 1, 1, 'A区-电子零件', 1, '2026-06-17 08:00:01'),
('R-2026-06-17-R20260617001-P001C-2', 1, 'R20260617001', 1, 'P001', '电阻器 10KΩ', '深圳电子科技有限公司', 200, 200, 2, 1, 'A区-电子零件', 1, '2026-06-17 08:00:02'),
('R-2026-06-17-R20260617001-P001C-3', 1, 'R20260617001', 1, 'P001', '电阻器 10KΩ', '深圳电子科技有限公司', 150, 200, 3, 1, 'A区-电子零件', 1, '2026-06-17 08:00:03');

-- P002 看板: 3箱 (500,500,200尾箱)
INSERT INTO kanban (kanban_no, inbound_order_id, inbound_order_no, part_id, part_code, part_name, supplier_name, quantity, original_qty, box_seq, warehouse_area_id, warehouse_area_name, status, create_time)
VALUES
('R-2026-06-17-R20260617001-P002C-0', 1, 'R20260617001', 2, 'P002', '电容器 100μF', '深圳电子科技有限公司', 500, 500, 0, 1, 'A区-电子零件', 1, '2026-06-17 08:10:00'),
('R-2026-06-17-R20260617001-P002C-1', 1, 'R20260617001', 2, 'P002', '电容器 100μF', '深圳电子科技有限公司', 500, 500, 1, 1, 'A区-电子零件', 1, '2026-06-17 08:10:01'),
('R-2026-06-17-R20260617001-P002C-2', 1, 'R20260617001', 2, 'P002', '电容器 100μF', '深圳电子科技有限公司', 200, 500, 2, 1, 'A区-电子零件', 1, '2026-06-17 08:10:02');

-- P003 看板: 3箱 (300,300,100尾箱)
INSERT INTO kanban (kanban_no, inbound_order_id, inbound_order_no, part_id, part_code, part_name, supplier_name, quantity, original_qty, box_seq, warehouse_area_id, warehouse_area_name, status, create_time)
VALUES
('R-2026-06-17-R20260617001-P003C-0', 1, 'R20260617001', 3, 'P003', 'M3螺丝', '上海精密零件有限公司', 300, 300, 0, 2, 'B区-五金配件', 1, '2026-06-17 08:20:00'),
('R-2026-06-17-R20260617001-P003C-1', 1, 'R20260617001', 3, 'P003', 'M3螺丝', '上海精密零件有限公司', 300, 300, 1, 2, 'B区-五金配件', 1, '2026-06-17 08:20:01'),
('R-2026-06-17-R20260617001-P003C-2', 1, 'R20260617001', 3, 'P003', 'M3螺丝', '上海精密零件有限公司', 100, 300, 2, 2, 'B区-五金配件', 1, '2026-06-17 08:20:02');

-- 入库单2：上海精密 → P003(M3螺丝) 补充库存
INSERT INTO inbound_order (id, order_no, supplier_id, supplier_name, order_number, status, remark, create_time)
VALUES (2, 'R20260617002', 2, '上海精密零件有限公司', 'PO-2026-0618', 2, '补充M3螺丝库存', '2026-06-17 09:00:00');

INSERT INTO inbound_order_detail (inbound_order_id, part_id, part_code, part_name, unit, planned_qty, actual_qty, warehouse_area_id, box_count, line_no)
VALUES (2, 3, 'P003', 'M3螺丝', '个', 600, 600, 2, 2, 1);

INSERT INTO kanban (kanban_no, inbound_order_id, inbound_order_no, part_id, part_code, part_name, supplier_name, quantity, original_qty, box_seq, warehouse_area_id, warehouse_area_name, status, create_time)
VALUES
('R-2026-06-17-R20260617002-P003C-0', 2, 'R20260617002', 3, 'P003', 'M3螺丝', '上海精密零件有限公司', 300, 300, 0, 2, 'B区-五金配件', 1, '2026-06-17 09:00:00'),
('R-2026-06-17-R20260617002-P003C-1', 2, 'R20260617002', 3, 'P003', 'M3螺丝', '上海精密零件有限公司', 300, 300, 1, 2, 'B区-五金配件', 1, '2026-06-17 09:00:01');

-- ============================================================
-- 三、出库测试数据
-- ============================================================

-- 出库单1：多零件出库，测试零件进度可视化
-- P001: plannedQty=320，可用看板: 200+200+200+150=750
-- P003: plannedQty=500，可用看板: 300+300+100+300+300=1300
INSERT INTO outbound_order (id, order_no, status, remark, customer_name, create_user_id, create_time)
VALUES (1, 'C20260617001', 0, '测试多零件出库-零件进度可视化', '测试客户A', 1, '2026-06-17 10:00:00');

INSERT INTO outbound_order_detail (outbound_order_id, part_id, part_code, part_name, unit, planned_qty, actual_qty, box_count, line_no)
VALUES
(1, 1, 'P001', '电阻器 10KΩ', '个', 320, 0, 0, 1),
(1, 3, 'P003', 'M3螺丝', '个', 500, 0, 0, 2);

-- 出库单2：单零件出库，测试非FIFO出库 + 超量拆分
-- P001: plannedQty=320
INSERT INTO outbound_order (id, order_no, status, remark, customer_name, create_user_id, create_time)
VALUES (2, 'C20260617002', 0, '测试非FIFO出库+超量拆分', '测试客户B', 1, '2026-06-17 10:30:00');

INSERT INTO outbound_order_detail (outbound_order_id, part_id, part_code, part_name, unit, planned_qty, actual_qty, box_count, line_no)
VALUES (2, 1, 'P001', '电阻器 10KΩ', '个', 320, 0, 0, 1);

SELECT '测试数据生成完毕' AS result;
SELECT '--- 入库 ---' AS info;
SELECT '入库单1: R20260617001 深圳电子 P001(750) P002(1200) P003(700)' AS description;
SELECT '入库单2: R20260617002 上海精密 P003(600)' AS description;
SELECT '--- 出库 ---' AS info;
SELECT '出库单1: C20260617001 多零件(P001=320, P003=500) - 测试进度可视化' AS description;
SELECT '出库单2: C20260617002 单零件(P001=320) - 测试非FIFO+超量拆分' AS description;
SELECT '--- 看板 ---' AS info;
SELECT 'P001 可用看板: 200+200+200+150 = 750件 (4箱)' AS description;
SELECT 'P002 可用看板: 500+500+200 = 1200件 (3箱)' AS description;
SELECT 'P003 可用看板: 300+300+100+300+300 = 1300件 (5箱)' AS description;
