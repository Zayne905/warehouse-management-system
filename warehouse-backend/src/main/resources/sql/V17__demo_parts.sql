-- V17: 30个示例物料 + 入库单 + 看板（用于演示高低储预警功能）
-- ============================================================
-- 预警分布：
--   5个低储预警（库存 ≤ minStock）
--   5个高储预警（库存 ≥ maxStock）
--   5个双阈值正常（库存在 min/max 之间）
--   5个仅有最低储备（库存 OK）
--   5个仅有最高储备（库存 OK）
--   5个无阈值
-- ============================================================

USE warehouse_db;

-- ============================================================
-- 1. 新增 30 个物料（P016 ~ P045）
-- ============================================================

-- --- 低储预警组（5个）：库存远低于 minStock ---
INSERT INTO part (id, code, name, unit, spec, package_capacity, warehouse_area_id, min_stock, max_stock, enabled, create_time)
VALUES
(16, 'P016', 'M4不锈钢螺丝 12mm',    '个', '304不锈钢', 500, 2, 2000, 0,    1, NOW()),
(17, 'P017', 'M6碳钢螺母',           '个', '8.8级 镀锌', 500, 2, 3000, 0,    1, NOW()),
(18, 'P018', '伺服电机 750W',         '台', '220V 3000rpm', 10, 4, 200,  0,    1, NOW()),
(19, 'P019', '热缩管套装 φ1-10mm',   '套', '彩色 100根',   50, 5, 5000, 0,    1, NOW()),
(20, 'P020', '锂电池 18650 3.7V',    '节', '2600mAh',     100, 5, 1000, 0,    1, NOW());

-- --- 高储预警组（5个）：库存远超 maxStock ---
INSERT INTO part (id, code, name, unit, spec, package_capacity, warehouse_area_id, min_stock, max_stock, enabled, create_time)
VALUES
(21, 'P021', '铝电解电容 220μF 25V', '个', 'Φ8x12mm 插件', 200, 1, 0, 3000,  1, NOW()),
(22, 'P022', '贴片电阻 1KΩ 0603',    '个', '±1% 0.1W',   200, 1, 0, 5000,  1, NOW()),
(23, 'P023', 'PVC绝缘胶带 18mm',      '卷', '黑色 20m',    100, 3, 0, 500,   1, NOW()),
(24, 'P024', '尼龙扎带 3×200mm',      '包', '100根/包',   200, 3, 0, 10000, 1, NOW()),
(25, 'P025', '导热硅脂 30g',          '支', '灰色 1.2W/mK', 50, 4, 0, 500,   1, NOW());

-- --- 双阈值正常组（5个）：库存处于 min~max 正常范围 ---
INSERT INTO part (id, code, name, unit, spec, package_capacity, warehouse_area_id, min_stock, max_stock, enabled, create_time)
VALUES
(26, 'P026', '继电器模块 5V 单路',     '个', '10A 250VAC', 100, 1, 500,  5000,  1, NOW()),
(27, 'P027', '排针 2.54mm 40P 直插',  '排', '镀金 单排',   200, 1, 2000, 15000, 1, NOW()),
(28, 'P028', '接线端子 5mm 2P',        '个', '紫铜 阻燃',  200, 1, 300,  3000,  1, NOW()),
(29, 'P029', '焊锡丝 0.8mm 500g',      '卷', '63/37 松香芯',10, 1, 100,  500,   1, NOW()),
(30, 'P030', '散热风扇 12V 80mm',      '个', '双滚珠 静音', 50, 4, 150,  1500,  1, NOW());

-- --- 仅最低储备，库存 OK（5个）---
INSERT INTO part (id, code, name, unit, spec, package_capacity, warehouse_area_id, min_stock, max_stock, enabled, create_time)
VALUES
(31, 'P031', '步进驱动器 DM542',       '台', '2相 4.2A',   10, 4, 100,  0, 1, NOW()),
(32, 'P032', '开关电源 24V 10A',       '个', '导轨安装',   20, 4, 80,   0, 1, NOW()),
(33, 'P033', '光电开关 E3F-DS30C4',    '个', 'NPN 常开 DC', 50, 4, 300, 0, 1, NOW()),
(34, 'P034', '接近开关 LJ12A3-4-Z/BX', '个', 'M12 NPN',    50, 4, 200,  0, 1, NOW()),
(35, 'P035', '电磁阀 4V210-08 24V',    '个', '二位五通',   30, 4, 100,  0, 1, NOW());

-- --- 仅最高储备，库存 OK（5个）---
INSERT INTO part (id, code, name, unit, spec, package_capacity, warehouse_area_id, min_stock, max_stock, enabled, create_time)
VALUES
(36, 'P036', 'PVC线槽 40×40mm',        '米', '阻燃 灰色',  100, 5, 0, 500,  1, NOW()),
(37, 'P037', '铝型材 2020 1m',         '根', '欧标 阳极',  20,  5, 0, 200,  1, NOW()),
(38, 'P038', '角码连接件 2020',        '个', '铸铝 L型',   100, 5, 0, 5000, 1, NOW()),
(39, 'P039', 'T型螺母 M4 2020系列',    '个', '镀镍',       200, 5, 0, 8000, 1, NOW()),
(40, 'P040', '端盖 2020型材用',        '个', '黑色塑料',   200, 5, 0, 3000, 1, NOW());

-- --- 无阈值组（5个）---
INSERT INTO part (id, code, name, unit, spec, package_capacity, warehouse_area_id, min_stock, max_stock, enabled, create_time)
VALUES
(41, 'P041', '面包板 830孔',           '块', 'MB-102 透明', 30, 1, 0, 0, 1, NOW()),
(42, 'P042', '杜邦线 公母 20cm',       '套', '40pin 彩色',  100, 1, 0, 0, 1, NOW()),
(43, 'P043', '万用表笔 1对',           '套', '1000V 20A',   50, 5, 0, 0, 1, NOW()),
(44, 'P044', '防静电手环 有线',        '个', '1MΩ 电阻',    100, 5, 0, 0, 1, NOW()),
(45, 'P045', '元件收纳盒 15格',        '个', 'ABS 透明盖',  20, 5, 0, 0, 1, NOW());

-- ============================================================
-- 2. 供应商-物料关联（supplier 1=深圳电子科技, 2=上海精密零件）
-- ============================================================
INSERT INTO supplier_part (supplier_id, part_id) VALUES
-- 深圳电子科技 → 电子类零件（P016-P030，15个）
(1, 16), (1, 17), (1, 18), (1, 19), (1, 20),
(1, 21), (1, 22), (1, 23), (1, 24), (1, 25),
(1, 26), (1, 27), (1, 28), (1, 29), (1, 30),
-- 上海精密零件 → 机械/配件类零件（P031-P045，15个）
(2, 31), (2, 32), (2, 33), (2, 34), (2, 35),
(2, 36), (2, 37), (2, 38), (2, 39), (2, 40),
(2, 41), (2, 42), (2, 43), (2, 44), (2, 45);

-- ============================================================
-- 3. 入库单（已入库，status=2）+ 入库明细
-- ============================================================

-- 入库单1: 深圳电子科技（15个电子类物料）
INSERT INTO inbound_order (id, order_no, supplier_id, supplier_name, order_number, status, remark, create_time)
VALUES (200, 'R20260629001', 1, '深圳电子科技有限公司', 'PO-2026-DEMO-001', 2, '示例数据-电子类物料', '2026-06-20 08:00:00');

INSERT INTO inbound_order_detail (inbound_order_id, part_id, part_code, part_name, unit, planned_qty, actual_qty, warehouse_area_id, box_count, line_no)
SELECT 200, p.id, p.code, p.name, p.unit, p.package_capacity * 1, p.package_capacity * 1, p.warehouse_area_id, 1, p.id - 15
FROM part p WHERE p.id BETWEEN 16 AND 30;

-- 入库单2: 上海精密零件（15个机械/配件类物料）
INSERT INTO inbound_order (id, order_no, supplier_id, supplier_name, order_number, status, remark, create_time)
VALUES (201, 'R20260629002', 2, '上海精密零件有限公司', 'PO-2026-DEMO-002', 2, '示例数据-机械配件类', '2026-06-20 09:00:00');

INSERT INTO inbound_order_detail (inbound_order_id, part_id, part_code, part_name, unit, planned_qty, actual_qty, warehouse_area_id, box_count, line_no)
SELECT 201, p.id, p.code, p.name, p.unit, p.package_capacity * 1, p.package_capacity * 1, p.warehouse_area_id, 1, p.id - 30
FROM part p WHERE p.id BETWEEN 31 AND 45;

-- ============================================================
-- 4. 看板（每个物料1个看板，quantity 按设计设置以触发预警）
-- ============================================================

INSERT INTO kanban (id, kanban_no, inbound_order_id, inbound_order_no, part_id, part_code, part_name, supplier_name, quantity, original_qty, box_seq, warehouse_area_id, warehouse_area_name, status, create_time)
SELECT
    999 + p.id,
    CONCAT('DEMO-', p.code, '-001'),
    200, 'R20260629001', p.id, p.code, p.name,
    '深圳电子科技有限公司',
    CASE p.id
        -- 低储组: quantity 远低于 minStock
        WHEN 16 THEN 800    -- minStock=2000, stock=800 → 低储!
        WHEN 17 THEN 500    -- minStock=3000, stock=500 → 低储!
        WHEN 18 THEN 45     -- minStock=200,  stock=45  → 低储!
        WHEN 19 THEN 800    -- minStock=5000, stock=800 → 低储!
        WHEN 20 THEN 120    -- minStock=1000, stock=120 → 低储!
        -- 高储组: quantity 远超 maxStock
        WHEN 21 THEN 8500   -- maxStock=3000, stock=8500  → 高储!
        WHEN 22 THEN 15000  -- maxStock=5000, stock=15000 → 高储!
        WHEN 23 THEN 3200   -- maxStock=500,  stock=3200  → 高储!
        WHEN 24 THEN 25000  -- maxStock=10000,stock=25000 → 高储!
        WHEN 25 THEN 2800   -- maxStock=500,  stock=2800  → 高储!
        -- 双阈值正常组
        WHEN 26 THEN 1200   -- min=500, max=5000,  stock=1200 → OK
        WHEN 27 THEN 6000   -- min=2000,maz=15000, stock=6000 → OK
        WHEN 28 THEN 800    -- min=300, max=3000,  stock=800  → OK
        WHEN 29 THEN 280    -- min=100, max=500,   stock=280  → OK
        WHEN 30 THEN 500    -- min=150, max=1500,  stock=500  → OK
    END,
    p.package_capacity, 0,
    p.warehouse_area_id,
    CASE p.warehouse_area_id
        WHEN 1 THEN 'A区-电子零件' WHEN 2 THEN 'B区-五金配件'
        WHEN 3 THEN 'C区-包装材料' WHEN 4 THEN 'D区-机电设备'
        WHEN 5 THEN 'E区-综合区' ELSE 'A区-电子零件'
    END,
    1, '2026-06-20 08:00:00'
FROM part p WHERE p.id BETWEEN 16 AND 30;

INSERT INTO kanban (id, kanban_no, inbound_order_id, inbound_order_no, part_id, part_code, part_name, supplier_name, quantity, original_qty, box_seq, warehouse_area_id, warehouse_area_name, status, create_time)
SELECT
    999 + p.id,
    CONCAT('DEMO-', p.code, '-001'),
    201, 'R20260629002', p.id, p.code, p.name,
    '上海精密零件有限公司',
    CASE p.id
        -- 仅 min 组: quantity > minStock（正常）
        WHEN 31 THEN 250    -- min=100, stock=250 → OK
        WHEN 32 THEN 150    -- min=80,  stock=150 → OK
        WHEN 33 THEN 450    -- min=300, stock=450 → OK
        WHEN 34 THEN 350    -- min=200, stock=350 → OK
        WHEN 35 THEN 180    -- min=100, stock=180 → OK
        -- 仅 max 组: quantity < maxStock（正常）
        WHEN 36 THEN 300    -- max=500,  stock=300 → OK
        WHEN 37 THEN 120    -- max=200,  stock=120 → OK
        WHEN 38 THEN 2000   -- max=5000, stock=2000 → OK
        WHEN 39 THEN 3000   -- max=8000, stock=3000 → OK
        WHEN 40 THEN 800    -- max=3000, stock=800 → OK
        -- 无阈值组
        WHEN 41 THEN 90     -- no thresholds
        WHEN 42 THEN 500    -- no thresholds
        WHEN 43 THEN 200    -- no thresholds
        WHEN 44 THEN 300    -- no thresholds
        WHEN 45 THEN 100    -- no thresholds
    END,
    p.package_capacity, 0,
    p.warehouse_area_id,
    CASE p.warehouse_area_id
        WHEN 1 THEN 'A区-电子零件' WHEN 2 THEN 'B区-五金配件'
        WHEN 3 THEN 'C区-包装材料' WHEN 4 THEN 'D区-机电设备'
        WHEN 5 THEN 'E区-综合区' ELSE 'A区-电子零件'
    END,
    1, '2026-06-20 09:00:00'
FROM part p WHERE p.id BETWEEN 31 AND 45;
