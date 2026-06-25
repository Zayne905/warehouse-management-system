-- V15: 看板增加 qr_content 字段，持久化二维码内容，保证全生命周期唯一
USE warehouse_db;

ALTER TABLE kanban
    ADD COLUMN qr_content VARCHAR(600) DEFAULT NULL COMMENT '二维码JSON内容(创建时生成，不可变)' AFTER kanban_no;
