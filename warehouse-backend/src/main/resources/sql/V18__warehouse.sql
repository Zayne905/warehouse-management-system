-- V18: 仓库管理模块
-- ============================================================
USE warehouse_db;

CREATE TABLE IF NOT EXISTS warehouse (
    id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    code        VARCHAR(50)  NOT NULL COMMENT '仓库编码',
    name        VARCHAR(100) NOT NULL COMMENT '仓库名称',
    address     VARCHAR(255) DEFAULT NULL COMMENT '仓库地址',
    admin_name  VARCHAR(50)  DEFAULT NULL COMMENT '负责人',
    enabled     TINYINT(1)   DEFAULT 1 COMMENT '启用: 1=启用 0=停用',
    create_time DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_code (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='仓库';

INSERT INTO warehouse (code, name, address, admin_name) VALUES
('WH001', '主仓库', '深圳市龙华区工业一路168号', '张主管'),
('WH002', '分仓A', '深圳市宝安区西乡大道99号', '李主管');
