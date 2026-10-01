package com.gjx.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.gjx.entity.OrderItem;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Mapper
public interface OrderItemMapper extends BaseMapper<OrderItem> {

    /**
     * 查询热销商品（按销量降序）
     * <p>
     * 使用 SQL GROUP BY 聚合替代全量加载到内存再排序的方式。
     */
    @Select("SELECT oi.product_id AS id, oi.product_name AS name, oi.price, " +
            "SUM(oi.quantity) AS salesCount " +
            "FROM order_item oi " +
            "GROUP BY oi.product_id, oi.product_name, oi.price " +
            "ORDER BY salesCount DESC " +
            "LIMIT #{limit}")
    List<Map<String, Object>> getHotProducts(@Param("limit") int limit);

    // ==================== 商家仪表板专用聚合 ====================
    // 以下 4 个方法共同特征：商家归属一律经 order_item → product.merchant_id 判定
    // （order.merchant_id 列未映射、无索引、代码不使用），且只统计有效订单
    // status IN (1,2,3) 即 已付款/已发货/已完成，与商家端销售额口径保持一致。
    // 统一放在本 Mapper 内，避免把「订单项聚合」逻辑散落到 OrderMapper。

    /**
     * 统计本商家商品的成交总额
     * <p>
     * 关键修复：原实现按 {@code order.total_amount} 求和，而 total_amount 是<b>整张订单</b>的金额；
     * 一张订单可能包含多个商家的商品，导致每个商家都按全额计数、各商家求和 ≠ 平台真实 GMV。
     * 本方法改为按「订单项 × 本商家商品」聚合，只累加属于该商家的 order_item 金额。
     *
     * @param merchantId 商家 ID
     * @return 本商家商品在有效订单中的成交总额，无数据时返回 0
     */
    @Select("SELECT COALESCE(SUM(oi.price * oi.quantity), 0) " +
            "FROM order_item oi " +
            "JOIN `order` o ON o.id = oi.order_id " +
            "JOIN product p ON p.id = oi.product_id " +
            "WHERE p.merchant_id = #{merchantId} AND o.status IN (1, 2, 3)")
    BigDecimal selectMerchantSales(@Param("merchantId") Long merchantId);

    /**
     * 查询指定商家的热销商品（按销量降序）
     * <p>
     * 与 {@link #getHotProducts(int)} 的区别：本方法在 SQL 内<b>先按 merchant_id 过滤再 LIMIT</b>。
     * 原实现是「先取全平台 Top N，再用 Java 过滤本商家」，若本商家商品不在平台前 N 名，
     * 结果会为空或残缺——中小商家将永远看不到自己的热销榜。
     * <p>
     * 聚合口径：按 {@code product_id} 分组（原 SQL 把 price 放进 GROUP BY，
     * 同一商品历史成交价不同会产生多行）。展示价取 {@code MAX(oi.price)}，销量为累计 quantity。
     *
     * @param merchantId 商家 ID
     * @param limit      返回条数
     * @return 热销商品列表（id / name / price / salesCount）
     */
    @Select("SELECT oi.product_id AS id, MAX(oi.product_name) AS name, MAX(oi.price) AS price, " +
            "SUM(oi.quantity) AS salesCount " +
            "FROM order_item oi " +
            "JOIN product p ON p.id = oi.product_id " +
            "JOIN `order` o ON o.id = oi.order_id " +
            "WHERE p.merchant_id = #{merchantId} AND o.status IN (1, 2, 3) " +
            "GROUP BY oi.product_id " +
            "ORDER BY salesCount DESC " +
            "LIMIT #{limit}")
    List<Map<String, Object>> getHotProductsByMerchant(@Param("merchantId") Long merchantId,
                                                       @Param("limit") int limit);

    /**
     * 按日聚合指定商家的营收趋势
     * <p>
     * 原实现循环 7 天、每天一次 selectList（共 7 次查询）。本方法合并为单条
     * {@code GROUP BY DATE(create_time)} 查询，并同时过滤订单状态（只计有效订单）。
     * 金额口径与 {@link #selectMerchantSales(Long)} 一致（按 order_item 聚合），
     * 使「近 7 天销售额之和」与「总销售额」口径自洽。
     *
     * @param merchantId 商家 ID
     * @param start      起始时间（含）
     * @param end        结束时间（不含）
     * @return 每日聚合结果（statDate / orders / sales），仅包含有数据的日期
     */
    @Select("SELECT DATE(o.create_time) AS statDate, COUNT(DISTINCT o.id) AS orders, " +
            "COALESCE(SUM(oi.price * oi.quantity), 0) AS sales " +
            "FROM `order` o " +
            "JOIN order_item oi ON oi.order_id = o.id " +
            "JOIN product p ON p.id = oi.product_id " +
            "WHERE p.merchant_id = #{merchantId} AND o.status IN (1, 2, 3) " +
            "AND o.create_time >= #{start} AND o.create_time < #{end} " +
            "GROUP BY DATE(o.create_time)")
    List<Map<String, Object>> selectMerchantDailySales(@Param("merchantId") Long merchantId,
                                                       @Param("start") LocalDateTime start,
                                                       @Param("end") LocalDateTime end);

    /**
     * 按订单状态统计指定商家的订单数（订单漏斗）
     * <p>
     * 原实现按 4 个状态循环 COUNT（共 4 次查询）。本方法合并为单条 {@code GROUP BY status} 查询，
     * 并用 {@code COUNT(DISTINCT o.id)} 去重（一张订单可能含本商家的多个商品）。
     *
     * @param merchantId 商家 ID
     * @return 各状态的订单数（status / cnt），仅包含 0-3 四个漏斗状态
     */
    @Select("SELECT o.status AS status, COUNT(DISTINCT o.id) AS cnt " +
            "FROM `order` o " +
            "JOIN order_item oi ON oi.order_id = o.id " +
            "JOIN product p ON p.id = oi.product_id " +
            "WHERE p.merchant_id = #{merchantId} AND o.status IN (0, 1, 2, 3) " +
            "GROUP BY o.status")
    List<Map<String, Object>> selectMerchantOrderStatusCounts(@Param("merchantId") Long merchantId);
}
