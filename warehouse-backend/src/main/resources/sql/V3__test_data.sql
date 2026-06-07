-- ============================================================
-- 测试数据：Phase 2 入库管理
-- 先执行 V2__phase2_schema.sql，再执行此文件
-- ============================================================

USE warehouse_db;

-- ============================================================
-- 1. 创建测试用户
-- ============================================================

-- 普通用户 (用于权限测试)
INSERT INTO sys_user (username, password, nickname, enabled, role) VALUES
('user1', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', '测试用户', 1, 'user'),
('user2', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', '仓库操作员', 1, 'user')
ON DUPLICATE KEY UPDATE role = VALUES(role);
-- 密码都是 admin123

-- 管理员 (用于权限对比测试)
-- admin 已存在，确认 role 正确
UPDATE sys_user SET role = 'admin' WHERE username = 'admin';

-- ============================================================
-- 2. 补充供应商数据（如果 V2 种子数据已存在则跳过）
-- ============================================================

-- 额外测试供应商
INSERT INTO supplier (code, name, contact, phone, address) VALUES
('GYS005', '成都精密机械有限公司', '陈经理', '13500135005', '成都市高新区'),
('GYS006', '杭州电子元件厂', '刘厂长', '13400134006', '杭州市滨江区')
ON DUPLICATE KEY UPDATE name = VALUES(name);

-- ============================================================
-- 3. 补充测试物料
-- ============================================================

INSERT INTO part (code, name, unit, spec) VALUES
('P011', 'USB连接线 1m', '根', 'Type-C'),
('P012', '电源适配器 12V2A', '个', 'DC 5.5'),
('P013', '轴承 6205', '个', '深沟球'),
('P014', '密封圈 Φ50', '个', '丁腈橡胶'),
('P015', '标签纸 50x30mm', '卷', '热敏纸')
ON DUPLICATE KEY UPDATE name = VALUES(name);

-- ============================================================
-- 4. 补充供应商-物料关联
-- ============================================================

INSERT INTO supplier_part (supplier_id, part_id)
SELECT s.id, p.id FROM supplier s, part p
WHERE s.code = 'GYS005' AND p.code IN ('P013', 'P014')
ON DUPLICATE KEY UPDATE supplier_id = VALUES(supplier_id);

INSERT INTO supplier_part (supplier_id, part_id)
SELECT s.id, p.id FROM supplier s, part p
WHERE s.code = 'GYS006' AND p.code IN ('P011', 'P012', 'P015')
ON DUPLICATE KEY UPDATE supplier_id = VALUES(supplier_id);

INSERT INTO supplier_part (supplier_id, part_id)
SELECT s.id, p.id FROM supplier s, part p
WHERE s.code = 'GYS001' AND p.code IN ('P011', 'P012')
ON DUPLICATE KEY UPDATE supplier_id = VALUES(supplier_id);

-- ============================================================
-- 5. 创建测试用入库单（各种状态）
-- ============================================================

-- 5.1 未入库状态的入库单 (用于编辑测试)
INSERT INTO inbound_order (order_no, supplier_id, supplier_name, order_number, status, remark, create_user_id)
SELECT 'R20260606001', s.id, s.name, 'PO-2026-001', 0, '测试入库单-未入库', u.id
FROM supplier s, sys_user u
WHERE s.code = 'GYS001' AND u.username = 'admin';

-- 添加入库单明细
INSERT INTO inbound_order_detail (inbound_order_id, part_id, part_code, part_name, unit, planned_qty, actual_qty, warehouse_area_id, line_no)
SELECT io.id, p.id, p.code, p.name, p.unit, 100, 0, wa.id, 1
FROM inbound_order io, part p, warehouse_area wa
WHERE io.order_no = 'R20260606001' AND p.code = 'P001' AND wa.code = 'A';

INSERT INTO inbound_order_detail (inbound_order_id, part_id, part_code, part_name, unit, planned_qty, actual_qty, warehouse_area_id, line_no)
SELECT io.id, p.id, p.code, p.name, p.unit, 200, 0, wa.id, 2
FROM inbound_order io, part p, warehouse_area wa
WHERE io.order_no = 'R20260606001' AND p.code = 'P002' AND wa.code = 'A';

INSERT INTO inbound_order_detail (inbound_order_id, part_id, part_code, part_name, unit, planned_qty, actual_qty, warehouse_area_id, line_no)
SELECT io.id, p.id, p.code, p.name, p.unit, 50, 0, wa.id, 3
FROM inbound_order io, part p, warehouse_area wa
WHERE io.order_no = 'R20260606001' AND p.code = 'P009' AND wa.code = 'B';

-- ============================================================
-- 5.2 部分入库状态的入库单 (用于部分入库+继续扫码测试)
-- ============================================================

INSERT INTO inbound_order (order_no, supplier_id, supplier_name, order_number, status, remark, create_user_id)
SELECT 'R20260606002', s.id, s.name, 'PO-2026-002', 1, '测试入库单-部分入库', u.id
FROM supplier s, sys_user u
WHERE s.code = 'GYS002' AND u.username = 'admin';

-- 明细1：已完成入库
INSERT INTO inbound_order_detail (inbound_order_id, part_id, part_code, part_name, unit, planned_qty, actual_qty, warehouse_area_id, line_no)
SELECT io.id, p.id, p.code, p.name, p.unit, 500, 500, wa.id, 1
FROM inbound_order io, part p, warehouse_area wa
WHERE io.order_no = 'R20260606002' AND p.code = 'P003' AND wa.code = 'B';

-- 明细2：部分入库 (300/1000)
INSERT INTO inbound_order_detail (inbound_order_id, part_id, part_code, part_name, unit, planned_qty, actual_qty, warehouse_area_id, line_no)
SELECT io.id, p.id, p.code, p.name, p.unit, 1000, 300, wa.id, 2
FROM inbound_order io, part p, warehouse_area wa
WHERE io.order_no = 'R20260606002' AND p.code = 'P004' AND wa.code = 'B';

-- 对应的扫描记录
INSERT INTO scan_record (inbound_order_id, inbound_order_no, part_id, part_code, part_name, batch_no, scan_qty, operator_id)
SELECT io.id, io.order_no, p.id, p.code, p.name, 'BATCH-001', 500, u.id
FROM inbound_order io, part p, sys_user u
WHERE io.order_no = 'R20260606002' AND p.code = 'P003' AND u.username = 'admin';

INSERT INTO scan_record (inbound_order_id, inbound_order_no, part_id, part_code, part_name, batch_no, scan_qty, operator_id)
SELECT io.id, io.order_no, p.id, p.code, p.name, 'BATCH-002', 100, u.id
FROM inbound_order io, part p, sys_user u
WHERE io.order_no = 'R20260606002' AND p.code = 'P004' AND u.username = 'user1';

INSERT INTO scan_record (inbound_order_id, inbound_order_no, part_id, part_code, part_name, batch_no, scan_qty, operator_id)
SELECT io.id, io.order_no, p.id, p.code, p.name, 'BATCH-002', 100, u.id
FROM inbound_order io, part p, sys_user u
WHERE io.order_no = 'R20260606002' AND p.code = 'P004' AND u.username = 'user1';

INSERT INTO scan_record (inbound_order_id, inbound_order_no, part_id, part_code, part_name, batch_no, scan_qty, operator_id)
SELECT io.id, io.order_no, p.id, p.code, p.name, 'BATCH-003', 100, u.id
FROM inbound_order io, part p, sys_user u
WHERE io.order_no = 'R20260606002' AND p.code = 'P004' AND u.username = 'user1';

-- ============================================================
-- 5.3 已入库状态的入库单 (用于权限测试)
-- ============================================================

INSERT INTO inbound_order (order_no, supplier_id, supplier_name, order_number, status, remark, create_user_id)
SELECT 'R20260606003', s.id, s.name, 'PO-2026-003', 2, '测试入库单-已入库(已完成)', u.id
FROM supplier s, sys_user u
WHERE s.code = 'GYS003' AND u.username = 'admin';

INSERT INTO inbound_order_detail (inbound_order_id, part_id, part_code, part_name, unit, planned_qty, actual_qty, warehouse_area_id, line_no)
SELECT io.id, p.id, p.code, p.name, p.unit, 300, 300, wa.id, 1
FROM inbound_order io, part p, warehouse_area wa
WHERE io.order_no = 'R20260606003' AND p.code = 'P005' AND wa.code = 'C';

INSERT INTO inbound_order_detail (inbound_order_id, part_id, part_code, part_name, unit, planned_qty, actual_qty, warehouse_area_id, line_no)
SELECT io.id, p.id, p.code, p.name, p.unit, 200, 200, wa.id, 2
FROM inbound_order io, part p, warehouse_area wa
WHERE io.order_no = 'R20260606003' AND p.code = 'P006' AND wa.code = 'C';

-- ============================================================
-- 5.4 作废状态的入库单
-- ============================================================

INSERT INTO inbound_order (order_no, supplier_id, supplier_name, order_number, status, remark, create_user_id)
SELECT 'R20260606004', s.id, s.name, 'PO-2026-004', 3, '测试入库单-作废', u.id
FROM supplier s, sys_user u
WHERE s.code = 'GYS004' AND u.username = 'admin';

INSERT INTO inbound_order_detail (inbound_order_id, part_id, part_code, part_name, unit, planned_qty, actual_qty, warehouse_area_id, line_no)
SELECT io.id, p.id, p.code, p.name, p.unit, 10, 0, wa.id, 1
FROM inbound_order io, part p, warehouse_area wa
WHERE io.order_no = 'R20260606004' AND p.code = 'P007' AND wa.code = 'D';

-- ============================================================
-- 6. 空白入库单 (用于新建流程测试)
-- 不预先创建，通过 API 新建来验证完整流程
-- 由测试脚本动态创建: R20260606005
-- ============================================================
