package com.gjx.service;

import java.util.List;
import java.util.Map;

/**
 * 商家仪表板（经营罗盘）服务接口。
 *
 * <p><b>为什么需要这一层（分层理由）：</b>仪表板的 5 个接口此前把「查商家商品 → 反查订单/订单项
 * → 聚合统计」的编排全部写在 {@link com.gjx.controller.merchant.MerchantDashboardController} 里，
 * 该 Controller 因此注入了 {@code OrderMapper}、{@code OrderItemMapper}，违反分层红线。
 * 统计口径（销售额按 {@code order_item} 聚合并只计 {@code status IN (1,2,3)}；归属一律经
 * {@code order_item → product.merchant_id}）属于业务规则，收口到 Service 后可被复用与单测，
 * Controller 退化为「取商家 ID → 调接口 → 包装 {@code R}」。
 */
public interface IMerchantDashboardService {

    /**
     * 顶部统计卡片：商品数、待处理订单数、总销售额、今日订单数。
     *
     * @param merchantId 商家 ID
     * @return 含 {@code productCount} / {@code pendingOrderCount} / {@code totalSales} / {@code todayOrderCount}
     */
    Map<String, Object> getDashboard(Long merchantId);

    /**
     * 营收趋势（近 7 天），恒返回 7 个数据点，无成交的日期补 0。
     *
     * @param merchantId 商家 ID
     * @return 每个元素含 {@code date}(MM-dd) / {@code orders} / {@code sales}
     */
    List<Map<String, Object>> getRevenueTrend(Long merchantId);

    /**
     * 商品品类分布。
     *
     * @param merchantId 商家 ID
     * @return 每个元素含 {@code name}(分类名) / {@code value}(商品数)
     */
    List<Map<String, Object>> getCategoryDistribution(Long merchantId);

    /**
     * 订单漏斗（待付款/已付款/已发货/已完成）。
     *
     * @param merchantId 商家 ID
     * @return 固定四个键 {@code pending} / {@code paid} / {@code delivered} / {@code completed}
     */
    Map<String, Long> getOrderFunnel(Long merchantId);

    /**
     * 热销商品 Top N（先按商家过滤再 LIMIT，避免取到平台榜里不属于本商家的商品）。
     *
     * @param merchantId 商家 ID
     * @return 热销商品列表（{@code id} / {@code name} / {@code price} / {@code salesCount}）
     */
    List<Map<String, Object>> getTopProducts(Long merchantId);
}
