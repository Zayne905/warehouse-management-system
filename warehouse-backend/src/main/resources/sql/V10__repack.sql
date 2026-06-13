-- V10: 转包功能模块

USE warehouse_db;

-- 1. 转包单主表
CREATE TABLE IF NOT EXISTS repack_order (
    id                  BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    order_no            VARCHAR(20)  NOT NULL COMMENT '转包单号 RPyyyyMMddNNN',
    status              TINYINT      DEFAULT 0 COMMENT '状态: 0=待转包 1=已完成 2=已取消',
    repack_type         VARCHAR(20)  DEFAULT NULL COMMENT '转包类型: BREAKDOWN/CONSOLIDATE/REMAINDER',
    part_id             BIGINT       DEFAULT NULL COMMENT '限定零件ID',
    part_code           VARCHAR(50)  DEFAULT NULL COMMENT '限定零件编码',
    part_name           VARCHAR(100) DEFAULT NULL COMMENT '限定零件名称',
    warehouse_area_id   BIGINT       DEFAULT NULL COMMENT '限定库区ID',
    warehouse_area_name VARCHAR(100) DEFAULT NULL COMMENT '限定库区名称',
    target_box_capacity INT          DEFAULT NULL COMMENT '向下转包:目标箱容量',
    outbound_order_id   BIGINT       DEFAULT NULL COMMENT '关联出库单ID(可选)',
    outbound_order_no   VARCHAR(20)  DEFAULT NULL COMMENT '关联出库单号',
    remark              VARCHAR(500) DEFAULT NULL COMMENT '备注',
    operator_id         BIGINT       DEFAULT NULL COMMENT '操作人ID',
    create_time         DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time         DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_repack_order_no (order_no),
    KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='转包单';

-- 2. 转包单明细表
CREATE TABLE IF NOT EXISTS repack_order_detail (
    id                   BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    repack_order_id      BIGINT       NOT NULL COMMENT '转包单ID',
    source_kanban_id     BIGINT       NOT NULL COMMENT '源看板ID',
    source_kanban_no     VARCHAR(80)  NOT NULL COMMENT '源看板号',
    target_kanban_no     VARCHAR(80)  DEFAULT NULL COMMENT '目标看板号(执行转包时生成)',
    part_id              BIGINT       NOT NULL COMMENT '零件ID',
    part_code            VARCHAR(50)  DEFAULT NULL COMMENT '零件编码(冗余)',
    part_name            VARCHAR(100) DEFAULT NULL COMMENT '零件名称(冗余)',
    transfer_qty         INT          NOT NULL COMMENT '转出数量',
    remainder_qty        INT          DEFAULT 0 COMMENT '余量(带余量转包场景用)',
    source_remaining_qty INT          DEFAULT 0 COMMENT '转包后源看板剩余数量',
    line_no              INT          DEFAULT 0 COMMENT '行号',
    create_time          DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    KEY idx_repack_order_id (repack_order_id),
    KEY idx_source_kanban_no (source_kanban_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='转包单明细';

-- 3. 转包父子关系表
CREATE TABLE IF NOT EXISTS repack_relation (
    id               BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    parent_kanban_no VARCHAR(80)  NOT NULL COMMENT '父看板号(原包装)',
    child_kanban_no  VARCHAR(80)  NOT NULL COMMENT '子看板号(新包装)',
    repack_order_id  BIGINT       NOT NULL COMMENT '转包单ID',
    repack_order_no  VARCHAR(20)  DEFAULT NULL COMMENT '转包单号(冗余)',
    transfer_qty     INT          NOT NULL COMMENT '转出数量',
    part_id          BIGINT       DEFAULT NULL COMMENT '零件ID(冗余)',
    part_code        VARCHAR(50)  DEFAULT NULL COMMENT '零件编码(冗余)',
    part_name        VARCHAR(100) DEFAULT NULL COMMENT '零件名称(冗余)',
    operator_id      BIGINT       DEFAULT NULL COMMENT '操作人ID',
    repack_time      DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '转包时间',
    create_time      DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    KEY idx_parent_kanban (parent_kanban_no),
    KEY idx_child_kanban (child_kanban_no),
    KEY idx_repack_order (repack_order_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='转包父子关系表';

-- 4. 更新看板状态注释（增加状态5=部分转出, 6=已清空）
ALTER TABLE kanban
    MODIFY COLUMN status TINYINT DEFAULT 0 COMMENT '状态: 0=待入库 1=在库可用 2=待出库(锁定) 3=已出库 4=封存 5=部分转出 6=已清空';

-- 5. 看板增加箱容量字段，inbound_order_id改为可空（转包生成的新看板入库时再填充）
ALTER TABLE kanban
    ADD COLUMN original_qty INT DEFAULT NULL COMMENT '箱容量（原始装箱数量）' AFTER quantity;
ALTER TABLE kanban
    MODIFY COLUMN inbound_order_id BIGINT DEFAULT NULL COMMENT '入库单ID';
