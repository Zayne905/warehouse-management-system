-- V6: 扫描记录关联看板号

USE warehouse_db;

ALTER TABLE scan_record
    ADD COLUMN kanban_no VARCHAR(80) DEFAULT NULL COMMENT '关联看板号';
