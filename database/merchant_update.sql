-- 商家管理功能数据库更新脚本
-- 执行前请确保已经执行过 schema.sql

-- ============================================
-- 1. 修改商品表，添加商家ID字段
-- ============================================
ALTER TABLE `product` ADD COLUMN IF NOT EXISTS `merchant_id` BIGINT COMMENT '商家ID' AFTER `description`;

-- 为已有的商品设置一个默认商家ID（假设商家ID为3）
UPDATE `product` SET `merchant_id` = 3 WHERE `merchant_id` IS NULL;

-- 添加索引
CREATE INDEX IF NOT EXISTS `idx_merchant_id` ON `product` (`merchant_id`);

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
