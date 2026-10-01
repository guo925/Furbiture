-- ============================================
-- Furbiture 数据库增量迁移 v5
-- 修复：review 表重复评价问题（配合 ReviewServiceImpl 归属/防重校验）
--
-- 背景（2026-10-01 全量体检 P0）：
--   createReview 原只校验评分区间，不校验"是否真的买过"，
--   任意登录用户可对任意商品无限次刷 5 星，评分被污染。
--   Service 层已补：订单归属 + 订单已 COMPLETED + 订单含该商品 + 防重复。
--   本脚本补数据库侧的**并发兜底**：同一 (user_id, order_id, product_id)
--   只能有一条评价。
--
-- ⚠️ 加唯一键前必须先处理存量重复数据，否则 ALTER 会失败(1062)。
--    本脚本先打印重复行供核对（只读），再删除重复、每个分组只保留最小 id
--    （即最早那条），最后才加唯一键。
--
-- 幂等：可重复执行（第二次执行时无重复行、索引已存在，两步都自动跳过）。
-- 执行：USE furniture_db; SOURCE database/migration_v5_review_unique.sql;
-- ============================================

-- ------------------------------------------------------------
-- 0. 只读：先把当前存在的重复评价打印出来，供操作者核对留档
--    （若结果为空，说明无存量重复，可安全加唯一键）
--    order_id 为空的行不参与——MySQL 唯一索引把 NULL 视为互不相同，
--    历史 order_id 为 NULL 的旧数据不会阻塞加索引，也无需清理。
-- ------------------------------------------------------------
SELECT '=== 以下为待清理的重复评价（保留每个分组中 id 最小的一条） ===' AS notice;
SELECT user_id,
       order_id,
       product_id,
       COUNT(*)                       AS dup_count,
       MIN(id)                        AS keep_id,
       GROUP_CONCAT(id ORDER BY id)   AS all_ids
FROM `review`
WHERE order_id IS NOT NULL
GROUP BY user_id, order_id, product_id
HAVING COUNT(*) > 1;

-- ------------------------------------------------------------
-- 1. 清理重复：同一 (user_id, order_id, product_id) 只保留最小 id
-- ------------------------------------------------------------
DELIMITER //
CREATE PROCEDURE IF NOT EXISTS cleanup_review_duplicates()
BEGIN
  DELETE r
  FROM `review` r
  JOIN (
    SELECT user_id, order_id, product_id, MIN(id) AS keep_id
    FROM `review`
    WHERE order_id IS NOT NULL
    GROUP BY user_id, order_id, product_id
    HAVING COUNT(*) > 1
  ) d
    ON r.user_id    = d.user_id
   AND r.order_id   = d.order_id
   AND r.product_id = d.product_id
   AND r.id        <> d.keep_id;   -- 分组内除最小 id 外全部删除
END //
DELIMITER ;

CALL cleanup_review_duplicates();
DROP PROCEDURE IF EXISTS cleanup_review_duplicates;

-- ------------------------------------------------------------
-- 2. 加唯一键（先查 information_schema.STATISTICS，已存在则跳过）
--    MySQL 8.0 不支持 ALTER TABLE ... ADD ... IF NOT EXISTS（MariaDB 方言），
--    故沿用 migration_v2/v4 的存储过程写法。
-- ------------------------------------------------------------
DELIMITER //
CREATE PROCEDURE IF NOT EXISTS add_review_unique_key()
BEGIN
  IF NOT EXISTS (SELECT * FROM information_schema.STATISTICS
                 WHERE TABLE_SCHEMA = DATABASE()
                   AND TABLE_NAME = 'review'
                   AND INDEX_NAME = 'uk_user_order_product') THEN
    ALTER TABLE `review`
      ADD UNIQUE KEY `uk_user_order_product` (`user_id`, `order_id`, `product_id`);
  END IF;
END //
DELIMITER ;

CALL add_review_unique_key();
DROP PROCEDURE IF EXISTS add_review_unique_key;

-- ------------------------------------------------------------
-- 3. 完成确认：打印索引，确认唯一键已建立
-- ------------------------------------------------------------
SELECT 'migration_v5_review_unique 执行完成' AS status;
SELECT INDEX_NAME, COLUMN_NAME, NON_UNIQUE
FROM information_schema.STATISTICS
WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'review'
ORDER BY INDEX_NAME, SEQ_IN_INDEX;
