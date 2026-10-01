package com.gjx.service.Impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.gjx.entity.Category;
import com.gjx.entity.Order;
import com.gjx.entity.OrderItem;
import com.gjx.entity.Product;
import com.gjx.enums.OrderStatusEnum;
import com.gjx.mapper.OrderItemMapper;
import com.gjx.mapper.OrderMapper;
import com.gjx.service.ICategoryService;
import com.gjx.service.IMerchantDashboardService;
import com.gjx.service.IProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 商家仪表板服务实现。
 *
 * <p>承载原先写在 {@code MerchantDashboardController} 中的统计编排。核心口径（务必保持不变）：
 * <ul>
 *   <li>商家归属一律经 {@code order_item → product.merchant_id} 判定；
 *       {@code order.merchant_id} 列未映射、代码不使用。</li>
 *   <li>销售额按 {@code order_item} 聚合（{@code SUM(oi.price * oi.quantity)}），
 *       而非整单 {@code total_amount}——整单金额在多商家订单中会重复计入，
 *       相关聚合 SQL 见 {@link OrderItemMapper}。</li>
 *   <li>营收趋势恒返回 7 个点（无成交的日期补 0），热销榜先按商家过滤再 LIMIT。</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MerchantDashboardServiceImpl implements IMerchantDashboardService {

    /**
     * 热销商品 Top N 的 N。原先作为字面量写在 Controller 里，这里收敛为具名常量。
     */
    private static final int TOP_PRODUCTS_LIMIT = 10;

    /**
     * 营收趋势的天数（含今天）。
     */
    private static final int REVENUE_TREND_DAYS = 7;

    /**
     * 未归类商品的展示名。
     */
    private static final String UNCATEGORIZED_NAME = "未分类";

    /**
     * 商品「在售」的状态值。
     * <p>项目暂无 ProductStatus 枚举，{@code ProductServiceImpl} 里同样是字面量 {@code 1}；
     * 此处收敛为具名常量，避免新代码再现魔法数字。
     */
    private static final int ON_SHELF_STATUS = 1;

    /**
     * 低库存告警阈值：在售商品库存小于等于该值即计入 {@code lowStockCount}。
     * <p>只统计在售商品——已下架商品卖不出去，库存再低也不构成待办。
     */
    private static final int LOW_STOCK_THRESHOLD = 10;

    private final IProductService productService;
    private final ICategoryService categoryService;
    private final OrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;

    @Override
    public Map<String, Object> getDashboard(Long merchantId) {
        List<Long> productIds = merchantProductIds(merchantId);
        List<Long> orderIds = orderIdsOfProducts(productIds);

        Map<String, Object> stats = new HashMap<>();
        stats.put("productCount", productService.count(
                new LambdaQueryWrapper<Product>().eq(Product::getMerchantId, merchantId)));
        stats.put("pendingOrderCount", orderIds.isEmpty() ? 0 : orderMapper.selectCount(
                new LambdaQueryWrapper<Order>().in(Order::getId, orderIds).eq(Order::getStatus, OrderStatusEnum.PAID.getCode())));
        stats.put("totalSales", calcTotalSales(merchantId));
        stats.put("todayOrderCount", countTodayOrders(orderIds));
        // 下面两项供商家端仪表板「交易待办」展示真实数字。
        // 此前前端那两项是硬编码文案（"库存巡检 / 1项 资料维护"），并非真实统计
        stats.put("lowStockCount", productService.count(new LambdaQueryWrapper<Product>()
                .eq(Product::getMerchantId, merchantId)
                .eq(Product::getStatus, ON_SHELF_STATUS)
                .le(Product::getStock, LOW_STOCK_THRESHOLD)));
        stats.put("offShelfCount", productService.count(new LambdaQueryWrapper<Product>()
                .eq(Product::getMerchantId, merchantId)
                .ne(Product::getStatus, ON_SHELF_STATUS)));
        return stats;
    }

    @Override
    public List<Map<String, Object>> getRevenueTrend(Long merchantId) {
        LocalDate today = LocalDate.now();
        LocalDate startDay = today.minusDays(REVENUE_TREND_DAYS - 1);

        // 单条 GROUP BY DATE(create_time) 查询取代原先「循环 7 天、每天一次 selectList」，
        // 且金额按 order_item 聚合（口径与 calcTotalSales 一致，避免整单金额在多商家订单中重复计入）。
        Map<String, Map<String, Object>> byDate = new HashMap<>();
        List<Map<String, Object>> rows = orderItemMapper.selectMerchantDailySales(
                merchantId, startDay.atStartOfDay(), today.plusDays(1).atStartOfDay());
        for (Map<String, Object> row : rows) {
            byDate.put(String.valueOf(row.get("statDate")), row);
        }

        // 补齐 7 天：无成交的日期补 0，保证前端折线图连续
        List<Map<String, Object>> trend = new ArrayList<>();
        for (int i = REVENUE_TREND_DAYS - 1; i >= 0; i--) {
            String date = today.minusDays(i).toString();
            Map<String, Object> row = byDate.get(date);
            Map<String, Object> point = new HashMap<>();
            point.put("date", date.substring(5)); // MM-DD
            point.put("orders", row == null ? 0L : ((Number) row.get("orders")).longValue());
            point.put("sales", row == null ? 0.0 : ((Number) row.get("sales")).doubleValue());
            trend.add(point);
        }
        return trend;
    }

    @Override
    public List<Map<String, Object>> getCategoryDistribution(Long merchantId) {
        List<Product> products = productService.list(
                new LambdaQueryWrapper<Product>().eq(Product::getMerchantId, merchantId));

        // 注意：Product.categoryName 是 @TableField(exist = false) 的内存字段，
        // 只有 ProductServiceImpl.fillCategoryNames() 会填充；裸 productService.list(...) 不会填。
        // 若直接读 getCategoryName() 恒为 null → 图表 100% 显示「未分类」。
        // 此处按 categoryId 批量取分类名（一次查询，避免 N+1）后自行映射。
        Set<Long> categoryIds = products.stream()
                .map(Product::getCategoryId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, String> categoryNameMap = categoryIds.isEmpty()
                ? Collections.emptyMap()
                : categoryService.listByIds(categoryIds).stream()
                        .collect(Collectors.toMap(Category::getId, Category::getName, (a, b) -> a));

        Map<String, Long> dist = products.stream()
                .collect(Collectors.groupingBy(p -> {
                    String name = p.getCategoryId() == null ? null : categoryNameMap.get(p.getCategoryId());
                    return name != null ? name : UNCATEGORIZED_NAME;
                }, Collectors.counting()));
        List<Map<String, Object>> result = new ArrayList<>();
        dist.forEach((name, count) -> {
            Map<String, Object> item = new HashMap<>();
            item.put("name", name);
            item.put("value", count);
            result.add(item);
        });
        return result;
    }

    @Override
    public Map<String, Long> getOrderFunnel(Long merchantId) {
        Map<String, Long> funnel = new LinkedHashMap<>();
        funnel.put("pending", 0L);
        funnel.put("paid", 0L);
        funnel.put("delivered", 0L);
        funnel.put("completed", 0L);

        // 单条 GROUP BY status 查询取代原先按 4 个状态循环 COUNT（4 次查询）
        for (Map<String, Object> row : orderItemMapper.selectMerchantOrderStatusCounts(merchantId)) {
            OrderStatusEnum status = OrderStatusEnum.fromCode(((Number) row.get("status")).intValue());
            long count = ((Number) row.get("cnt")).longValue();
            if (status == OrderStatusEnum.PENDING_PAYMENT) funnel.put("pending", count);
            else if (status == OrderStatusEnum.PAID) funnel.put("paid", count);
            else if (status == OrderStatusEnum.DELIVERED) funnel.put("delivered", count);
            else if (status == OrderStatusEnum.COMPLETED) funnel.put("completed", count);
        }
        return funnel;
    }

    @Override
    public List<Map<String, Object>> getTopProducts(Long merchantId) {
        // 在 SQL 内先按商家过滤再 LIMIT：原实现是「取全平台 Top10 再过滤本商家」，
        // 本商家商品若不在平台前 10 名，热销榜会为空或残缺。
        return orderItemMapper.getHotProductsByMerchant(merchantId, TOP_PRODUCTS_LIMIT);
    }

    private List<Long> merchantProductIds(Long merchantId) {
        return productService.list(new LambdaQueryWrapper<Product>().eq(Product::getMerchantId, merchantId))
                .stream().map(Product::getId).toList();
    }

    private List<Long> orderIdsOfProducts(List<Long> productIds) {
        if (productIds.isEmpty()) {
            return Collections.emptyList();
        }
        return orderItemMapper.selectList(
                        new LambdaQueryWrapper<OrderItem>().in(OrderItem::getProductId, productIds))
                .stream().map(OrderItem::getOrderId).distinct().toList();
    }

    /**
     * 统计商家总销售额。
     * <p>
     * 归属经 order_item → product.merchant_id 判定，金额按「订单项 × 本商家商品」聚合，
     * 避免用整单 total_amount 在多商家订单中重复计数（见 OrderItemMapper#selectMerchantSales）。
     */
    private BigDecimal calcTotalSales(Long merchantId) {
        BigDecimal sales = orderItemMapper.selectMerchantSales(merchantId);
        return sales != null ? sales : BigDecimal.ZERO;
    }

    /**
     * 统计今日订单数。
     * <p>
     * 用「范围条件 ge/lt」替代原 `.apply("DATE(create_time) = CURDATE()")`：
     * 对列施加函数会导致 create_time 索引失效（全表扫描），范围条件可用索引。
     */
    private long countTodayOrders(List<Long> orderIds) {
        if (orderIds.isEmpty()) {
            return 0;
        }
        LocalDate today = LocalDate.now();
        return orderMapper.selectCount(new LambdaQueryWrapper<Order>()
                .in(Order::getId, orderIds)
                .ge(Order::getCreateTime, today.atStartOfDay())
                .lt(Order::getCreateTime, today.plusDays(1).atStartOfDay()));
    }
}
