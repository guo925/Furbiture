-- ============================================
-- Furbiture 数据库增量迁移 v4
-- 修复：category 表的实体/表结构漂移
--
-- 背景（2026-10-01 全量体检发现）：
--   Category 实体声明了 icon / status 两个字段，但建表脚本里从来没有它们。
--   现有开发库是**手工补过列**的，所以一直没暴露；照 schema.sql 全新建库则会缺列，
--   MyBatis-Plus 把实体所有字段拼进 SELECT → 分类接口直接报
--   `Unknown column 'icon' in 'field list'`，整块分类功能不可用。
--
-- 同时修复 parent_id 允许 NULL 的问题：
--   AdminCategoryController.create 不设 parentId → 落 NULL →
--   CategoryServiceImpl.getCategoryTree 的 Collectors.groupingBy 抛 NPE →
--   全站分类树 500。改为 NOT NULL DEFAULT 0 后由数据库兜底。
--
-- 幂等：可重复执行。
-- 执行：USE furniture_db; SOURCE database/migration_v4_category_icon_status.sql;
-- ============================================

DELIMITER //
CREATE PROCEDURE IF NOT EXISTS fix_category_columns()
BEGIN
  -- 1. 补 icon 列
  IF NOT EXISTS (SELECT * FROM information_schema.COLUMNS
                 WHERE TABLE_SCHEMA = DATABASE()
                   AND TABLE_NAME = 'category' AND COLUMN_NAME = 'icon') THEN
    ALTER TABLE `category` ADD COLUMN `icon` VARCHAR(255) DEFAULT NULL COMMENT '分类图标';
  END IF;

  -- 2. 补 status 列
  IF NOT EXISTS (SELECT * FROM information_schema.COLUMNS
                 WHERE TABLE_SCHEMA = DATABASE()
                   AND TABLE_NAME = 'category' AND COLUMN_NAME = 'status') THEN
    ALTER TABLE `category` ADD COLUMN `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态：0-禁用 1-启用';
  END IF;
END //
DELIMITER ;

CALL fix_category_columns();
DROP PROCEDURE IF EXISTS fix_category_columns;

-- 3. 把历史遗留的 parent_id = NULL 归一为 0（否则分类树会 NPE）
UPDATE `category` SET `parent_id` = 0 WHERE `parent_id` IS NULL;

-- 4. 收紧为 NOT NULL DEFAULT 0（幂等：已是 NOT NULL 时会报 1060/无变化，
--    用条件判断规避重复执行报错）
DELIMITER //
CREATE PROCEDURE IF NOT EXISTS tighten_category_parent_id()
BEGIN
  IF EXISTS (SELECT * FROM information_schema.COLUMNS
             WHERE TABLE_SCHEMA = DATABASE()
               AND TABLE_NAME = 'category' AND COLUMN_NAME = 'parent_id'
               AND IS_NULLABLE = 'YES') THEN
    ALTER TABLE `category` MODIFY COLUMN `parent_id` BIGINT NOT NULL DEFAULT 0 COMMENT '父分类ID，0 表示顶级分类';
  END IF;
END //
DELIMITER ;

CALL tighten_category_parent_id();
DROP PROCEDURE IF EXISTS tighten_category_parent_id;

-- 完成确认
SELECT COLUMN_NAME, COLUMN_TYPE, IS_NULLABLE, COLUMN_COMMENT
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'category'
ORDER BY ORDINAL_POSITION;
