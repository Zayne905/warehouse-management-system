CREATE DATABASE IF NOT EXISTS warehouse_db
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_general_ci;

USE warehouse_db;

CREATE TABLE IF NOT EXISTS sys_user (
    id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    username    VARCHAR(50)  NOT NULL COMMENT '用户名',
    password    VARCHAR(255) NOT NULL COMMENT '密码(BCrypt)',
    nickname    VARCHAR(50)  DEFAULT NULL COMMENT '昵称',
    enabled     TINYINT(1)   DEFAULT 1 COMMENT '是否启用',
    create_time DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_username (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统用户表';

INSERT INTO sys_user (username, password, nickname) VALUES
('admin', '$2a$10$pOEi7dfWpZEgonqq/8Z8YepNVtYsGFFzFO34SAznSM9s4hzI0tFnq', '管理员')
ON DUPLICATE KEY UPDATE username = username;
