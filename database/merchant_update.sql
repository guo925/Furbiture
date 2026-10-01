-- 商家管理功能数据库更新脚本
-- 执行前请确保已经执行过 schema.sql

-- ============================================
-- 1. 修改商品表，添加商家ID字段
-- ============================================
-- ⚠️ 不要写 `ALTER TABLE ... ADD COLUMN IF NOT EXISTS` / `CREATE INDEX IF NOT EXISTS`：
--    `IF NOT EXISTS` 是 MariaDB 方言，**MySQL 8.0 不支持**，直接执行会报 ERROR 1064。
--    这里沿用 migration_v2.sql 的幂等写法：用存储过程查 information_schema 判断
--    列/索引是否已存在，再决定是否创建。本脚本因此可以安全地重复执行。
DELIMITER //
CREATE PROCEDURE IF NOT EXISTS add_product_merchant_id()
BEGIN
  IF NOT EXISTS (SELECT * FROM information_schema.COLUMNS
                 WHERE TABLE_SCHEMA = DATABASE()
                   AND TABLE_NAME = 'product'
                   AND COLUMN_NAME = 'merchant_id') THEN
    ALTER TABLE `product` ADD COLUMN `merchant_id` BIGINT COMMENT '商家ID' AFTER `description`;
  END IF;

  -- 不再在这里建 merchant_id 索引：migration_v2 会建 idx_product_merchant(merchant_id)，
  -- 两者完全重复。迁移 v7 会删掉存量库里多余的 idx_merchant_id。
END //
DELIMITER ;

CALL add_product_merchant_id();
DROP PROCEDURE IF EXISTS add_product_merchant_id;

-- 为已有商品回填一个默认商家ID（假设商家ID为3）。
-- 注意：这会把所有 merchant_id 为 NULL 的商品归给 merchant1，属一次性数据回填；
--       应用创建商品时应由服务端强制写入真实的 merchant_id，不应依赖这条兜底。
UPDATE `product` SET `merchant_id` = 3 WHERE `merchant_id` IS NULL;

-- ============================================
-- 2. 修改用户表，支持MERCHANT角色
-- 注意：role字段已经是VARCHAR(20)，已经支持
-- ============================================

-- ============================================
-- 3. 插入测试商家用户
-- 密码是 123456（BCrypt加密后的值）
-- ============================================
INSERT INTO `user` (`username`, `password`, `phone`, `email`, `role`) VALUES
('merchant1', '$2a$10$rsZCZpE5SIJE7qy5PDuyLOKID6A4DLfdbRdNDHjSZywKQ1ILl2N3O', '13800138002', 'merchant1@example.com', 'MERCHANT'),
('merchant2', '$2a$10$rsZCZpE5SIJE7qy5PDuyLOKID6A4DLfdbRdNDHjSZywKQ1ILl2N3O', '13800138003', 'merchant2@example.com', 'MERCHANT')
ON DUPLICATE KEY UPDATE
`password` = VALUES(`password`),
`phone` = VALUES(`phone`),
`email` = VALUES(`email`),
`role` = VALUES(`role`);

-- ============================================
-- 4. 为商家添加一些商品
-- ============================================
INSERT INTO `product` (`name`, `category_id`, `brand`, `main_image`, `price`, `stock`, `status`, `sales`, `description`, `merchant_id`)
SELECT '意式真皮沙发', 5, '顾家', '/images/sofa2.jpg', 5999.00, 30, 1, 15, '意式设计，真皮材质，高端大气', 3
WHERE NOT EXISTS (SELECT 1 FROM `product` WHERE `name` = '意式真皮沙发' AND `merchant_id` = 3);

INSERT INTO `product` (`name`, `category_id`, `brand`, `main_image`, `price`, `stock`, `status`, `sales`, `description`, `merchant_id`)
SELECT '现代简约茶几', 6, '林氏木业', '/images/coffee_table2.jpg', 799.00, 40, 1, 8, '现代简约设计，适合小户型', 3
WHERE NOT EXISTS (SELECT 1 FROM `product` WHERE `name` = '现代简约茶几' AND `merchant_id` = 3);

INSERT INTO `product` (`name`, `category_id`, `brand`, `main_image`, `price`, `stock`, `status`, `sales`, `description`, `merchant_id`)
SELECT '轻奢电视柜', 7, '顾家', '/images/tv_cabinet2.jpg', 2299.00, 25, 1, 5, '轻奢风格，收纳功能强大', 3
WHERE NOT EXISTS (SELECT 1 FROM `product` WHERE `name` = '轻奢电视柜' AND `merchant_id` = 3);

INSERT INTO `product` (`name`, `category_id`, `brand`, `main_image`, `price`, `stock`, `status`, `sales`, `description`, `merchant_id`)
SELECT '美式大床', 8, '芝华士', '/images/bed2.jpg', 4599.00, 20, 1, 10, '美式风格，舒适睡眠', 4
WHERE NOT EXISTS (SELECT 1 FROM `product` WHERE `name` = '美式大床' AND `merchant_id` = 4);

INSERT INTO `product` (`name`, `category_id`, `brand`, `main_image`, `price`, `stock`, `status`, `sales`, `description`, `merchant_id`)
SELECT '欧式衣柜', 9, '索菲亚', '/images/wardrobe2.jpg', 5599.00, 15, 1, 3, '欧式设计，大容量存储', 4
WHERE NOT EXISTS (SELECT 1 FROM `product` WHERE `name` = '欧式衣柜' AND `merchant_id` = 4);
