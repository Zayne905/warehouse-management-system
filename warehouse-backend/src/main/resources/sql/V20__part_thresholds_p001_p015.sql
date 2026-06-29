-- V20: 为P001-P015零件设置高低储阈值
-- ============================================================
USE warehouse_db;

UPDATE part SET min_stock = 500,  max_stock = 5000  WHERE id = 1;
UPDATE part SET min_stock = 800,  max_stock = 8000  WHERE id = 2;
UPDATE part SET min_stock = 1000, max_stock = 10000 WHERE id = 3;
UPDATE part SET min_stock = 2000, max_stock = 15000 WHERE id = 4;
UPDATE part SET min_stock = 200,  max_stock = 3000  WHERE id = 5;
UPDATE part SET min_stock = 300,  max_stock = 5000  WHERE id = 6;
UPDATE part SET min_stock = 100,  max_stock = 1000  WHERE id = 7;
UPDATE part SET min_stock = 300,  max_stock = 3000  WHERE id = 8;
UPDATE part SET min_stock = 1000, max_stock = 8000  WHERE id = 9;
UPDATE part SET min_stock = 500,  max_stock = 5000  WHERE id = 10;
UPDATE part SET min_stock = 200,  max_stock = 2000  WHERE id = 11;
UPDATE part SET min_stock = 500,  max_stock = 6000  WHERE id = 12;
UPDATE part SET min_stock = 80,   max_stock = 800   WHERE id = 13;
UPDATE part SET min_stock = 300,  max_stock = 3000  WHERE id = 14;
UPDATE part SET min_stock = 150,  max_stock = 1500  WHERE id = 15;
