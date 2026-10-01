-- ============================================
-- Furbiture 数据库增量迁移 v6
-- 新增：order.pay_time 索引
--
-- 背景：
--   订单销售额按支付时间过滤（selectTotalSalesByDateAndStatus：
--   `WHERE pay_time >= ? AND pay_time < ? AND status = ?`），以及按支付日期出报表，
--   但 order 表此前只有 idx_user_id / idx_order_no / idx_status，pay_time 无索引。
--   订单量增长后这些统计查询会退化为全表扫描。
--
--   注意：countTodayOrders 已改用半开区间（create_time >= CURDATE() ...）以复用 create_time，
--   而本索引针对的是 pay_time 上的统计口径，两者互补。
--
-- 幂等：可重复执行（MySQL 8.0 不支持 ALTER ... ADD INDEX IF NOT EXISTS，
--       故用 information_schema.STATISTICS 判存在后再 ALTER）。
-- 执行：USE furniture_db; SOURCE database/migration_v6_order_pay_time_index.sql;
-- ============================================

DELIMITER //
CREATE PROCEDURE IF NOT EXISTS add_order_pay_time_index()
BEGIN
  IF NOT EXISTS (SELECT * FROM information_schema.STATISTICS
                 WHERE TABLE_SCHEMA = DATABASE()
                   AND TABLE_NAME = 'order'
                   AND INDEX_NAME = 'idx_pay_time') THEN
    ALTER TABLE `order` ADD INDEX `idx_pay_time` (`pay_time`);
  END IF;
END //
DELIMITER ;

CALL add_order_pay_time_index();
DROP PROCEDURE IF EXISTS add_order_pay_time_index;

-- 完成确认：列出 order 表当前索引
SELECT INDEX_NAME, COLUMN_NAME, SEQ_IN_INDEX
FROM information_schema.STATISTICS
WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'order'
ORDER BY INDEX_NAME, SEQ_IN_INDEX;
