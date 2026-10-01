-- ============================================
-- Furbiture Plan B — 数据库增量迁移 v3
-- 新增表：favorite, review, notification, operation_log
-- 新增列：order.merchant_id, order.refund_*
-- 执行方式：USE furniture_db; SOURCE migration_v3_planb.sql;
-- ============================================

-- 收藏表
CREATE TABLE IF NOT EXISTS `favorite` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '收藏ID',
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `product_id` BIGINT NOT NULL COMMENT '商品ID',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '收藏时间',
  UNIQUE KEY `uk_user_product` (`user_id`, `product_id`)
  -- idx_user_id 不再单独建：被 uk_user_product 最左前缀覆盖（迁移 v7 会删存量库里的它）
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品收藏表';

-- 评价表
CREATE TABLE IF NOT EXISTS `review` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '评价ID',
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `product_id` BIGINT NOT NULL COMMENT '商品ID',
  `order_id` BIGINT COMMENT '关联订单ID',
  `rating` TINYINT NOT NULL COMMENT '评分1-5',
  `content` TEXT COMMENT '评价内容',
  `images` VARCHAR(1000) COMMENT '评价图片JSON数组',
  `is_anonymous` TINYINT DEFAULT 0 COMMENT '是否匿名：1是 0否',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '评价时间',
  -- 同一订单同一商品只能评价一次（并发兜底；迁移 v5 为存量库补同款唯一键）
  UNIQUE KEY `uk_user_order_product` (`user_id`, `order_id`, `product_id`),
  INDEX `idx_product_id` (`product_id`),
  -- idx_user_id 不再单独建：被 uk_user_order_product 最左前缀覆盖（迁移 v7 会删存量库里的它）
  INDEX `idx_order_id` (`order_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品评价表';

-- 通知表
CREATE TABLE IF NOT EXISTS `notification` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '通知ID',
  `user_id` BIGINT NOT NULL COMMENT '接收用户ID，0表示全局通知',
  `type` VARCHAR(50) NOT NULL COMMENT '通知类型：ORDER/SYSTEM/PROMOTION/STOCK',
  `title` VARCHAR(200) NOT NULL COMMENT '通知标题',
  `content` TEXT COMMENT '通知内容',
  `is_read` TINYINT DEFAULT 0 COMMENT '是否已读：1是 0否',
  `link` VARCHAR(500) COMMENT '点击跳转链接',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  INDEX `idx_user_read` (`user_id`, `is_read`),
  INDEX `idx_type` (`type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='通知表';

-- 操作日志表
CREATE TABLE IF NOT EXISTS `operation_log` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '日志ID',
  `user_id` BIGINT COMMENT '操作用户ID',
  `username` VARCHAR(50) COMMENT '操作用户名',
  `action` VARCHAR(100) NOT NULL COMMENT '操作类型：CREATE/UPDATE/DELETE/LOGIN等',
  `target` VARCHAR(200) COMMENT '操作目标描述',
  `target_id` BIGINT COMMENT '操作目标ID',
  `ip` VARCHAR(50) COMMENT '操作IP地址',
  `detail` TEXT COMMENT '操作详情JSON',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
  INDEX `idx_user_id` (`user_id`),
  INDEX `idx_action` (`action`),
  INDEX `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='操作日志表';

-- 折扣码表
CREATE TABLE IF NOT EXISTS `discount` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '折扣ID',
  `merchant_id` BIGINT NOT NULL COMMENT '商家ID',
  `code` VARCHAR(50) NOT NULL UNIQUE COMMENT '折扣码',
  `discount_type` VARCHAR(20) NOT NULL COMMENT '折扣类型：PERCENT/AMOUNT',
  `discount_value` DECIMAL(10,2) NOT NULL COMMENT '折扣值（百分比或金额）',
  `min_amount` DECIMAL(10,2) DEFAULT 0 COMMENT '最低消费金额',
  `max_uses` INT DEFAULT 0 COMMENT '最大使用次数，0表示不限',
  `used_count` INT DEFAULT 0 COMMENT '已使用次数',
  `start_time` DATETIME COMMENT '开始时间',
  `end_time` DATETIME COMMENT '结束时间',
  `status` TINYINT DEFAULT 1 COMMENT '状态：1启用 0停用',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  INDEX `idx_merchant_id` (`merchant_id`)
  -- idx_code 不再单独建：与 UNIQUE(code) 完全重复（迁移 v7 会删存量库里的它）
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='折扣码表';

-- ============================================
-- 为 order 表增加退款和商家归属字段
-- ============================================

-- 使用存储过程安全添加列（MySQL 不支持 ADD COLUMN IF NOT EXISTS）
DELIMITER //
CREATE PROCEDURE IF NOT EXISTS add_order_columns()
BEGIN
  IF NOT EXISTS (SELECT * FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'order' AND COLUMN_NAME = 'merchant_id') THEN
    ALTER TABLE `order` ADD COLUMN `merchant_id` BIGINT COMMENT '商家ID' AFTER `user_id`;
  END IF;
  IF NOT EXISTS (SELECT * FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'order' AND COLUMN_NAME = 'refund_amount') THEN
    ALTER TABLE `order` ADD COLUMN `refund_amount` DECIMAL(10,2) COMMENT '退款金额' AFTER `status`;
  END IF;
  IF NOT EXISTS (SELECT * FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'order' AND COLUMN_NAME = 'refund_reason') THEN
    ALTER TABLE `order` ADD COLUMN `refund_reason` VARCHAR(500) COMMENT '退款原因' AFTER `refund_amount`;
  END IF;
  IF NOT EXISTS (SELECT * FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'order' AND COLUMN_NAME = 'refund_time') THEN
    ALTER TABLE `order` ADD COLUMN `refund_time` DATETIME COMMENT '退款时间' AFTER `refund_reason`;
  END IF;
END //
DELIMITER ;

CALL add_order_columns();
DROP PROCEDURE IF EXISTS add_order_columns;

-- ============================================
-- 完成确认
-- ============================================
SELECT 'migration_v3_planb 执行完成' AS status;
SELECT TABLE_NAME, TABLE_COMMENT FROM information_schema.TABLES
WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME IN ('favorite', 'review', 'notification', 'operation_log', 'discount');
