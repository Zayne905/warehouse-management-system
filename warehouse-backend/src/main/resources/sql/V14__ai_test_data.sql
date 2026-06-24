-- ============================================================
-- V14: AI 功能验证测试数据
-- 生成跨30天的出入库数据、多状态看板、扫描记录
-- 用于验证 AI 需求分析、趋势图表、今日摘要等所有 AI 功能
-- ============================================================

USE warehouse_db;

-- ============================================================
-- 1. 补充更多客户数据
-- ============================================================
INSERT IGNORE INTO customer (code, name, contact, phone, address) VALUES
('CUST004', '华东汽车零部件有限公司', '周总', '13900004001', '杭州市萧山区'),
('CUST005', '南方智能设备制造厂', '吴经理', '13800005005', '广州市番禺区'),
('CUST006', '北京航天精密仪器所', '郑工', '13600006006', '北京市海淀区'),
('CUST007', '武汉光电技术有限公司', '钱经理', '13700007007', '武汉市东湖高新区'),
('CUST008', '成都电子科技大学产业园', '孙主任', '13500008008', '成都市高新区');

-- ============================================================
-- 2. 补充供应商-物料关联（覆盖所有零件到所有供应商）
-- ============================================================
INSERT IGNORE INTO supplier_part (supplier_id, part_id)
SELECT s.id, p.id FROM supplier s, part p
WHERE s.code = 'GYS001' AND p.code IN ('P003','P004','P005');

INSERT IGNORE INTO supplier_part (supplier_id, part_id)
SELECT s.id, p.id FROM supplier s, part p
WHERE s.code = 'GYS002' AND p.code IN ('P001','P002','P009','P010');

INSERT IGNORE INTO supplier_part (supplier_id, part_id)
SELECT s.id, p.id FROM supplier s, part p
WHERE s.code = 'GYS005' AND p.code IN ('P001','P002','P007','P008','P009','P010');

INSERT IGNORE INTO supplier_part (supplier_id, part_id)
SELECT s.id, p.id FROM supplier s, part p
WHERE s.code = 'GYS006' AND p.code IN ('P003','P004','P005','P006','P013','P014');

-- ============================================================
-- 3. 更新零件信息：设置 packageCapacity 和默认库区
-- ============================================================
UPDATE part SET package_capacity = 100, warehouse_area_id = (SELECT id FROM warehouse_area WHERE code = 'A') WHERE code = 'P001';
UPDATE part SET package_capacity = 200, warehouse_area_id = (SELECT id FROM warehouse_area WHERE code = 'A') WHERE code = 'P002';
UPDATE part SET package_capacity = 500, warehouse_area_id = (SELECT id FROM warehouse_area WHERE code = 'B') WHERE code = 'P003';
UPDATE part SET package_capacity = 500, warehouse_area_id = (SELECT id FROM warehouse_area WHERE code = 'B') WHERE code = 'P004';
UPDATE part SET package_capacity = 50,  warehouse_area_id = (SELECT id FROM warehouse_area WHERE code = 'C') WHERE code = 'P005';
UPDATE part SET package_capacity = 100, warehouse_area_id = (SELECT id FROM warehouse_area WHERE code = 'C') WHERE code = 'P006';
UPDATE part SET package_capacity = 10,  warehouse_area_id = (SELECT id FROM warehouse_area WHERE code = 'D') WHERE code = 'P007';
UPDATE part SET package_capacity = 50,  warehouse_area_id = (SELECT id FROM warehouse_area WHERE code = 'D') WHERE code = 'P008';
UPDATE part SET package_capacity = 200, warehouse_area_id = (SELECT id FROM warehouse_area WHERE code = 'A') WHERE code = 'P009';
UPDATE part SET package_capacity = 100, warehouse_area_id = (SELECT id FROM warehouse_area WHERE code = 'A') WHERE code = 'P010';
UPDATE part SET package_capacity = 50,  warehouse_area_id = (SELECT id FROM warehouse_area WHERE code = 'E') WHERE code = 'P011';
UPDATE part SET package_capacity = 20,  warehouse_area_id = (SELECT id FROM warehouse_area WHERE code = 'E') WHERE code = 'P012';
UPDATE part SET package_capacity = 100, warehouse_area_id = (SELECT id FROM warehouse_area WHERE code = 'B') WHERE code = 'P013';
UPDATE part SET package_capacity = 500, warehouse_area_id = (SELECT id FROM warehouse_area WHERE code = 'C') WHERE code = 'P014';
UPDATE part SET package_capacity = 10,  warehouse_area_id = (SELECT id FROM warehouse_area WHERE code = 'C') WHERE code = 'P015';

-- ============================================================
-- 4. 生成30天历史数据（通过存储过程批量生成）
-- ============================================================

DELIMITER //

DROP PROCEDURE IF EXISTS gen_ai_test_data//

CREATE PROCEDURE gen_ai_test_data()
BEGIN
    DECLARE v_day INT DEFAULT 0;
    DECLARE v_date DATE;
    DECLARE v_date_str VARCHAR(10);
    DECLARE v_inbound_seq INT DEFAULT 1;
    DECLARE v_outbound_seq INT DEFAULT 1;
    DECLARE v_admin_id BIGINT;
    DECLARE v_user1_id BIGINT;
    DECLARE v_order_no VARCHAR(20);
    DECLARE v_inbound_id BIGINT;
    DECLARE v_outbound_id BIGINT;
    DECLARE v_part_id BIGINT;
    DECLARE v_part_code VARCHAR(10);
    DECLARE v_part_name VARCHAR(100);
    DECLARE v_unit VARCHAR(20);
    DECLARE v_supplier_id BIGINT;
    DECLARE v_supplier_name VARCHAR(100);
    DECLARE v_supplier_code VARCHAR(10);
    DECLARE v_customer_name VARCHAR(100);
    DECLARE v_customer_code VARCHAR(10);
    DECLARE v_area_id BIGINT;
    DECLARE v_planned_qty DECIMAL(10,2);
    DECLARE v_actual_qty DECIMAL(10,2);
    DECLARE v_scan_time DATETIME;
    DECLARE v_i INT;
    DECLARE v_status INT;

    -- 获取用户ID
    SELECT id INTO v_admin_id FROM sys_user WHERE username = 'admin' LIMIT 1;
    SELECT id INTO v_user1_id FROM sys_user WHERE username = 'user1' LIMIT 1;

    -- 循环生成30天数据（从30天前到今天）
    WHILE v_day < 30 DO
        SET v_date = DATE_SUB(CURDATE(), INTERVAL (29 - v_day) DAY);
        SET v_date_str = DATE_FORMAT(v_date, '%Y%m%d');

        -- ============================================================
        -- 每天生成 1-3 个入库单
        -- ============================================================

        -- 入库单1：深圳电子 (GYS001)
        SET v_inbound_seq = v_inbound_seq + 1;
        SET v_order_no = CONCAT('R', v_date_str, LPAD(v_inbound_seq, 3, '0'));
        SELECT id, name INTO v_supplier_id, v_supplier_name FROM supplier WHERE code = 'GYS001' LIMIT 1;

        INSERT IGNORE INTO inbound_order (order_no, supplier_id, supplier_name, order_number, status, remark, create_user_id, create_time, update_time)
        VALUES (v_order_no, v_supplier_id, v_supplier_name, CONCAT('PO-', v_date_str, '-001'),
                IF(v_day >= 27, 0, 2), -- 最近3天的为待入库，其余已完成
                CONCAT('AI测试-', v_date_str), v_admin_id,
                CONCAT(v_date, ' 08:', LPAD(FLOOR(RAND()*60),2,'0'), ':00'),
                CONCAT(v_date, ' 17:', LPAD(FLOOR(RAND()*60),2,'0'), ':00'));

        SET v_inbound_id = LAST_INSERT_ID();

        -- 明细：P001 + P002
        SELECT area.id INTO v_area_id FROM warehouse_area area WHERE area.code = 'A' LIMIT 1;

        INSERT IGNORE INTO inbound_order_detail (inbound_order_id, part_id, part_code, part_name, unit, planned_qty, actual_qty, warehouse_area_id, line_no, create_time)
        VALUES (v_inbound_id, (SELECT id FROM part WHERE code='P001'), 'P001', '电阻器 10KΩ', '个',
                IF(v_day >= 27, 1000, 1000),
                IF(v_day >= 27, 0, 1000),
                v_area_id, 1, CONCAT(v_date, ' 08:30:00'));

        INSERT IGNORE INTO inbound_order_detail (inbound_order_id, part_id, part_code, part_name, unit, planned_qty, actual_qty, warehouse_area_id, line_no, create_time)
        VALUES (v_inbound_id, (SELECT id FROM part WHERE code='P002'), 'P002', '电容器 100μF', '个',
                IF(v_day >= 27, 2000, 2000),
                IF(v_day >= 27, 0, 2000),
                v_area_id, 2, CONCAT(v_date, ' 08:30:00'));

        -- 入库单2：上海精密 (GYS002) - 部分日期有
        IF v_day % 3 = 0 THEN
            SET v_inbound_seq = v_inbound_seq + 1;
            SET v_order_no = CONCAT('R', v_date_str, LPAD(v_inbound_seq, 3, '0'));
            SELECT id, name INTO v_supplier_id, v_supplier_name FROM supplier WHERE code = 'GYS002' LIMIT 1;

            INSERT IGNORE INTO inbound_order (order_no, supplier_id, supplier_name, order_number, status, remark, create_user_id, create_time, update_time)
            VALUES (v_order_no, v_supplier_id, v_supplier_name, CONCAT('PO-', v_date_str, '-002'),
                    2, CONCAT('AI测试-', v_date_str), v_admin_id,
                    CONCAT(v_date, ' 09:15:00'),
                    CONCAT(v_date, ' 16:45:00'));

            SET v_inbound_id = LAST_INSERT_ID();
            SELECT area.id INTO v_area_id FROM warehouse_area area WHERE area.code = 'B' LIMIT 1;

            INSERT IGNORE INTO inbound_order_detail (inbound_order_id, part_id, part_code, part_name, unit, planned_qty, actual_qty, warehouse_area_id, line_no, create_time)
            VALUES (v_inbound_id, (SELECT id FROM part WHERE code='P003'), 'P003', 'M3螺丝', '个', 5000, 5000, v_area_id, 1, CONCAT(v_date, ' 09:15:00'));

            INSERT IGNORE INTO inbound_order_detail (inbound_order_id, part_id, part_code, part_name, unit, planned_qty, actual_qty, warehouse_area_id, line_no, create_time)
            VALUES (v_inbound_id, (SELECT id FROM part WHERE code='P004'), 'P004', '弹簧垫圈 M3', '个', 3000, 3000, v_area_id, 2, CONCAT(v_date, ' 09:15:00'));
        END IF;

        -- 入库单3：成都精密 (GYS005) - 每隔2天
        IF v_day % 2 = 0 THEN
            SET v_inbound_seq = v_inbound_seq + 1;
            SET v_order_no = CONCAT('R', v_date_str, LPAD(v_inbound_seq, 3, '0'));
            SELECT id, name INTO v_supplier_id, v_supplier_name FROM supplier WHERE code = 'GYS005' LIMIT 1;

            SET v_status = IF(v_day >= 28, 1, 2); -- 最近2天部分入库

            INSERT IGNORE INTO inbound_order (order_no, supplier_id, supplier_name, order_number, status, remark, create_user_id, create_time, update_time)
            VALUES (v_order_no, v_supplier_id, v_supplier_name, CONCAT('PO-', v_date_str, '-003'),
                    v_status, CONCAT('AI测试-', v_date_str), v_admin_id,
                    CONCAT(v_date, ' 10:00:00'),
                    CONCAT(v_date, ' 18:00:00'));

            SET v_inbound_id = LAST_INSERT_ID();
            SELECT area.id INTO v_area_id FROM warehouse_area area WHERE area.code = 'D' LIMIT 1;

            INSERT IGNORE INTO inbound_order_detail (inbound_order_id, part_id, part_code, part_name, unit, planned_qty, actual_qty, warehouse_area_id, line_no, create_time)
            VALUES (v_inbound_id, (SELECT id FROM part WHERE code='P007'), 'P007', '步进电机 42步', '台',
                    100, IF(v_day >= 28, 60, 100), v_area_id, 1, CONCAT(v_date, ' 10:00:00'));

            INSERT IGNORE INTO inbound_order_detail (inbound_order_id, part_id, part_code, part_name, unit, planned_qty, actual_qty, warehouse_area_id, line_no, create_time)
            VALUES (v_inbound_id, (SELECT id FROM part WHERE code='P008'), 'P008', '联轴器 5-8mm', '个',
                    200, IF(v_day >= 28, 120, 200), v_area_id, 2, CONCAT(v_date, ' 10:00:00'));
        END IF;

        -- ============================================================
        -- 每天生成 1-2 个出库单
        -- ============================================================

        -- 出库单1
        SET v_outbound_seq = v_outbound_seq + 1;
        SET v_order_no = CONCAT('C', v_date_str, LPAD(v_outbound_seq, 3, '0'));
        SELECT name INTO v_customer_name FROM customer ORDER BY RAND() LIMIT 1;

        SET v_status = IF(v_day >= 27, IF(v_day = 29, 0, 1), 2); -- 最近3天包含未出库/部分出库

        INSERT IGNORE INTO outbound_order (order_no, status, remark, customer_name, create_user_id, create_time, update_time)
        VALUES (v_order_no, v_status, CONCAT('AI测试出库-', v_date_str), v_customer_name, v_admin_id,
                CONCAT(v_date, ' ', LPAD(8 + FLOOR(RAND()*10), 2, '0'), ':', LPAD(FLOOR(RAND()*60),2,'0'), ':00'),
                CONCAT(v_date, ' ', LPAD(14 + FLOOR(RAND()*6), 2, '0'), ':', LPAD(FLOOR(RAND()*60),2,'0'), ':00'));

        SET v_outbound_id = LAST_INSERT_ID();

        -- 随机选2个零件
        SET v_i = 1;
        WHILE v_i <= 2 DO
            SELECT id, code, name, unit INTO v_part_id, v_part_code, v_part_name, v_unit
            FROM part ORDER BY RAND() LIMIT 1;
            SELECT area.id INTO v_area_id FROM warehouse_area area ORDER BY RAND() LIMIT 1;

            SET v_planned_qty = (FLOOR(RAND() * 500) + 100);
            SET v_actual_qty = IF(v_day >= 27, FLOOR(RAND() * v_planned_qty), v_planned_qty);

            INSERT IGNORE INTO outbound_order_detail (outbound_order_id, part_id, part_code, part_name, unit, planned_qty, actual_qty, warehouse_area_id, line_no, create_time)
            VALUES (v_outbound_id, v_part_id, v_part_code, v_part_name, v_unit,
                    v_planned_qty, v_actual_qty, v_area_id, v_i, CONCAT(v_date, ' 09:00:00'));
            SET v_i = v_i + 1;
        END WHILE;

        -- 出库单2 每隔2天
        IF v_day % 3 = 0 THEN
            SET v_outbound_seq = v_outbound_seq + 1;
            SET v_order_no = CONCAT('C', v_date_str, LPAD(v_outbound_seq, 3, '0'));
            SELECT name INTO v_customer_name FROM customer ORDER BY RAND() LIMIT 1;

            INSERT IGNORE INTO outbound_order (order_no, status, remark, customer_name, create_user_id, create_time, update_time)
            VALUES (v_order_no, 2, CONCAT('AI测试出库-', v_date_str), v_customer_name, v_admin_id,
                    CONCAT(v_date, ' 11:00:00'),
                    CONCAT(v_date, ' 15:30:00'));

            SET v_outbound_id = LAST_INSERT_ID();

            SELECT id, code, name, unit INTO v_part_id, v_part_code, v_part_name, v_unit
            FROM part ORDER BY RAND() LIMIT 1;
            SELECT area.id INTO v_area_id FROM warehouse_area area ORDER BY RAND() LIMIT 1;

            SET v_planned_qty = (FLOOR(RAND() * 300) + 50);

            INSERT IGNORE INTO outbound_order_detail (outbound_order_id, part_id, part_code, part_name, unit, planned_qty, actual_qty, warehouse_area_id, line_no, create_time)
            VALUES (v_outbound_id, v_part_id, v_part_code, v_part_name, v_unit,
                    v_planned_qty, v_planned_qty, v_area_id, 1, CONCAT(v_date, ' 11:00:00'));
        END IF;

        SET v_day = v_day + 1;
    END WHILE;

    -- ============================================================
    -- 生成看板数据（已入库的 = 有库存的）
    -- 为每个已完成的入库单明细生成看板
    -- ============================================================
    INSERT IGNORE INTO kanban (kanban_no, inbound_order_id, inbound_order_no, part_id, part_code, part_name,
                        supplier_name, quantity, original_qty, box_seq, warehouse_area_id, warehouse_area_name, status, create_time)
    SELECT
        CONCAT('K-', io.order_no, '-', d.part_code, '-', FLOOR(RAND() * 900 + 100)),
        io.id,
        io.order_no,
        d.part_id,
        d.part_code,
        d.part_name,
        io.supplier_name,
        d.actual_qty,
        d.planned_qty,
        1,
        d.warehouse_area_id,
        COALESCE(wa.name, '未知'),
        CASE
            WHEN io.status = 2 THEN 1  -- 已完成 → 在库可用
            WHEN io.status = 1 THEN 0  -- 部分入库 → 待入库
            ELSE 0
        END,
        io.create_time
    FROM inbound_order io
    JOIN inbound_order_detail d ON d.inbound_order_id = io.id
    LEFT JOIN warehouse_area wa ON wa.id = d.warehouse_area_id
    WHERE io.status IN (1, 2)
      AND d.actual_qty > 0
      AND io.create_time >= DATE_SUB(CURDATE(), INTERVAL 30 DAY)
      AND NOT EXISTS (SELECT 1 FROM kanban k WHERE k.inbound_order_id = io.id AND k.part_id = d.part_id);

    -- ============================================================
    -- 生成入库扫描记录（已完成的入库单）
    -- ============================================================
    INSERT IGNORE INTO scan_record (inbound_order_id, inbound_order_no, part_id, part_code, part_name, batch_no, scan_qty, scan_time, operator_id, create_time)
    SELECT
        io.id,
        io.order_no,
        d.part_id,
        d.part_code,
        d.part_name,
        CONCAT('BATCH-AI-', io.order_no),
        d.actual_qty,
        io.update_time,
        v_admin_id,
        io.update_time
    FROM inbound_order io
    JOIN inbound_order_detail d ON d.inbound_order_id = io.id
    CROSS JOIN (SELECT v_admin_id FROM (SELECT id AS v_admin_id FROM sys_user WHERE username = 'admin' LIMIT 1) t) u
    WHERE io.status = 2
      AND d.actual_qty > 0
      AND io.create_time >= DATE_SUB(CURDATE(), INTERVAL 30 DAY)
      AND NOT EXISTS (SELECT 1 FROM scan_record sr WHERE sr.inbound_order_id = io.id AND sr.part_id = d.part_id);

    -- ============================================================
    -- 生成出库扫描记录（已完成的出库单）
    -- ============================================================
    INSERT IGNORE INTO outbound_scan (outbound_order_id, outbound_order_no, kanban_no, part_id, part_code, part_name, quantity, warehouse_area_id, warehouse_area_name, scan_time, operator_id, create_time)
    SELECT
        oo.id,
        oo.order_no,
        CONCAT('K-', oo.order_no, '-', d.part_code),
        d.part_id,
        d.part_code,
        d.part_name,
        d.actual_qty,
        d.warehouse_area_id,
        COALESCE(wa.name, '未知'),
        oo.update_time,
        v_admin_id,
        oo.update_time
    FROM outbound_order oo
    JOIN outbound_order_detail d ON d.outbound_order_id = oo.id
    LEFT JOIN warehouse_area wa ON wa.id = d.warehouse_area_id
    CROSS JOIN (SELECT id AS v_admin_id FROM sys_user WHERE username = 'admin' LIMIT 1) u
    WHERE oo.status = 2
      AND d.actual_qty > 0
      AND oo.create_time >= DATE_SUB(CURDATE(), INTERVAL 30 DAY)
      AND NOT EXISTS (SELECT 1 FROM outbound_scan os WHERE os.outbound_order_id = oo.id AND os.part_id = d.part_id);

END//

DELIMITER ;

-- 执行存储过程
CALL gen_ai_test_data();

-- 清理
DROP PROCEDURE IF EXISTS gen_ai_test_data;

-- ============================================================
-- 5. 确保今日有足够数据用于"今日摘要"验证
-- ============================================================

-- 今日入库单（未入库状态，用于待处理提醒）
INSERT IGNORE INTO inbound_order (order_no, supplier_id, supplier_name, order_number, status, remark, create_user_id, create_time)
SELECT CONCAT('R', DATE_FORMAT(CURDATE(), '%Y%m%d'), '990'), s.id, s.name, CONCAT('PO-TODAY-001'), 0,
       '今日新建-待入库', u.id, NOW()
FROM supplier s, sys_user u
WHERE s.code = 'GYS001' AND u.username = 'admin';

-- 为今日入库单增加明细
INSERT IGNORE INTO inbound_order_detail (inbound_order_id, part_id, part_code, part_name, unit, planned_qty, actual_qty, warehouse_area_id, line_no, create_time)
SELECT io.id, p.id, p.code, p.name, p.unit, 500, 0, wa.id, 1, NOW()
FROM inbound_order io, part p, warehouse_area wa
WHERE io.order_no = CONCAT('R', DATE_FORMAT(CURDATE(), '%Y%m%d'), '990')
  AND p.code = 'P001' AND wa.code = 'A';

INSERT IGNORE INTO inbound_order_detail (inbound_order_id, part_id, part_code, part_name, unit, planned_qty, actual_qty, warehouse_area_id, line_no, create_time)
SELECT io.id, p.id, p.code, p.name, p.unit, 300, 0, wa.id, 2, NOW()
FROM inbound_order io, part p, warehouse_area wa
WHERE io.order_no = CONCAT('R', DATE_FORMAT(CURDATE(), '%Y%m%d'), '990')
  AND p.code = 'P009' AND wa.code = 'A';

-- 今日第二个入库单（部分入库）
INSERT IGNORE INTO inbound_order (order_no, supplier_id, supplier_name, order_number, status, remark, create_user_id, create_time)
SELECT CONCAT('R', DATE_FORMAT(CURDATE(), '%Y%m%d'), '991'), s.id, s.name, CONCAT('PO-TODAY-002'), 1,
       '今日-部分入库', u.id, NOW()
FROM supplier s, sys_user u
WHERE s.code = 'GYS002' AND u.username = 'admin';

INSERT IGNORE INTO inbound_order_detail (inbound_order_id, part_id, part_code, part_name, unit, planned_qty, actual_qty, warehouse_area_id, line_no, create_time)
SELECT io.id, p.id, p.code, p.name, p.unit, 1000, 600, wa.id, 1, NOW()
FROM inbound_order io, part p, warehouse_area wa
WHERE io.order_no = CONCAT('R', DATE_FORMAT(CURDATE(), '%Y%m%d'), '991')
  AND p.code = 'P003' AND wa.code = 'B';

-- 今日出库单
INSERT IGNORE INTO outbound_order (order_no, status, remark, customer_name, create_user_id, create_time)
SELECT CONCAT('C', DATE_FORMAT(CURDATE(), '%Y%m%d'), '990'), 1, '今日-部分出库', '华东汽车零部件有限公司', u.id, NOW()
FROM sys_user u WHERE u.username = 'admin';

INSERT IGNORE INTO outbound_order_detail (outbound_order_id, part_id, part_code, part_name, unit, planned_qty, actual_qty, warehouse_area_id, line_no, create_time)
SELECT oo.id, p.id, p.code, p.name, p.unit, 200, 80, wa.id, 1, NOW()
FROM outbound_order oo, part p, warehouse_area wa
WHERE oo.order_no = CONCAT('C', DATE_FORMAT(CURDATE(), '%Y%m%d'), '990')
  AND p.code = 'P007' AND wa.code = 'D';

-- 今日第二个出库单（已完成）
INSERT IGNORE INTO outbound_order (order_no, status, remark, customer_name, create_user_id, create_time)
SELECT CONCAT('C', DATE_FORMAT(CURDATE(), '%Y%m%d'), '991'), 2, '今日-已完成出库', '南方智能设备制造厂', u.id, NOW()
FROM sys_user u WHERE u.username = 'admin';

INSERT IGNORE INTO outbound_order_detail (outbound_order_id, part_id, part_code, part_name, unit, planned_qty, actual_qty, warehouse_area_id, line_no, create_time)
SELECT oo.id, p.id, p.code, p.name, p.unit, 150, 150, wa.id, 1, NOW()
FROM outbound_order oo, part p, warehouse_area wa
WHERE oo.order_no = CONCAT('C', DATE_FORMAT(CURDATE(), '%Y%m%d'), '991')
  AND p.code = 'P005' AND wa.code = 'C';

-- ============================================================
-- 6. 统计数据汇总（验证用）
-- ============================================================
-- 执行以下查询可验证数据生成情况:
--
-- SELECT '入库单' AS type, COUNT(*) AS cnt FROM inbound_order WHERE create_time >= DATE_SUB(CURDATE(), INTERVAL 30 DAY)
-- UNION ALL
-- SELECT '出库单', COUNT(*) FROM outbound_order WHERE create_time >= DATE_SUB(CURDATE(), INTERVAL 30 DAY)
-- UNION ALL
-- SELECT '看板', COUNT(*) FROM kanban WHERE create_time >= DATE_SUB(CURDATE(), INTERVAL 30 DAY)
-- UNION ALL
-- SELECT '入库扫描', COUNT(*) FROM scan_record WHERE scan_time >= DATE_SUB(CURDATE(), INTERVAL 30 DAY)
-- UNION ALL
-- SELECT '出库扫描', COUNT(*) FROM outbound_scan WHERE scan_time >= DATE_SUB(CURDATE(), INTERVAL 30 DAY);
