-- V5: 零件看板标签表

USE warehouse_db;

CREATE TABLE IF NOT EXISTS kanban (
    id                  BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    kanban_no           VARCHAR(80)  NOT NULL COMMENT '看板号 (唯一)',
    inbound_order_id    BIGINT       NOT NULL COMMENT '入库单ID',
    inbound_order_no    VARCHAR(20)  DEFAULT NULL COMMENT '入库单号(冗余)',
    part_id             BIGINT       NOT NULL COMMENT '零件ID',
    part_code           VARCHAR(50)  DEFAULT NULL COMMENT '零件编码(冗余)',
    part_name           VARCHAR(100) DEFAULT NULL COMMENT '零件名称(冗余)',
    supplier_name       VARCHAR(100) DEFAULT NULL COMMENT '供应商名称(冗余)',
    quantity            INT          DEFAULT 0 COMMENT '本箱数量(包装容量)',
    box_seq             INT          DEFAULT 0 COMMENT '箱序号',
    warehouse_area_id   BIGINT       DEFAULT NULL COMMENT '库区ID',
    warehouse_area_name VARCHAR(100) DEFAULT NULL COMMENT '库区名称',
    status              TINYINT      DEFAULT 0 COMMENT '状态: 0=待入库 1=已入库 2=已出库',
    create_time         DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_kanban_no (kanban_no),
    KEY idx_inbound_order_id (inbound_order_id),
    KEY idx_part_id (part_id),
    KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='零件看板标签';
