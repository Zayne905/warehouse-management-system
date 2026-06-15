-- V7: 出库管理

USE warehouse_db;

-- 出库单主表
CREATE TABLE IF NOT EXISTS outbound_order (
    id              BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    order_no        VARCHAR(20)  NOT NULL COMMENT '出库单号 (C+yyyyMMdd+序号)',
    status          TINYINT      DEFAULT 0 COMMENT '0=未出库 1=部分出库 2=已出库 3=作废',
    remark          VARCHAR(500) DEFAULT NULL COMMENT '备注',
    customer_name   VARCHAR(100) DEFAULT NULL COMMENT '客户名称',
    create_user_id  BIGINT       DEFAULT NULL COMMENT '创建人ID',
    create_time     DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time     DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_order_no (order_no),
    KEY idx_status (status),
    KEY idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='出库单主表';

-- 出库单明细
CREATE TABLE IF NOT EXISTS outbound_order_detail (
    id                BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    outbound_order_id BIGINT       NOT NULL COMMENT '出库单ID',
    part_id           BIGINT       NOT NULL COMMENT '零件ID',
    part_code         VARCHAR(50)  DEFAULT NULL COMMENT '零件编码(冗余)',
    part_name         VARCHAR(100) DEFAULT NULL COMMENT '零件名称(冗余)',
    unit              VARCHAR(20)  DEFAULT NULL COMMENT '单位(冗余)',
    planned_qty       DECIMAL(10,2) DEFAULT 0.00 COMMENT '计划出库数量',
    actual_qty        DECIMAL(10,2) DEFAULT 0.00 COMMENT '实际出库数量',
    box_count         INT          DEFAULT 0 COMMENT '计划箱数',
    warehouse_area_id BIGINT       DEFAULT NULL COMMENT '指定库区',
    line_no           INT          DEFAULT 0 COMMENT '行号',
    create_time       DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    KEY idx_outbound_order_id (outbound_order_id),
    KEY idx_part_id (part_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='出库单明细';

-- 出库扫描记录
CREATE TABLE IF NOT EXISTS outbound_scan (
    id                  BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    outbound_order_id   BIGINT       DEFAULT NULL COMMENT '出库单ID(无单时为NULL)',
    outbound_order_no   VARCHAR(20)  DEFAULT NULL COMMENT '出库单号',
    kanban_no           VARCHAR(80)  DEFAULT NULL COMMENT '看板号',
    part_id             BIGINT       NOT NULL COMMENT '零件ID',
    part_code           VARCHAR(50)  DEFAULT NULL COMMENT '零件编码',
    part_name           VARCHAR(100) DEFAULT NULL COMMENT '零件名称',
    quantity            DECIMAL(10,2) DEFAULT 0.00 COMMENT '出库数量',
    warehouse_area_id   BIGINT       DEFAULT NULL COMMENT '出库时所在库区ID',
    warehouse_area_name VARCHAR(100) DEFAULT NULL COMMENT '出库时所在库区名称',
    scan_time           DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '扫码时间',
    operator_id         BIGINT       DEFAULT NULL COMMENT '操作人ID',
    create_time         DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    KEY idx_outbound_order_id (outbound_order_id),
    KEY idx_kanban_no (kanban_no),
    KEY idx_part_id (part_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='出库扫描记录';
