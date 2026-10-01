-- ============================================
-- Furbiture 数据库增量迁移 v7
-- 主题：数据完整性约束 + 冗余索引清理
--
-- 背景（2026-10-01 全量体检）：
--   全库 0 个外键、0 个 CHECK 约束 → 价格/库存/数量/评分都允许负数或越界，
--   这是脏数据的根源；另有若干唯一约束缺失、一批冗余索引。
--
-- 幂等：可重复执行。MySQL 8.0 **不支持**
--   `ALTER TABLE ... ADD CONSTRAINT IF NOT EXISTS` / `ADD INDEX IF NOT EXISTS`
--   （那是 MariaDB 方言，直接执行报 ERROR 1064），故沿用 migration_v2/v4/v5/v6 的写法：
--   `CREATE PROCEDURE IF NOT EXISTS` + 查 information_schema 判存在后再 ALTER。
--
-- ⚠️ 执行前请先跑本文件顶部的「第 0 部分」只读巡检，确认没有存量违规数据。
--    本脚本对每一项都做了「先查存量、有脏数据就跳过并打印」的防御，
--    因此即使库里有历史脏数据，脚本也不会中断，但相应约束不会被创建。
--
-- 执行：USE furniture_db; SOURCE database/migration_v7_integrity_constraints.sql;
-- ============================================


-- ============================================================
-- 第 0 部分 · 只读巡检（不改任何数据，供操作者核对留档）
--   结果全部为空 = 无存量违规，可安全加约束。
-- ============================================================
SELECT '=== [0.1] CHECK 待校验项：存量违规行 ===' AS notice;
SELECT 'product.price < 0' AS rule, COUNT(*) AS bad_rows FROM `product` WHERE `price` < 0
UNION ALL SELECT 'product.stock < 0',        COUNT(*) FROM `product`    WHERE `stock` < 0
UNION ALL SELECT 'cart.quantity <= 0',       COUNT(*) FROM `cart`       WHERE `quantity` <= 0
UNION ALL SELECT 'order_item.quantity <= 0', COUNT(*) FROM `order_item` WHERE `quantity` <= 0
UNION ALL SELECT 'review.rating NOT IN 1..5',COUNT(*) FROM `review`     WHERE `rating` < 1 OR `rating` > 5
UNION ALL SELECT 'order.total_amount < 0',   COUNT(*) FROM `order`      WHERE `total_amount` < 0;

SELECT '=== [0.2] category 同级重复名（parent_id, name）===' AS notice;
SELECT `parent_id`, `name`, COUNT(*) AS dup_count, GROUP_CONCAT(`id` ORDER BY `id`) AS ids
FROM `category` GROUP BY `parent_id`, `name` HAVING COUNT(*) > 1;

SELECT '=== [0.3] 外键悬空引用巡检（核心链路）===' AS notice;
SELECT 'order_item.order_id → order.id 悬空' AS ref, COUNT(*) AS orphan_rows
  FROM `order_item` oi LEFT JOIN `order` o ON o.id = oi.order_id WHERE o.id IS NULL
UNION ALL
SELECT 'order_item.product_id → product.id 悬空', COUNT(*)
  FROM `order_item` oi LEFT JOIN `product` p ON p.id = oi.product_id WHERE p.id IS NULL;

SELECT '=== [0.4] 冗余索引实际存在情况 ===' AS notice;
SELECT `TABLE_NAME`, `INDEX_NAME`, GROUP_CONCAT(`COLUMN_NAME` ORDER BY `SEQ_IN_INDEX`) AS cols
FROM information_schema.STATISTICS
WHERE TABLE_SCHEMA = DATABASE()
  AND INDEX_NAME IN ('idx_user_id','idx_order_no','idx_category_id','idx_code',
                     'idx_order_id','idx_product_id','idx_merchant_id',
                     'uk_user_product','uk_user_order_product','idx_order_user_status',
                     'idx_product_category_status','idx_order_item_order',
                     'idx_order_item_product','idx_product_merchant','order_no','code')
GROUP BY `TABLE_NAME`, `INDEX_NAME`
ORDER BY `TABLE_NAME`, `INDEX_NAME`;


-- ============================================================
-- 第 1 部分 · CHECK 约束（MySQL 8.0.16+ 才真正强制；本项目 8.0.46）
--   逐项：先查存量违规 → 有则打印并跳过，无则创建。
--   注意：MySQL 中 NULL 参与 CHECK 结果为 UNKNOWN，视为通过，
--        因此 stock/quantity 等可空列上的 NULL 行不会被判违规。
-- ============================================================
DELIMITER //
CREATE PROCEDURE IF NOT EXISTS v7_add_check_constraints()
BEGIN
  -- 1.1 product.price >= 0
  IF NOT EXISTS (SELECT * FROM information_schema.TABLE_CONSTRAINTS
                 WHERE CONSTRAINT_SCHEMA = DATABASE() AND TABLE_NAME = 'product'
                   AND CONSTRAINT_TYPE = 'CHECK' AND CONSTRAINT_NAME = 'chk_product_price') THEN
    IF EXISTS (SELECT 1 FROM `product` WHERE `price` < 0) THEN
      SELECT '!! 跳过 chk_product_price：存在 price<0 的行，请先修复' AS warning;
      SELECT `id`, `name`, `price` FROM `product` WHERE `price` < 0;
    ELSE
      ALTER TABLE `product` ADD CONSTRAINT `chk_product_price` CHECK (`price` >= 0);
    END IF;
  END IF;

  -- 1.2 product.stock >= 0
  IF NOT EXISTS (SELECT * FROM information_schema.TABLE_CONSTRAINTS
                 WHERE CONSTRAINT_SCHEMA = DATABASE() AND TABLE_NAME = 'product'
                   AND CONSTRAINT_TYPE = 'CHECK' AND CONSTRAINT_NAME = 'chk_product_stock') THEN
    IF EXISTS (SELECT 1 FROM `product` WHERE `stock` < 0) THEN
      SELECT '!! 跳过 chk_product_stock：存在 stock<0 的行，请先修复' AS warning;
      SELECT `id`, `name`, `stock` FROM `product` WHERE `stock` < 0;
    ELSE
      ALTER TABLE `product` ADD CONSTRAINT `chk_product_stock` CHECK (`stock` >= 0);
    END IF;
  END IF;

  -- 1.3 cart.quantity > 0
  IF NOT EXISTS (SELECT * FROM information_schema.TABLE_CONSTRAINTS
                 WHERE CONSTRAINT_SCHEMA = DATABASE() AND TABLE_NAME = 'cart'
                   AND CONSTRAINT_TYPE = 'CHECK' AND CONSTRAINT_NAME = 'chk_cart_quantity') THEN
    IF EXISTS (SELECT 1 FROM `cart` WHERE `quantity` <= 0) THEN
      SELECT '!! 跳过 chk_cart_quantity：存在 quantity<=0 的行，请先修复' AS warning;
      SELECT `id`, `user_id`, `product_id`, `quantity` FROM `cart` WHERE `quantity` <= 0;
    ELSE
      ALTER TABLE `cart` ADD CONSTRAINT `chk_cart_quantity` CHECK (`quantity` > 0);
    END IF;
  END IF;

  -- 1.4 order_item.quantity > 0
  IF NOT EXISTS (SELECT * FROM information_schema.TABLE_CONSTRAINTS
                 WHERE CONSTRAINT_SCHEMA = DATABASE() AND TABLE_NAME = 'order_item'
                   AND CONSTRAINT_TYPE = 'CHECK' AND CONSTRAINT_NAME = 'chk_order_item_quantity') THEN
    IF EXISTS (SELECT 1 FROM `order_item` WHERE `quantity` <= 0) THEN
      SELECT '!! 跳过 chk_order_item_quantity：存在 quantity<=0 的行，请先修复' AS warning;
      SELECT `id`, `order_id`, `product_id`, `quantity` FROM `order_item` WHERE `quantity` <= 0;
    ELSE
      ALTER TABLE `order_item` ADD CONSTRAINT `chk_order_item_quantity` CHECK (`quantity` > 0);
    END IF;
  END IF;

  -- 1.5 review.rating BETWEEN 1 AND 5
  IF NOT EXISTS (SELECT * FROM information_schema.TABLE_CONSTRAINTS
                 WHERE CONSTRAINT_SCHEMA = DATABASE() AND TABLE_NAME = 'review'
                   AND CONSTRAINT_TYPE = 'CHECK' AND CONSTRAINT_NAME = 'chk_review_rating') THEN
    IF EXISTS (SELECT 1 FROM `review` WHERE `rating` < 1 OR `rating` > 5) THEN
      SELECT '!! 跳过 chk_review_rating：存在 rating 越界的行，请先修复' AS warning;
      SELECT `id`, `user_id`, `product_id`, `rating` FROM `review` WHERE `rating` < 1 OR `rating` > 5;
    ELSE
      ALTER TABLE `review` ADD CONSTRAINT `chk_review_rating` CHECK (`rating` BETWEEN 1 AND 5);
    END IF;
  END IF;

  -- 1.6 order.total_amount >= 0
  IF NOT EXISTS (SELECT * FROM information_schema.TABLE_CONSTRAINTS
                 WHERE CONSTRAINT_SCHEMA = DATABASE() AND TABLE_NAME = 'order'
                   AND CONSTRAINT_TYPE = 'CHECK' AND CONSTRAINT_NAME = 'chk_order_total_amount') THEN
    IF EXISTS (SELECT 1 FROM `order` WHERE `total_amount` < 0) THEN
      SELECT '!! 跳过 chk_order_total_amount：存在 total_amount<0 的行，请先修复' AS warning;
      SELECT `id`, `order_no`, `total_amount` FROM `order` WHERE `total_amount` < 0;
    ELSE
      ALTER TABLE `order` ADD CONSTRAINT `chk_order_total_amount` CHECK (`total_amount` >= 0);
    END IF;
  END IF;
END //
DELIMITER ;

CALL v7_add_check_constraints();
DROP PROCEDURE IF EXISTS v7_add_check_constraints;


-- ============================================================
-- 第 2 部分 · 唯一约束：category(parent_id, name) 同级不重名
--   discount 的 (merchant_id, code) **不加**：discount.code 在 migration_v3 中
--   已声明为**全局** UNIQUE（`code VARCHAR(50) NOT NULL UNIQUE`），
--   全局唯一已经蕴含"每商家唯一"，再加复合唯一键纯属冗余。
--   若将来要改成"每商家各自唯一"，必须先删除全局唯一键——那是语义变更，
--   不在本次范围，故此处跳过（详见报告）。
-- ============================================================
DELIMITER //
CREATE PROCEDURE IF NOT EXISTS v7_add_category_unique()
BEGIN
  IF NOT EXISTS (SELECT * FROM information_schema.STATISTICS
                 WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'category'
                   AND INDEX_NAME = 'uk_category_parent_name') THEN
    IF EXISTS (SELECT 1 FROM `category` GROUP BY `parent_id`, `name` HAVING COUNT(*) > 1) THEN
      SELECT '!! 跳过 uk_category_parent_name：存在同级重名分类，请先合并/改名' AS warning;
      SELECT `parent_id`, `name`, COUNT(*) AS dup_count, GROUP_CONCAT(`id` ORDER BY `id`) AS ids
      FROM `category` GROUP BY `parent_id`, `name` HAVING COUNT(*) > 1;
    ELSE
      ALTER TABLE `category` ADD UNIQUE KEY `uk_category_parent_name` (`parent_id`, `name`);
    END IF;
  END IF;
END //
DELIMITER ;

CALL v7_add_category_unique();
DROP PROCEDURE IF EXISTS v7_add_category_unique;


-- ============================================================
-- 第 3 部分 · 外键约束（**仅核心链路**，ON DELETE RESTRICT）
--   加外键前先巡检悬空引用；有悬空则打印并**跳过该外键**（绝不清洗数据）。
--   本次只加 2 个：
--     order_item.order_id   → order.id    （订单项脱离订单无意义；order 从不被删）
--     order_item.product_id → product.id  （防止商品被删后订单项悬挂）
--   ⚠️ RESTRICT 的含义：当订单项引用某商品时，`DELETE FROM product` 会被拒绝(errno 1451)。
--      这正是"保护订单历史"的预期行为，但需同步修改
--      AdminProductController.delete 的物理删除路径（见报告"后续动作"）。
-- ============================================================
DELIMITER //
CREATE PROCEDURE IF NOT EXISTS v7_add_core_foreign_keys()
BEGIN
  -- 3.1 order_item.order_id → order.id
  IF NOT EXISTS (SELECT * FROM information_schema.TABLE_CONSTRAINTS
                 WHERE CONSTRAINT_SCHEMA = DATABASE() AND TABLE_NAME = 'order_item'
                   AND CONSTRAINT_TYPE = 'FOREIGN KEY' AND CONSTRAINT_NAME = 'fk_order_item_order') THEN
    IF EXISTS (SELECT 1 FROM `order_item` oi LEFT JOIN `order` o ON o.id = oi.order_id WHERE o.id IS NULL) THEN
      SELECT '!! 跳过 fk_order_item_order：存在悬空 order_id，请先处理' AS warning;
      SELECT oi.`id`, oi.`order_id` FROM `order_item` oi
        LEFT JOIN `order` o ON o.id = oi.order_id WHERE o.id IS NULL;
    ELSE
      ALTER TABLE `order_item`
        ADD CONSTRAINT `fk_order_item_order` FOREIGN KEY (`order_id`)
        REFERENCES `order` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT;
    END IF;
  END IF;

  -- 3.2 order_item.product_id → product.id
  IF NOT EXISTS (SELECT * FROM information_schema.TABLE_CONSTRAINTS
                 WHERE CONSTRAINT_SCHEMA = DATABASE() AND TABLE_NAME = 'order_item'
                   AND CONSTRAINT_TYPE = 'FOREIGN KEY' AND CONSTRAINT_NAME = 'fk_order_item_product') THEN
    IF EXISTS (SELECT 1 FROM `order_item` oi LEFT JOIN `product` p ON p.id = oi.product_id WHERE p.id IS NULL) THEN
      SELECT '!! 跳过 fk_order_item_product：存在悬空 product_id，请先处理' AS warning;
      SELECT oi.`id`, oi.`product_id` FROM `order_item` oi
        LEFT JOIN `product` p ON p.id = oi.product_id WHERE p.id IS NULL;
    ELSE
      ALTER TABLE `order_item`
        ADD CONSTRAINT `fk_order_item_product` FOREIGN KEY (`product_id`)
        REFERENCES `product` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT;
    END IF;
  END IF;
END //
DELIMITER ;

CALL v7_add_core_foreign_keys();
DROP PROCEDURE IF EXISTS v7_add_core_foreign_keys;


-- ============================================================
-- 第 4 部分 · 冗余索引清理
--   规则：只删「与另一索引完全重复」或「其列是另一索引的最左前缀」的索引，
--         且**必须**覆盖它的那个索引真实存在才删（防止在某些未跑全迁移链的库上误删）。
--   最左前缀原理：`idx(a)` 与 `idx(a,b)` 并存时，`idx(a)` 能做的查询 `idx(a,b)` 都能做，
--                 故 `idx(a)` 纯属浪费写入与空间。
-- ============================================================
DELIMITER //
CREATE PROCEDURE IF NOT EXISTS v7_drop_redundant_indexes()
BEGIN
  -- 4.1 cart.idx_user_id：被 uk_user_product(user_id, product_id) 最左前缀覆盖
  IF EXISTS (SELECT * FROM information_schema.STATISTICS
             WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'cart' AND INDEX_NAME = 'idx_user_id')
     AND EXISTS (SELECT * FROM information_schema.STATISTICS
             WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'cart'
               AND INDEX_NAME = 'uk_user_product' AND SEQ_IN_INDEX = 1 AND COLUMN_NAME = 'user_id') THEN
    ALTER TABLE `cart` DROP INDEX `idx_user_id`;
  END IF;

  -- 4.2 favorite.idx_user_id：被 uk_user_product(user_id, product_id) 最左前缀覆盖
  IF EXISTS (SELECT * FROM information_schema.STATISTICS
             WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'favorite' AND INDEX_NAME = 'idx_user_id')
     AND EXISTS (SELECT * FROM information_schema.STATISTICS
             WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'favorite'
               AND INDEX_NAME = 'uk_user_product' AND SEQ_IN_INDEX = 1 AND COLUMN_NAME = 'user_id') THEN
    ALTER TABLE `favorite` DROP INDEX `idx_user_id`;
  END IF;

  -- 4.3 review.idx_user_id：被 uk_user_order_product(user_id, order_id, product_id) 最左前缀覆盖
  IF EXISTS (SELECT * FROM information_schema.STATISTICS
             WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'review' AND INDEX_NAME = 'idx_user_id')
     AND EXISTS (SELECT * FROM information_schema.STATISTICS
             WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'review'
               AND INDEX_NAME = 'uk_user_order_product' AND SEQ_IN_INDEX = 1 AND COLUMN_NAME = 'user_id') THEN
    ALTER TABLE `review` DROP INDEX `idx_user_id`;
  END IF;

  -- 4.4 order.idx_user_id：被 idx_order_user_status(user_id, status) 最左前缀覆盖
  IF EXISTS (SELECT * FROM information_schema.STATISTICS
             WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'order' AND INDEX_NAME = 'idx_user_id')
     AND EXISTS (SELECT * FROM information_schema.STATISTICS
             WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'order'
               AND INDEX_NAME = 'idx_order_user_status' AND SEQ_IN_INDEX = 1 AND COLUMN_NAME = 'user_id') THEN
    ALTER TABLE `order` DROP INDEX `idx_user_id`;
  END IF;

  -- 4.5 order.idx_order_no：与 UNIQUE(order_no) 完全重复（同列同序，只是非唯一）
  IF EXISTS (SELECT * FROM information_schema.STATISTICS
             WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'order' AND INDEX_NAME = 'idx_order_no')
     AND EXISTS (SELECT * FROM information_schema.STATISTICS
             WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'order'
               AND INDEX_NAME = 'order_no' AND NON_UNIQUE = 0) THEN
    ALTER TABLE `order` DROP INDEX `idx_order_no`;
  END IF;

  -- 4.6 product.idx_category_id：被 idx_product_category_status(category_id, status) 最左前缀覆盖
  IF EXISTS (SELECT * FROM information_schema.STATISTICS
             WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'product' AND INDEX_NAME = 'idx_category_id')
     AND EXISTS (SELECT * FROM information_schema.STATISTICS
             WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'product'
               AND INDEX_NAME = 'idx_product_category_status' AND SEQ_IN_INDEX = 1 AND COLUMN_NAME = 'category_id') THEN
    ALTER TABLE `product` DROP INDEX `idx_category_id`;
  END IF;

  -- 4.7 product.idx_merchant_id：与 idx_product_merchant(merchant_id) 完全重复
  --     （前者由 merchant_update.sql 建、后者由 migration_v2 建，属两份脚本的重复造）
  IF EXISTS (SELECT * FROM information_schema.STATISTICS
             WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'product' AND INDEX_NAME = 'idx_merchant_id')
     AND EXISTS (SELECT * FROM information_schema.STATISTICS
             WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'product' AND INDEX_NAME = 'idx_product_merchant') THEN
    ALTER TABLE `product` DROP INDEX `idx_merchant_id`;
  END IF;

  -- 4.8 discount.idx_code：与 UNIQUE(code) 完全重复
  IF EXISTS (SELECT * FROM information_schema.STATISTICS
             WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'discount' AND INDEX_NAME = 'idx_code')
     AND EXISTS (SELECT * FROM information_schema.STATISTICS
             WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'discount'
               AND INDEX_NAME = 'code' AND NON_UNIQUE = 0) THEN
    ALTER TABLE `discount` DROP INDEX `idx_code`;
  END IF;

  -- 4.9 order_item 上重复的一对：老 schema.sql 建 idx_order_id / idx_product_id，
  --     migration_v2 又建 idx_order_item_order / idx_order_item_product（同列同序，完全重复）。
  --     本次统一保留 migration_v2 的两个（schema.sql 已改成同名，全新部署不再重复）。
  IF EXISTS (SELECT * FROM information_schema.STATISTICS
             WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'order_item' AND INDEX_NAME = 'idx_order_id')
     AND EXISTS (SELECT * FROM information_schema.STATISTICS
             WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'order_item' AND INDEX_NAME = 'idx_order_item_order') THEN
    ALTER TABLE `order_item` DROP INDEX `idx_order_id`;
  END IF;

  IF EXISTS (SELECT * FROM information_schema.STATISTICS
             WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'order_item' AND INDEX_NAME = 'idx_product_id')
     AND EXISTS (SELECT * FROM information_schema.STATISTICS
             WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'order_item' AND INDEX_NAME = 'idx_order_item_product') THEN
    ALTER TABLE `order_item` DROP INDEX `idx_product_id`;
  END IF;
END //
DELIMITER ;

CALL v7_drop_redundant_indexes();
DROP PROCEDURE IF EXISTS v7_drop_redundant_indexes;


-- ============================================================
-- 第 5 部分 · 完成确认
-- ============================================================
SELECT '=== [5.1] CHECK 约束清单 ===' AS notice;
SELECT `TABLE_NAME`, `CONSTRAINT_NAME`
FROM information_schema.TABLE_CONSTRAINTS
WHERE CONSTRAINT_SCHEMA = DATABASE() AND CONSTRAINT_TYPE = 'CHECK'
ORDER BY `TABLE_NAME`, `CONSTRAINT_NAME`;

SELECT '=== [5.2] 外键清单 ===' AS notice;
SELECT `TABLE_NAME`, `CONSTRAINT_NAME`, `REFERENCED_TABLE_NAME`, `REFERENCED_COLUMN_NAME`
FROM information_schema.KEY_COLUMN_USAGE
WHERE TABLE_SCHEMA = DATABASE() AND REFERENCED_TABLE_NAME IS NOT NULL
ORDER BY `TABLE_NAME`, `CONSTRAINT_NAME`;

SELECT '=== [5.3] 唯一约束清单 ===' AS notice;
SELECT `TABLE_NAME`, `CONSTRAINT_NAME`
FROM information_schema.TABLE_CONSTRAINTS
WHERE CONSTRAINT_SCHEMA = DATABASE() AND CONSTRAINT_TYPE = 'UNIQUE'
ORDER BY `TABLE_NAME`, `CONSTRAINT_NAME`;

SELECT 'migration_v7_integrity_constraints 执行完成' AS status;
