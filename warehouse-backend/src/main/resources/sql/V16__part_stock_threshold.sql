-- V16: 零件高低储阈值
-- 为每种物料设置最低储备和最高储备阈值
-- min_stock: 库存下限，低于此值需补货（0=不启用）
-- max_stock: 库存上限，高于此值需停止采购（0=不启用）

USE warehouse_db;

ALTER TABLE part
    ADD COLUMN min_stock INT DEFAULT 0 COMMENT '最低储备阈值（0=不启用）',
    ADD COLUMN max_stock INT DEFAULT 0 COMMENT '最高储备阈值（0=不启用）';
