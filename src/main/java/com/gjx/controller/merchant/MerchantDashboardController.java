package com.gjx.controller.merchant;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.gjx.common.R;
import com.gjx.entity.*;
import com.gjx.enums.OrderStatusEnum;
import com.gjx.mapper.OrderItemMapper;
import com.gjx.mapper.OrderMapper;
import com.gjx.service.IProductService;
import com.gjx.service.IUserService;
import com.gjx.util.AuthenticationUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 商家仪表板控制器 — 经营罗盘
 */
@Slf4j
@RestController
@RequestMapping("/api/merchant")
@Tag(name = "商家仪表板", description = "商家仪表板统计接口")
public class MerchantDashboardController {

    @Autowired private IProductService productService;
    @Autowired private OrderMapper orderMapper;
    @Autowired private OrderItemMapper orderItemMapper;
    @Autowired private IUserService userService;

    @Operation(summary = "获取商家仪表板统计")
    @GetMapping("/dashboard")
    public R<Map<String, Object>> getDashboard(HttpServletRequest request) {
        Long merchantId = getMerchantId(request);
        List<Long> productIds = getMerchantProductIds(merchantId);
        List<Long> orderIds = getMerchantOrderIds(productIds);

        Map<String, Object> stats = new HashMap<>();
        stats.put("productCount", productService.count(
                new LambdaQueryWrapper<Product>().eq(Product::getMerchantId, merchantId)));
        stats.put("pendingOrderCount", orderIds.isEmpty() ? 0 : orderMapper.selectCount(
                new LambdaQueryWrapper<Order>().in(Order::getId, orderIds).eq(Order::getStatus, OrderStatusEnum.PAID.getCode())));
        stats.put("totalSales", calcTotalSales(orderIds, productIds));
        stats.put("todayOrderCount", countTodayOrders(orderIds));
        return R.ok(stats);
    }

    @Operation(summary = "营收趋势（近7天）")
    @GetMapping("/dashboard/revenue-trend")
    public R<List<Map<String, Object>>> getRevenueTrend(HttpServletRequest request) {
        Long merchantId = getMerchantId(request);
        List<Long> productIds = getMerchantProductIds(merchantId);
        List<Long> orderIds = getMerchantOrderIds(productIds);
        if (orderIds.isEmpty()) return R.ok(Collections.emptyList());

        List<Map<String, Object>> trend = new ArrayList<>();
        for (int i = 6; i >= 0; i--) {
            String date = LocalDate.now().minusDays(i).toString();
            String nextDate = LocalDate.now().minusDays(i).plusDays(1).toString();
            long count = 0;
            BigDecimal sales = BigDecimal.ZERO;
            if (!orderIds.isEmpty()) {
                LambdaQueryWrapper<Order> q = new LambdaQueryWrapper<>();
                q.in(Order::getId, orderIds).ge(Order::getCreateTime, date).lt(Order::getCreateTime, nextDate);
                List<Order> dayOrders = orderMapper.selectList(q);
                count = dayOrders.size();
                for (Order o : dayOrders) {
                    sales = sales.add(o.getTotalAmount());
                }
            }
            Map<String, Object> point = new HashMap<>();
            point.put("date", date.substring(5)); // MM-DD
            point.put("orders", count);
            point.put("sales", sales.doubleValue());
            trend.add(point);
        }
        return R.ok(trend);
    }

    @Operation(summary = "品类分布")
    @GetMapping("/dashboard/category-distribution")
    public R<List<Map<String, Object>>> getCategoryDistribution(HttpServletRequest request) {
        Long merchantId = getMerchantId(request);
        List<Product> products = productService.list(
                new LambdaQueryWrapper<Product>().eq(Product::getMerchantId, merchantId));
        Map<String, Long> dist = products.stream()
                .collect(Collectors.groupingBy(p -> p.getCategoryName() != null ? p.getCategoryName() : "未分类", Collectors.counting()));
        List<Map<String, Object>> result = new ArrayList<>();
        dist.forEach((name, count) -> {
            Map<String, Object> item = new HashMap<>();
            item.put("name", name);
            item.put("value", count);
            result.add(item);
        });
        return R.ok(result);
    }

    @Operation(summary = "订单漏斗")
    @GetMapping("/dashboard/order-funnel")
    public R<Map<String, Long>> getOrderFunnel(HttpServletRequest request) {
        Long merchantId = getMerchantId(request);
        List<Long> productIds = getMerchantProductIds(merchantId);
        List<Long> orderIds = getMerchantOrderIds(productIds);
        Map<String, Long> funnel = new LinkedHashMap<>();
        funnel.put("pending", 0L);
        funnel.put("paid", 0L);
        funnel.put("delivered", 0L);
        funnel.put("completed", 0L);
        if (!orderIds.isEmpty()) {
            for (OrderStatusEnum status : OrderStatusEnum.values()) {
                String key = status.getDescription();
                long c = orderMapper.selectCount(
                        new LambdaQueryWrapper<Order>().in(Order::getId, orderIds).eq(Order::getStatus, status.getCode()));
                if (status == OrderStatusEnum.PENDING_PAYMENT) funnel.put("pending", c);
                else if (status == OrderStatusEnum.PAID) funnel.put("paid", c);
                else if (status == OrderStatusEnum.DELIVERED) funnel.put("delivered", c);
                else if (status == OrderStatusEnum.COMPLETED) funnel.put("completed", c);
            }
        }
        return R.ok(funnel);
    }

    @Operation(summary = "热销商品Top10")
    @GetMapping("/dashboard/top-products")
    public R<List<Map<String, Object>>> getTopProducts(HttpServletRequest request) {
        Long merchantId = getMerchantId(request);
        List<Product> products = productService.list(
                new LambdaQueryWrapper<Product>().eq(Product::getMerchantId, merchantId));
        if (products.isEmpty()) return R.ok(Collections.emptyList());

        Set<Long> productIds = products.stream().map(Product::getId).collect(Collectors.toSet());
        Map<Long, String> nameMap = products.stream().collect(Collectors.toMap(Product::getId, Product::getName));
        List<Map<String, Object>> hot = orderItemMapper.getHotProducts(10);
        return R.ok(hot.stream()
                .filter(item -> productIds.contains(Long.valueOf(item.get("id").toString())))
                .limit(10)
                .collect(Collectors.toList()));
    }

    private List<Long> getMerchantProductIds(Long merchantId) {
        return productService.list(new LambdaQueryWrapper<Product>().eq(Product::getMerchantId, merchantId))
                .stream().map(Product::getId).toList();
    }

    private List<Long> getMerchantOrderIds(List<Long> productIds) {
        if (productIds.isEmpty()) return Collections.emptyList();
        return orderItemMapper.selectList(
                new LambdaQueryWrapper<OrderItem>().in(OrderItem::getProductId, productIds))
                .stream().map(OrderItem::getOrderId).distinct().toList();
    }

    private BigDecimal calcTotalSales(List<Long> orderIds, List<Long> productIds) {
        if (orderIds.isEmpty() || productIds.isEmpty()) return BigDecimal.ZERO;
        // 使用已付款/已完成/已发货订单的 totalAmount 近似代替
        LambdaQueryWrapper<Order> q = new LambdaQueryWrapper<>();
        q.in(Order::getId, orderIds).in(Order::getStatus,
                OrderStatusEnum.PAID.getCode(), OrderStatusEnum.DELIVERED.getCode(), OrderStatusEnum.COMPLETED.getCode());
        return orderMapper.selectList(q).stream()
                .map(Order::getTotalAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private long countTodayOrders(List<Long> orderIds) {
        if (orderIds.isEmpty()) return 0;
        return orderMapper.selectCount(
                new LambdaQueryWrapper<Order>().in(Order::getId, orderIds).apply("DATE(create_time) = CURDATE()"));
    }

    private Long getMerchantId(HttpServletRequest request) {
        return userService.findByUsername(AuthenticationUtil.getUsernameFromRequest(request)).getId();
    }
}
