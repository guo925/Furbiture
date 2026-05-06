-- ============================================
-- 清空所有表数据并重新插入
-- 执行前请先执行: USE furniture_db;
-- ============================================

-- 禁用外键检查（如果有外键约束）
SET FOREIGN_KEY_CHECKS = 0;

-- ============================================
-- 1. 清空所有表数据
-- ============================================

TRUNCATE TABLE `order_item`;
TRUNCATE TABLE `order`;
TRUNCATE TABLE `cart`;
TRUNCATE TABLE `address`;
TRUNCATE TABLE `product_spec`;
TRUNCATE TABLE `product_image`;
TRUNCATE TABLE `product`;
TRUNCATE TABLE `category`;
TRUNCATE TABLE `user`;

-- 启用外键检查
SET FOREIGN_KEY_CHECKS = 1;

-- ============================================
-- 2. 重新插入测试数据
-- ============================================

-- 插入测试用户
INSERT INTO `user` (`username`, `password`, `phone`, `email`, `role`) VALUES
('admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EH', '13800138000', 'admin@example.com', 'ADMIN'),
('user1', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EH', '13800138001', 'user1@example.com', 'USER'),
('user2', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EH', '13800138002', 'user2@example.com', 'USER'),
('user3', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EH', '13800138003', 'user3@example.com', 'USER');

-- 插入测试分类
INSERT INTO `category` (`name`, `parent_id`, `level`, `sort_order`) VALUES
('客厅家具', 0, 1, 1),
('卧室家具', 0, 1, 2),
('餐厅家具', 0, 1, 3),
('办公家具', 0, 1, 4),
('沙发', 1, 2, 1),
('茶几', 1, 2, 2),
('电视柜', 1, 2, 3),
('床', 2, 2, 1),
('衣柜', 2, 2, 2),
('床头柜', 2, 2, 3),
('餐桌', 3, 2, 1),
('餐椅', 3, 2, 2),
('酒柜', 3, 2, 3),
('办公桌', 4, 2, 1),
('办公椅', 4, 2, 2),
('文件柜', 4, 2, 3);

-- 插入测试商品
INSERT INTO `product` (`name`, `category_id`, `brand`, `main_image`, `price`, `stock`, `status`, `sales`, `description`) VALUES
-- 沙发类
('现代简约三人沙发', 5, '宜家', '/images/sofa1.jpg', 2999.00, 50, 1, 10, '现代简约风格，舒适面料，适合客厅使用'),
('北欧布艺沙发', 5, '全友', '/images/sofa2.jpg', 3599.00, 30, 1, 8, '北欧风格，优质布艺，舒适耐用'),
('真皮沙发组合', 5, '顾家', '/images/sofa3.jpg', 5999.00, 20, 1, 5, '头层牛皮，奢华舒适，彰显品质'),
-- 茶几类
('北欧实木茶几', 6, '宜家', '/images/coffee_table1.jpg', 899.00, 30, 1, 5, '北欧风格，实木材质，简约大方'),
('现代玻璃茶几', 6, '全友', '/images/coffee_table2.jpg', 699.00, 40, 1, 12, '钢化玻璃，现代简约，易清洁'),
('大理石茶几', 6, '顾家', '/images/coffee_table3.jpg', 1299.00, 25, 1, 3, '天然大理石，高端大气，经久耐用'),
-- 电视柜类
('现代电视柜', 7, '全友', '/images/tv_cabinet1.jpg', 1299.00, 20, 1, 3, '现代简约设计，大容量储物空间'),
('北欧实木电视柜', 7, '宜家', '/images/tv_cabinet2.jpg', 1599.00, 15, 1, 7, '实木材质，环保健康，简约时尚'),
-- 床类
('实木双人床', 8, '全友', '/images/bed1.jpg', 3599.00, 15, 1, 8, '实木材质，环保健康，舒适睡眠'),
('现代简约床', 8, '宜家', '/images/bed2.jpg', 2899.00, 20, 1, 15, '简约设计，舒适床品，优质睡眠'),
('软包床', 8, '顾家', '/images/bed3.jpg', 4299.00, 12, 1, 6, '软包设计，舒适靠背，奢华体验'),
-- 衣柜类
('现代简约衣柜', 9, '索菲亚', '/images/wardrobe1.jpg', 4599.00, 10, 1, 2, '大容量储物，简约设计，适合卧室'),
('推拉门衣柜', 9, '全友', '/images/wardrobe2.jpg', 3999.00, 15, 1, 9, '推拉门设计，节省空间，实用美观'),
-- 床头柜类
('简约床头柜', 10, '宜家', '/images/nightstand1.jpg', 399.00, 50, 1, 20, '简约设计，实用储物，搭配床品'),
-- 餐桌类
('实木餐桌', 11, '宜家', '/images/dining_table1.jpg', 1899.00, 25, 1, 6, '实木材质，简约大方，适合家庭使用'),
('现代餐桌', 11, '全友', '/images/dining_table2.jpg', 1599.00, 30, 1, 11, '现代设计，实用美观，适合家庭'),
-- 餐椅类
('舒适餐椅', 12, '全友', '/images/dining_chair1.jpg', 399.00, 50, 1, 12, '舒适设计，人体工学，适合长时间用餐'),
('实木餐椅', 12, '宜家', '/images/dining_chair2.jpg', 499.00, 40, 1, 8, '实木材质，环保健康，舒适耐用'),
-- 酒柜类
('现代酒柜', 13, '顾家', '/images/wine_cabinet1.jpg', 2599.00, 15, 1, 4, '现代设计，大容量储物，展示收藏'),
-- 办公桌类
('现代办公桌', 14, '震旦', '/images/office_table1.jpg', 1599.00, 20, 1, 4, '现代简约设计，大桌面，适合办公'),
('L型办公桌', 14, '得力', '/images/office_table2.jpg', 1999.00, 15, 1, 7, 'L型设计，充分利用空间，高效办公'),
-- 办公椅类
('人体工学办公椅', 15, '震旦', '/images/office_chair1.jpg', 899.00, 30, 1, 7, '人体工学设计，舒适透气，适合长时间办公'),
('网布办公椅', 15, '得力', '/images/office_chair2.jpg', 599.00, 40, 1, 15, '网布材质，透气舒适，性价比高'),
-- 文件柜类
('钢制文件柜', 16, '震旦', '/images/file_cabinet1.jpg', 799.00, 25, 1, 9, '钢制材质，坚固耐用，大容量储物');

-- 插入测试商品图片
INSERT INTO `product_image` (`product_id`, `image_url`, `sort_order`) VALUES
-- 沙发图片
(1, '/images/sofa1_1.jpg', 1), (1, '/images/sofa1_2.jpg', 2), (1, '/images/sofa1_3.jpg', 3),
(2, '/images/sofa2_1.jpg', 1), (2, '/images/sofa2_2.jpg', 2),
(3, '/images/sofa3_1.jpg', 1), (3, '/images/sofa3_2.jpg', 2), (3, '/images/sofa3_3.jpg', 3),
-- 茶几图片
(4, '/images/coffee_table1_1.jpg', 1), (4, '/images/coffee_table1_2.jpg', 2),
(5, '/images/coffee_table2_1.jpg', 1), (5, '/images/coffee_table2_2.jpg', 2),
(6, '/images/coffee_table3_1.jpg', 1), (6, '/images/coffee_table3_2.jpg', 2),
-- 电视柜图片
(7, '/images/tv_cabinet1_1.jpg', 1), (7, '/images/tv_cabinet1_2.jpg', 2),
(8, '/images/tv_cabinet2_1.jpg', 1), (8, '/images/tv_cabinet2_2.jpg', 2),
-- 床图片
(9, '/images/bed1_1.jpg', 1), (9, '/images/bed1_2.jpg', 2), (9, '/images/bed1_3.jpg', 3),
(10, '/images/bed2_1.jpg', 1), (10, '/images/bed2_2.jpg', 2),
(11, '/images/bed3_1.jpg', 1), (11, '/images/bed3_2.jpg', 2),
-- 衣柜图片
(12, '/images/wardrobe1_1.jpg', 1), (12, '/images/wardrobe1_2.jpg', 2),
(13, '/images/wardrobe2_1.jpg', 1), (13, '/images/wardrobe2_2.jpg', 2),
-- 床头柜图片
(14, '/images/nightstand1_1.jpg', 1), (14, '/images/nightstand1_2.jpg', 2),
-- 餐桌图片
(15, '/images/dining_table1_1.jpg', 1), (15, '/images/dining_table1_2.jpg', 2),
(16, '/images/dining_table2_1.jpg', 1), (16, '/images/dining_table2_2.jpg', 2),
-- 餐椅图片
(17, '/images/dining_chair1_1.jpg', 1), (17, '/images/dining_chair1_2.jpg', 2),
(18, '/images/dining_chair2_1.jpg', 1), (18, '/images/dining_chair2_2.jpg', 2),
-- 酒柜图片
(19, '/images/wine_cabinet1_1.jpg', 1), (19, '/images/wine_cabinet1_2.jpg', 2),
-- 办公桌图片
(20, '/images/office_table1_1.jpg', 1), (20, '/images/office_table1_2.jpg', 2),
(21, '/images/office_table2_1.jpg', 1), (21, '/images/office_table2_2.jpg', 2),
-- 办公椅图片
(22, '/images/office_chair1_1.jpg', 1), (22, '/images/office_chair1_2.jpg', 2),
(23, '/images/office_chair2_1.jpg', 1), (23, '/images/office_chair2_2.jpg', 2),
-- 文件柜图片
(24, '/images/file_cabinet1_1.jpg', 1), (24, '/images/file_cabinet1_2.jpg', 2);

-- 插入测试商品规格
INSERT INTO `product_spec` (`product_id`, `spec_name`, `spec_value`) VALUES
-- 沙发规格
(1, '颜色', '灰色'), (1, '颜色', '米色'), (1, '颜色', '蓝色'), (1, '尺寸', '1.8m'), (1, '尺寸', '2.0m'), (1, '尺寸', '2.2m'),
(2, '颜色', '灰色'), (2, '颜色', '米色'), (2, '颜色', '绿色'), (2, '尺寸', '1.8m'), (2, '尺寸', '2.0m'),
(3, '颜色', '黑色'), (3, '颜色', '棕色'), (3, '颜色', '米色'), (3, '尺寸', '1.8m'), (3, '尺寸', '2.0m'), (3, '尺寸', '2.4m'),
-- 茶几规格
(4, '颜色', '原木色'), (4, '颜色', '白色'), (4, '颜色', '黑色'), (4, '尺寸', '1.0m'), (4, '尺寸', '1.2m'),
(5, '颜色', '透明'), (5, '颜色', '黑色'), (5, '颜色', '白色'), (5, '尺寸', '0.8m'), (5, '尺寸', '1.0m'),
(6, '颜色', '白色'), (6, '颜色', '黑色'), (6, '颜色', '灰色'), (6, '尺寸', '1.0m'), (6, '尺寸', '1.2m'),
-- 电视柜规格
(7, '颜色', '白色'), (7, '颜色', '原木色'), (7, '颜色', '黑色'), (7, '尺寸', '1.5m'), (7, '尺寸', '1.8m'), (7, '尺寸', '2.0m'),
(8, '颜色', '原木色'), (8, '颜色', '胡桃木色'), (8, '颜色', '白色'), (8, '尺寸', '1.6m'), (8, '尺寸', '1.8m'),
-- 床规格
(9, '颜色', '原木色'), (9, '颜色', '胡桃木色'), (9, '颜色', '白色'), (9, '尺寸', '1.5m'), (9, '尺寸', '1.8m'),
(10, '颜色', '灰色'), (10, '颜色', '米色'), (10, '颜色', '蓝色'), (10, '尺寸', '1.5m'), (10, '尺寸', '1.8m'),
(11, '颜色', '灰色'), (11, '颜色', '米色'), (11, '颜色', '棕色'), (11, '尺寸', '1.8m'), (11, '尺寸', '2.0m'),
-- 衣柜规格
(12, '颜色', '白色'), (12, '颜色', '原木色'), (12, '颜色', '灰色'), (12, '尺寸', '1.6m'), (12, '尺寸', '1.8m'), (12, '尺寸', '2.0m'),
(13, '颜色', '白色'), (13, '颜色', '原木色'), (13, '颜色', '胡桃木色'), (13, '尺寸', '1.5m'), (13, '尺寸', '1.8m'),
-- 床头柜规格
(14, '颜色', '白色'), (14, '颜色', '原木色'), (14, '颜色', '黑色'),
-- 餐桌规格
(15, '颜色', '原木色'), (15, '颜色', '白色'), (15, '颜色', '胡桃木色'), (15, '尺寸', '1.2m'), (15, '尺寸', '1.4m'), (15, '尺寸', '1.6m'),
(16, '颜色', '白色'), (16, '颜色', '黑色'), (16, '颜色', '灰色'), (16, '尺寸', '1.2m'), (16, '尺寸', '1.4m'),
-- 餐椅规格
(17, '颜色', '灰色'), (17, '颜色', '米色'), (17, '颜色', '黑色'),
(18, '颜色', '原木色'), (18, '颜色', '白色'), (18, '颜色', '胡桃木色'),
-- 酒柜规格
(19, '颜色', '白色'), (19, '颜色', '原木色'), (19, '颜色', '黑色'), (19, '尺寸', '0.8m'), (19, '尺寸', '1.0m'),
-- 办公桌规格
(20, '颜色', '白色'), (20, '颜色', '原木色'), (20, '颜色', '黑色'), (20, '尺寸', '1.2m'), (20, '尺寸', '1.4m'),
(21, '颜色', '白色'), (21, '颜色', '原木色'), (21, '颜色', '黑色'), (21, '尺寸', '1.4m'), (21, '尺寸', '1.6m'),
-- 办公椅规格
(22, '颜色', '黑色'), (22, '颜色', '灰色'), (22, '颜色', '蓝色'),
(23, '颜色', '黑色'), (23, '颜色', '灰色'), (23, '颜色', '红色'),
-- 文件柜规格
(24, '颜色', '白色'), (24, '颜色', '灰色'), (24, '颜色', '黑色'), (24, '尺寸', '0.8m'), (24, '尺寸', '0.9m');

-- 插入测试地址
INSERT INTO `address` (`user_id`, `name`, `phone`, `province`, `city`, `district`, `detail_address`, `is_default`) VALUES
(2, '张三', '13800138001', '北京市', '北京市', '朝阳区', '朝阳区建国路88号', 1),
(2, '张三', '13800138001', '北京市', '北京市', '海淀区', '海淀区中关村大街1号', 0),
(3, '李四', '13800138002', '上海市', '上海市', '浦东新区', '浦东新区陆家嘴环路1000号', 1),
(3, '李四', '13800138002', '上海市', '上海市', '徐汇区', '徐汇区漕溪北路595号', 0),
(4, '王五', '13800138003', '广州市', '广州市', '天河区', '天河区天河路208号', 1);

-- 插入测试购物车数据
INSERT INTO `cart` (`user_id`, `product_id`, `quantity`, `selected`) VALUES
(2, 1, 1, 1),
(2, 4, 2, 1),
(2, 9, 1, 0),
(3, 2, 1, 1),
(3, 15, 1, 1),
(4, 3, 1, 1),
(4, 12, 1, 1);

-- ============================================
-- 3. 数据插入完成
-- ============================================

SELECT '数据重置完成！' AS message;
SELECT CONCAT('用户数量: ', COUNT(*)) AS info FROM `user`;
SELECT CONCAT('分类数量: ', COUNT(*)) AS info FROM `category`;
SELECT CONCAT('商品数量: ', COUNT(*)) AS info FROM `product`;
SELECT CONCAT('商品图片数量: ', COUNT(*)) AS info FROM `product_image`;
SELECT CONCAT('商品规格数量: ', COUNT(*)) AS info FROM `product_spec`;
SELECT CONCAT('地址数量: ', COUNT(*)) AS info FROM `address`;
SELECT CONCAT('购物车数量: ', COUNT(*)) AS info FROM `cart`;
furniture_db