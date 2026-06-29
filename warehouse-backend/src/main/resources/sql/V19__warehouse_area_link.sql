-- V19: 库区关联仓库
-- ============================================================
USE warehouse_db;

ALTER TABLE warehouse_area
    ADD COLUMN warehouse_id BIGINT DEFAULT NULL COMMENT '所属仓库ID' AFTER name,
    ADD INDEX idx_warehouse_id (warehouse_id);

-- 将现有库区关联到主仓库(WH001)
UPDATE warehouse_area SET warehouse_id = (SELECT id FROM warehouse WHERE code = 'WH001' LIMIT 1);
