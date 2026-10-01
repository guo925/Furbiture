package com.gjx.controller.merchant;

import com.gjx.common.R;
import com.gjx.service.IMerchantDashboardService;
import com.gjx.service.IUserService;
import com.gjx.util.AuthenticationUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 商家仪表板控制器 — 经营罗盘。
 *
 * <p>分层：Controller 只负责取当前商家 ID 并转发；全部统计编排与口径
 * （销售额按 {@code order_item} 聚合、归属经 {@code product.merchant_id} 判定、
 * 趋势补 7 个点、热销榜先过滤后 LIMIT 等）已下沉到 {@link IMerchantDashboardService}。
 * 此前本类直接注入 {@code OrderMapper} / {@code OrderItemMapper} 违反了
 * 「Controller 不得直接依赖 Mapper」的分层红线。
 */
@Slf4j
@RestController
@RequestMapping("/api/merchant")
@Tag(name = "商家仪表板", description = "商家仪表板统计接口")
@RequiredArgsConstructor
public class MerchantDashboardController {

    private final AuthenticationUtil authUtil;

    private final IUserService userService;

    private final IMerchantDashboardService merchantDashboardService;

    @Operation(summary = "获取商家仪表板统计")
    @GetMapping("/dashboard")
    public R<Map<String, Object>> getDashboard(HttpServletRequest request) {
        Long merchantId = getMerchantId(request);
        return R.ok(merchantDashboardService.getDashboard(merchantId));
    }

    @Operation(summary = "营收趋势（近7天）")
    @GetMapping("/dashboard/revenue-trend")
    public R<List<Map<String, Object>>> getRevenueTrend(HttpServletRequest request) {
        Long merchantId = getMerchantId(request);
        return R.ok(merchantDashboardService.getRevenueTrend(merchantId));
    }

    @Operation(summary = "品类分布")
    @GetMapping("/dashboard/category-distribution")
    public R<List<Map<String, Object>>> getCategoryDistribution(HttpServletRequest request) {
        Long merchantId = getMerchantId(request);
        return R.ok(merchantDashboardService.getCategoryDistribution(merchantId));
    }

    @Operation(summary = "订单漏斗")
    @GetMapping("/dashboard/order-funnel")
    public R<Map<String, Long>> getOrderFunnel(HttpServletRequest request) {
        Long merchantId = getMerchantId(request);
        return R.ok(merchantDashboardService.getOrderFunnel(merchantId));
    }

    @Operation(summary = "热销商品Top10")
    @GetMapping("/dashboard/top-products")
    public R<List<Map<String, Object>>> getTopProducts(HttpServletRequest request) {
        Long merchantId = getMerchantId(request);
        return R.ok(merchantDashboardService.getTopProducts(merchantId));
    }

    private Long getMerchantId(HttpServletRequest request) {
        return userService.findByUsername(authUtil.getUsernameFromRequest(request)).getId();
    }
}
