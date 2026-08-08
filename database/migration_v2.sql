-- ============================================================
-- Furbiture 数据库优化迁移 v2（修复版）
-- 日期：2026-08-08
-- 说明：索引优化 + 软删除支持 + 商家审核流程
-- 修复：使用存储过程安全添加，避免重复索引和语法错误
-- ============================================================

-- ============================================
-- 1. 软删除字段
-- ============================================
DELIMITER //
CREATE PROCEDURE IF NOT EXISTS add_deleted_columns()
BEGIN
  IF NOT EXISTS (SELECT * FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'user' AND COLUMN_NAME = 'deleted') THEN
    ALTER TABLE `user` ADD COLUMN `deleted` TINYINT(1) DEFAULT 0 COMMENT '逻辑删除：0-未删除 1-已删除';
  END IF;
  IF NOT EXISTS (SELECT * FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'product' AND COLUMN_NAME = 'deleted') THEN
    ALTER TABLE `product` ADD COLUMN `deleted` TINYINT(1) DEFAULT 0 COMMENT '逻辑删除：0-未删除 1-已删除';
  END IF;
  IF NOT EXISTS (SELECT * FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'category' AND COLUMN_NAME = 'deleted') THEN
    ALTER TABLE `category` ADD COLUMN `deleted` TINYINT(1) DEFAULT 0 COMMENT '逻辑删除：0-未删除 1-已删除';
  END IF;
END //
DELIMITER ;
CALL add_deleted_columns();
DROP PROCEDURE IF EXISTS add_deleted_columns;

-- ============================================
-- 2. 索引优化（先检查后创建，避免重复）
-- ============================================
DELIMITER //
CREATE PROCEDURE IF NOT EXISTS add_indexes()
BEGIN
  DECLARE CONTINUE HANDLER FOR 1061 BEGIN END;  -- 忽略重复索引错误

  -- 商品搜索索引
  IF NOT EXISTS (SELECT * FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'product' AND INDEX_NAME = 'idx_product_name') THEN
    CREATE INDEX idx_product_name ON `product`(`name`);
  END IF;
  IF NOT EXISTS (SELECT * FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'product' AND INDEX_NAME = 'idx_product_category_status') THEN
    CREATE INDEX idx_product_category_status ON `product`(`category_id`, `status`);
  END IF;
  IF NOT EXISTS (SELECT * FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'product' AND INDEX_NAME = 'idx_product_merchant') THEN
    CREATE INDEX idx_product_merchant ON `product`(`merchant_id`);
  END IF;
  IF NOT EXISTS (SELECT * FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'product' AND INDEX_NAME = 'idx_product_sales') THEN
    CREATE INDEX idx_product_sales ON `product`(`sales`);
  END IF;

  -- 订单复合查询索引（idx_order_user_status 是复合索引，与单独 idx_user_id 不重复）
  IF NOT EXISTS (SELECT * FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'order' AND INDEX_NAME = 'idx_order_user_status') THEN
    CREATE INDEX idx_order_user_status ON `order`(`user_id`, `status`);
  END IF;
  IF NOT EXISTS (SELECT * FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'order' AND INDEX_NAME = 'idx_order_create_time') THEN
    CREATE INDEX idx_order_create_time ON `order`(`create_time`);
  END IF;

  -- 订单项查询索引
  IF NOT EXISTS (SELECT * FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'order_item' AND INDEX_NAME = 'idx_order_item_order') THEN
    CREATE INDEX idx_order_item_order ON `order_item`(`order_id`);
  END IF;
  IF NOT EXISTS (SELECT * FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'order_item' AND INDEX_NAME = 'idx_order_item_product') THEN
    CREATE INDEX idx_order_item_product ON `order_item`(`product_id`);
  END IF;

  -- 分类查询索引
  IF NOT EXISTS (SELECT * FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'category' AND INDEX_NAME = 'idx_category_parent') THEN
    CREATE INDEX idx_category_parent ON `category`(`parent_id`);
  END IF;
END //
DELIMITER ;
CALL add_indexes();
DROP PROCEDURE IF EXISTS add_indexes;

-- ============================================
-- 3. 商家审核表
-- ============================================
CREATE TABLE IF NOT EXISTS `merchant_audit` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `store_name` VARCHAR(100) COMMENT '店铺名称',
    `store_description` VARCHAR(500) COMMENT '店铺简介',
    `business_license` VARCHAR(255) COMMENT '营业执照图片URL',
    `contact_name` VARCHAR(50) COMMENT '联系人',
    `contact_phone` VARCHAR(20) COMMENT '联系电话',
    `status` TINYINT DEFAULT 0 COMMENT '审核状态：0-待审核 1-已通过 2-已拒绝',
    `reject_reason` VARCHAR(255) COMMENT '拒绝原因',
    `audit_time` DATETIME COMMENT '审核时间',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_audit_user (`user_id`),
    INDEX idx_audit_status (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商家审核表';

SELECT 'migration_v2 执行完成（索引重复问题已修复）' AS status;
