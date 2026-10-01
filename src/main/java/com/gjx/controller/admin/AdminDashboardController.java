package com.gjx.controller.admin;

import com.gjx.common.R;
import com.gjx.service.IProductService;
import com.gjx.service.IOrderService;
import com.gjx.service.IUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 管理员仪表板控制器
 */
@RestController
@RequestMapping("/api/admin/dashboard")
@Tag(name = "管理员仪表板", description = "管理员仪表板相关接口")
@RequiredArgsConstructor
public class AdminDashboardController {

    private final IProductService productService;
    
    private final IOrderService orderService;
    
    private final IUserService userService;

    /**
     * 获取仪表板统计数据
     * @return 统计数据
     */
    @Operation(summary = "获取仪表板统计数据", description = "获取商品、订单、销售额和用户的统计数据")
    @GetMapping
    public R<Map<String, Object>> getDashboardStats() {
        Map<String, Object> stats = new HashMap<>();
        
        // 商品总数
        long productCount = productService.count();
        stats.put("productCount", productCount);
        
        // 总订单数
        long totalOrderCount = orderService.countTotalOrders();
        stats.put("totalOrderCount", totalOrderCount);
        
        // 今日订单数
        int todayOrderCount = orderService.countTodayOrders();
        stats.put("todayOrderCount", todayOrderCount);
        
        // 总销售额
        BigDecimal totalSales = orderService.getTotalSales();
        stats.put("totalSales", totalSales);

        // 今日销售额
        BigDecimal todaySales = orderService.getTodaySales();
        stats.put("todaySales", todaySales);
        
        // 用户总数
        long userCount = userService.count();
        stats.put("userCount", userCount);
        
        return R.ok(stats);
    }
    
    /**
     * 获取销售趋势数据
     * @return 销售趋势数据
     */
    @Operation(summary = "获取销售趋势数据", description = "获取最近7天的销售趋势数据")
    @GetMapping("/sales-trend")
    public R<List<Map<String, Object>>> getSalesTrend() {
        List<Map<String, Object>> trendData = new ArrayList<>();
        
        // 计算最近7天的日期
        LocalDate today = LocalDate.now();
        for (int i = 6; i >= 0; i--) {
            LocalDate date = today.minusDays(i);
            String dateStr = date.toString();
            
            // 计算当天的销售额
            BigDecimal sales = orderService.getSalesByDate(dateStr);
            
            // 计算当天的订单数量
            int orderCount = orderService.countOrdersByDate(dateStr);
            
            Map<String, Object> data = new HashMap<>();
            data.put("date", dateStr);
            data.put("sales", sales);
            data.put("orderCount", orderCount);
            trendData.add(data);
        }
        
        return R.ok(trendData);
    }
    
    /**
     * 获取热门商品数据
     * @return 热门商品数据
     */
    @Operation(summary = "获取热门商品数据", description = "获取按销售数量排序的热门商品")
    @GetMapping("/hot-products")
    public R<List<Map<String, Object>>> getHotProducts() {
        List<Map<String, Object>> hotProducts = orderService.getHotProducts(10);
        return R.ok(hotProducts);
    }
}
