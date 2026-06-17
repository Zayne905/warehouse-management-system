-- V12: 客户管理模块
USE warehouse_db;

CREATE TABLE IF NOT EXISTS customer (
    id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    code        VARCHAR(50)  NOT NULL COMMENT '客户编码',
    name        VARCHAR(100) NOT NULL COMMENT '客户名称',
    contact     VARCHAR(50)  DEFAULT NULL COMMENT '联系人',
    phone       VARCHAR(20)  DEFAULT NULL COMMENT '联系电话',
    address     VARCHAR(255) DEFAULT NULL COMMENT '地址',
    enabled     TINYINT(1)   DEFAULT 1 COMMENT '启用: 1=启用 0=停用',
    create_time DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_code (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='客户';

-- 种子数据
INSERT INTO customer (code, name, contact, phone, address) VALUES
('CUST001', '测试客户A', '张经理', '13800001111', '深圳市南山区科技园'),
('CUST002', '测试客户B', '李主管', '13800002222', '上海市浦东新区张江'),
('CUST003', '测试客户C', '王工',    '13800003333', '北京市海淀区中关村');
