-- 家具商城数据库表结构
-- 执行前请先创建数据库: CREATE DATABASE furniture_db DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
-- 然后执行: USE furniture_db;

-- ============================================
-- 1. 删除旧表（按依赖顺序倒序删除）
-- ============================================

DROP TABLE IF EXISTS `order_item`;
DROP TABLE IF EXISTS `order`;
DROP TABLE IF EXISTS `cart`;
DROP TABLE IF EXISTS `address`;
DROP TABLE IF EXISTS `product_spec`;
DROP TABLE IF EXISTS `product_image`;
DROP TABLE IF EXISTS `product`;
DROP TABLE IF EXISTS `category`;
DROP TABLE IF EXISTS `user`;

-- ============================================
-- 2. 创建新表
-- ============================================

-- 用户表
CREATE TABLE `user` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '用户ID',
  `username` VARCHAR(50) NOT NULL UNIQUE COMMENT '用户名',
  `password` VARCHAR(255) NOT NULL COMMENT '密码',
  `phone` VARCHAR(20) COMMENT '手机号',
  `email` VARCHAR(100) COMMENT '邮箱',
  `avatar` VARCHAR(255) COMMENT '头像',
  `role` VARCHAR(20) DEFAULT 'USER' COMMENT '角色：USER, ADMIN',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- 分类表
CREATE TABLE `category` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '分类ID',
  `name` VARCHAR(50) NOT NULL COMMENT '分类名称',
  `parent_id` BIGINT DEFAULT 0 COMMENT '父分类ID',
  `level` INT DEFAULT 1 COMMENT '分类级别',
  `sort_order` INT DEFAULT 0 COMMENT '排序序号',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='分类表';

-- 商品表
CREATE TABLE `product` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '商品ID',
  `name` VARCHAR(100) NOT NULL COMMENT '商品名称',
  `category_id` BIGINT COMMENT '分类ID',
  `brand` VARCHAR(50) COMMENT '品牌',
  `main_image` VARCHAR(255) COMMENT '主图片',
  `price` DECIMAL(10,2) NOT NULL COMMENT '价格',
  `stock` INT DEFAULT 0 COMMENT '库存',
  `status` INT DEFAULT 1 COMMENT '状态：1上架 0下架',
  `sales` INT DEFAULT 0 COMMENT '销量',
  `description` TEXT COMMENT '商品描述',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  INDEX `idx_category_id` (`category_id`),
  INDEX `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品表';

-- 商品图片表
CREATE TABLE `product_image` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '图片ID',
  `product_id` BIGINT NOT NULL COMMENT '商品ID',
  `image_url` VARCHAR(255) NOT NULL COMMENT '图片路径',
  `sort_order` INT DEFAULT 0 COMMENT '排序序号',
  INDEX `idx_product_id` (`product_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品图片表';

-- 商品规格表
CREATE TABLE `product_spec` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '规格ID',
  `product_id` BIGINT NOT NULL COMMENT '商品ID',
  `spec_name` VARCHAR(50) NOT NULL COMMENT '规格名称',
  `spec_value` VARCHAR(100) NOT NULL COMMENT '规格值',
  INDEX `idx_product_id` (`product_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品规格表';

-- 地址表
CREATE TABLE `address` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '地址ID',
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `name` VARCHAR(50) NOT NULL COMMENT '收货人姓名',
  `phone` VARCHAR(20) NOT NULL COMMENT '手机号',
  `province` VARCHAR(50) NOT NULL COMMENT '省',
  `city` VARCHAR(50) NOT NULL COMMENT '市',
  `district` VARCHAR(50) NOT NULL COMMENT '区',
  `detail_address` VARCHAR(255) NOT NULL COMMENT '详细地址',
  `is_default` INT DEFAULT 0 COMMENT '是否默认：1默认 0非默认',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  INDEX `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='地址表';

-- 购物车表
CREATE TABLE `cart` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '购物车ID',
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `product_id` BIGINT NOT NULL COMMENT '商品ID',
  `quantity` INT DEFAULT 1 COMMENT '商品数量',
  `selected` INT DEFAULT 1 COMMENT '是否选中：1选中 0未选中',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  UNIQUE KEY `uk_user_product` (`user_id`, `product_id`),
  INDEX `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='购物车表';

-- 订单表
CREATE TABLE `order` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '订单ID',
  `order_no` VARCHAR(50) NOT NULL UNIQUE COMMENT '订单号',
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `address_id` BIGINT COMMENT '地址ID',
  `total_amount` DECIMAL(10,2) NOT NULL COMMENT '总金额',
  `status` INT DEFAULT 0 COMMENT '状态：0待付款 1已付款 2已发货 3已完成 4已取消 5已退款',
  `pay_time` DATETIME COMMENT '支付时间',
  `delivery_time` DATETIME COMMENT '发货时间',
  `finish_time` DATETIME COMMENT '完成时间',
  `cancel_time` DATETIME COMMENT '取消时间',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  INDEX `idx_user_id` (`user_id`),
  INDEX `idx_order_no` (`order_no`),
  INDEX `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单表';

-- 订单项表
CREATE TABLE `order_item` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '订单项ID',
  `order_id` BIGINT NOT NULL COMMENT '订单ID',
  `product_id` BIGINT NOT NULL COMMENT '商品ID',
  `product_name` VARCHAR(100) NOT NULL COMMENT '商品名称',
  `product_image` VARCHAR(255) COMMENT '商品图片',
  `price` DECIMAL(10,2) NOT NULL COMMENT '商品价格',
  `quantity` INT NOT NULL COMMENT '商品数量',
  INDEX `idx_order_id` (`order_id`),
  INDEX `idx_product_id` (`product_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单项表';

-- ============================================
-- 3. 插入测试数据
-- ============================================

-- 插入测试用户
INSERT INTO `user` (`username`, `password`, `phone`, `email`, `role`) VALUES
('admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EH', '13800138000', 'admin@example.com', 'ADMIN'),
('user1', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EH', '13800138001', 'user1@example.com', 'USER');

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
('餐桌', 3, 2, 1),
('餐椅', 3, 2, 2),
('办公桌', 4, 2, 1),
('办公椅', 4, 2, 2);

-- 插入测试商品
INSERT INTO `product` (`name`, `category_id`, `brand`, `main_image`, `price`, `stock`, `status`, `sales`, `description`) VALUES
('现代简约三人沙发', 5, '宜家', '/images/sofa1.jpg', 2999.00, 50, 1, 10, '现代简约风格，舒适面料，适合客厅使用'),
('北欧实木茶几', 6, '宜家', '/images/coffee_table1.jpg', 899.00, 30, 1, 5, '北欧风格，实木材质，简约大方'),
('现代电视柜', 7, '全友', '/images/tv_cabinet1.jpg', 1299.00, 20, 1, 3, '现代简约设计，大容量储物空间'),
('实木双人床', 8, '全友', '/images/bed1.jpg', 3599.00, 15, 1, 8, '实木材质，环保健康，舒适睡眠'),
('现代简约衣柜', 9, '索菲亚', '/images/wardrobe1.jpg', 4599.00, 10, 1, 2, '大容量储物，简约设计，适合卧室'),
('实木餐桌', 10, '宜家', '/images/dining_table1.jpg', 1899.00, 25, 1, 6, '实木材质，简约大方，适合家庭使用'),
('舒适餐椅', 11, '全友', '/images/dining_chair1.jpg', 399.00, 50, 1, 12, '舒适设计，人体工学，适合长时间用餐'),
('现代办公桌', 12, '震旦', '/images/office_table1.jpg', 1599.00, 20, 1, 4, '现代简约设计，大桌面，适合办公'),
('人体工学办公椅', 13, '震旦', '/images/office_chair1.jpg', 899.00, 30, 1, 7, '人体工学设计，舒适透气，适合长时间办公');

-- 插入测试商品图片
INSERT INTO `product_image` (`product_id`, `image_url`, `sort_order`) VALUES
(1, '/images/sofa1_1.jpg', 1), (1, '/images/sofa1_2.jpg', 2), (1, '/images/sofa1_3.jpg', 3),
(2, '/images/coffee_table1_1.jpg', 1), (2, '/images/coffee_table1_2.jpg', 2),
(3, '/images/tv_cabinet1_1.jpg', 1), (3, '/images/tv_cabinet1_2.jpg', 2),
(4, '/images/bed1_1.jpg', 1), (4, '/images/bed1_2.jpg', 2), (4, '/images/bed1_3.jpg', 3),
(5, '/images/wardrobe1_1.jpg', 1), (5, '/images/wardrobe1_2.jpg', 2),
(6, '/images/dining_table1_1.jpg', 1), (6, '/images/dining_table1_2.jpg', 2),
(7, '/images/dining_chair1_1.jpg', 1), (7, '/images/dining_chair1_2.jpg', 2),
(8, '/images/office_table1_1.jpg', 1), (8, '/images/office_table1_2.jpg', 2),
(9, '/images/office_chair1_1.jpg', 1), (9, '/images/office_chair1_2.jpg', 2);

-- 插入测试商品规格
INSERT INTO `product_spec` (`product_id`, `spec_name`, `spec_value`) VALUES
(1, '颜色', '灰色'), (1, '颜色', '米色'), (1, '尺寸', '1.8m'), (1, '尺寸', '2.0m'),
(2, '颜色', '原木色'), (2, '颜色', '白色'), (2, '尺寸', '1.2m'), (2, '尺寸', '1.4m'),
(3, '颜色', '白色'), (3, '颜色', '原木色'), (3, '尺寸', '1.5m'), (3, '尺寸', '1.8m'),
(4, '颜色', '原木色'), (4, '颜色', '胡桃木色'), (4, '尺寸', '1.5m'), (4, '尺寸', '1.8m'),
(5, '颜色', '白色'), (5, '颜色', '原木色'), (5, '尺寸', '1.8m'), (5, '尺寸', '2.0m'),
(6, '颜色', '原木色'), (6, '颜色', '白色'), (6, '尺寸', '1.4m'), (6, '尺寸', '1.6m'),
(7, '颜色', '灰色'), (7, '颜色', '米色'), (7, '颜色', '黑色'),
(8, '颜色', '白色'), (8, '颜色', '原木色'), (8, '尺寸', '1.2m'), (8, '尺寸', '1.4m'),
(9, '颜色', '黑色'), (9, '颜色', '灰色'), (9, '颜色', '蓝色');

-- 插入测试地址
INSERT INTO `address` (`user_id`, `name`, `phone`, `province`, `city`, `district`, `detail_address`, `is_default`) VALUES
(2, '张三', '13800138001', '北京市', '北京市', '朝阳区', '朝阳区建国路88号', 1),
(2, '张三', '13800138001', '北京市', '北京市', '海淀区', '海淀区中关村大街1号', 0);
