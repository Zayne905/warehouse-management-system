-- Phase 2: 入库管理 数据库初始化
-- 在已有 sys_user 基础上新增角色字段 + 创建主数据表 + 入库相关表

USE warehouse_db;

-- ============================================================
-- 1. 已有表修改：sys_user 增加 role 字段
-- ============================================================
ALTER TABLE sys_user ADD COLUMN role VARCHAR(20) DEFAULT 'user' COMMENT '角色: admin/user' AFTER enabled;
UPDATE sys_user SET role = 'admin' WHERE username = 'admin';

-- ============================================================
-- 2. 供应商主数据
-- ============================================================
CREATE TABLE IF NOT EXISTS supplier (
    id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    code        VARCHAR(50)  NOT NULL COMMENT '供应商编码',
    name        VARCHAR(100) NOT NULL COMMENT '供应商名称',
    contact     VARCHAR(50)  DEFAULT NULL COMMENT '联系人',
    phone       VARCHAR(20)  DEFAULT NULL COMMENT '联系电话',
    address     VARCHAR(255) DEFAULT NULL COMMENT '地址',
    enabled     TINYINT(1)   DEFAULT 1 COMMENT '是否启用',
    create_time DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_code (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='供应商';

-- ============================================================
-- 3. 库区主数据
-- ============================================================
CREATE TABLE IF NOT EXISTS warehouse_area (
    id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    code        VARCHAR(50)  NOT NULL COMMENT '库区编码',
    name        VARCHAR(100) NOT NULL COMMENT '库区名称',
    enabled     TINYINT(1)   DEFAULT 1 COMMENT '是否启用',
    create_time DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_code (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='库区';

-- ============================================================
-- 4. 物料/零件主数据
-- ============================================================
CREATE TABLE IF NOT EXISTS part (
    id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    code        VARCHAR(50)  NOT NULL COMMENT '物料编码',
    name        VARCHAR(100) NOT NULL COMMENT '物料名称',
    unit        VARCHAR(20)  DEFAULT NULL COMMENT '单位 (个/箱/kg等)',
    spec        VARCHAR(100) DEFAULT NULL COMMENT '规格型号',
    enabled     TINYINT(1)   DEFAULT 1 COMMENT '是否启用',
    create_time DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_code (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='物料表';

-- ============================================================
-- 5. 供应商-物料关联
-- ============================================================
CREATE TABLE IF NOT EXISTS supplier_part (
    id          BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    supplier_id BIGINT NOT NULL COMMENT '供应商ID',
    part_id     BIGINT NOT NULL COMMENT '物料ID',
    PRIMARY KEY (id),
    UNIQUE KEY uk_supplier_part (supplier_id, part_id),
    KEY idx_supplier_id (supplier_id),
    KEY idx_part_id (part_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='供应商-物料关联';

-- ============================================================
-- 6. 入库单主表
-- ============================================================
CREATE TABLE IF NOT EXISTS inbound_order (
    id              BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    order_no        VARCHAR(20)  NOT NULL COMMENT '入库单号 (R+yyyyMMdd+序号)',
    supplier_id     BIGINT       NOT NULL COMMENT '供应商ID',
    supplier_name   VARCHAR(100) DEFAULT NULL COMMENT '供应商名称(冗余)',
    order_number    VARCHAR(100) DEFAULT NULL COMMENT '采购订单号',
    status          TINYINT      DEFAULT 0 COMMENT '状态: 0=未入库 1=部分入库 2=已入库 3=作废',
    remark          VARCHAR(500) DEFAULT NULL COMMENT '备注',
    create_user_id  BIGINT       DEFAULT NULL COMMENT '创建人ID',
    create_time     DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time     DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_order_no (order_no),
    KEY idx_supplier_id (supplier_id),
    KEY idx_status (status),
    KEY idx_order_number (order_number),
    KEY idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='入库单主表';

-- ============================================================
-- 7. 入库单明细
-- ============================================================
CREATE TABLE IF NOT EXISTS inbound_order_detail (
    id               BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    inbound_order_id BIGINT       NOT NULL COMMENT '入库单ID',
    part_id          BIGINT       NOT NULL COMMENT '物料ID',
    part_code        VARCHAR(50)  DEFAULT NULL COMMENT '物料编码(冗余)',
    part_name        VARCHAR(100) DEFAULT NULL COMMENT '物料名称(冗余)',
    unit             VARCHAR(20)  DEFAULT NULL COMMENT '单位(冗余)',
    planned_qty      DECIMAL(10,2) DEFAULT 0.00 COMMENT '计划入库数量',
    actual_qty       DECIMAL(10,2) DEFAULT 0.00 COMMENT '实际入库数量',
    warehouse_area_id BIGINT      DEFAULT NULL COMMENT '库区ID',
    batch_no         VARCHAR(50)  DEFAULT NULL COMMENT '批次号',
    line_no          INT          DEFAULT 0 COMMENT '行号(排序)',
    create_time      DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time      DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_inbound_order_id (inbound_order_id),
    KEY idx_part_id (part_id),
    KEY idx_batch_no (batch_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='入库单明细表';

-- ============================================================
-- 8. 扫描记录 (手持端)
-- ============================================================
CREATE TABLE IF NOT EXISTS scan_record (
    id               BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    inbound_order_id BIGINT       NOT NULL COMMENT '入库单ID',
    inbound_order_no VARCHAR(20)  DEFAULT NULL COMMENT '入库单号(冗余)',
    part_id          BIGINT       NOT NULL COMMENT '物料ID',
    part_code        VARCHAR(50)  DEFAULT NULL COMMENT '物料编码(冗余)',
    part_name        VARCHAR(100) DEFAULT NULL COMMENT '物料名称(冗余)',
    batch_no         VARCHAR(50)  DEFAULT NULL COMMENT '批次号',
    scan_qty         DECIMAL(10,2) DEFAULT 1.00 COMMENT '扫描数量',
    scan_time        DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '扫描时间',
    operator_id      BIGINT       DEFAULT NULL COMMENT '操作人ID',
    create_time      DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    KEY idx_inbound_order_id (inbound_order_id),
    KEY idx_inbound_order_part (inbound_order_id, part_id),
    KEY idx_batch_no (batch_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='扫描记录表';

-- ============================================================
-- 9. 种子数据
-- ============================================================

-- 供应商
INSERT INTO supplier (code, name, contact, phone, address) VALUES
('GYS001', '深圳电子科技有限公司', '张经理', '13800138001', '深圳市南山区科技园'),
('GYS002', '上海精密零件有限公司', '李经理', '13900139002', '上海市浦东新区张江'),
('GYS003', '广州包装材料厂', '王厂长', '13700137003', '广州市黄埔区'),
('GYS004', '北京机电设备公司', '赵总', '13600136004', '北京市大兴区')
ON DUPLICATE KEY UPDATE name = VALUES(name);

-- 库区
INSERT INTO warehouse_area (code, name) VALUES
('A', 'A区-电子零件'),
('B', 'B区-五金配件'),
('C', 'C区-包装材料'),
('D', 'D区-机电设备'),
('E', 'E区-综合区')
ON DUPLICATE KEY UPDATE name = VALUES(name);

-- 物料
INSERT INTO part (code, name, unit, spec) VALUES
('P001', '电阻器 10KΩ', '个', '0805贴片'),
('P002', '电容器 100μF', '个', '16V 铝电解'),
('P003', 'M3螺丝', '个', '304不锈钢'),
('P004', '弹簧垫圈 M3', '个', '65Mn'),
('P005', '纸箱 400x300x200', '只', '三层瓦楞'),
('P006', '泡沫板 1m×2m', '张', '10mm厚'),
('P007', '步进电机 42步', '台', '1.8° 1.5A'),
('P008', '联轴器 5-8mm', '个', '铝合金'),
('P009', 'LED灯珠 5mm', '个', '白光 3.2V'),
('P010', 'PCB板 100x100mm', '块', 'FR4 双面')
ON DUPLICATE KEY UPDATE name = VALUES(name);

-- 供应商-物料关联
INSERT INTO supplier_part (supplier_id, part_id)
SELECT s.id, p.id FROM supplier s, part p
WHERE s.code = 'GYS001' AND p.code IN ('P001', 'P002', 'P009', 'P010')
ON DUPLICATE KEY UPDATE supplier_id = VALUES(supplier_id);

INSERT INTO supplier_part (supplier_id, part_id)
SELECT s.id, p.id FROM supplier s, part p
WHERE s.code = 'GYS002' AND p.code IN ('P003', 'P004', 'P008')
ON DUPLICATE KEY UPDATE supplier_id = VALUES(supplier_id);

INSERT INTO supplier_part (supplier_id, part_id)
SELECT s.id, p.id FROM supplier s, part p
WHERE s.code = 'GYS003' AND p.code IN ('P005', 'P006')
ON DUPLICATE KEY UPDATE supplier_id = VALUES(supplier_id);

INSERT INTO supplier_part (supplier_id, part_id)
SELECT s.id, p.id FROM supplier s, part p
WHERE s.code = 'GYS004' AND p.code IN ('P007', 'P008')
ON DUPLICATE KEY UPDATE supplier_id = VALUES(supplier_id);
