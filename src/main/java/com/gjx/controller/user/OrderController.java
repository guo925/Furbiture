package com.gjx.controller.user;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.gjx.common.R;
import com.gjx.common.ResultCode;
import com.gjx.dto.request.RefundRequest;
import com.gjx.entity.Order;
import com.gjx.entity.OrderItem;
import com.gjx.service.IOrderService;
import com.gjx.service.IOrderItemService;
import com.gjx.util.AuthenticationUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 订单控制器
 */
@RestController
@RequestMapping("/api/orders")
@Tag(name = "订单管理", description = "订单相关接口")
@RequiredArgsConstructor
public class OrderController {

    private final AuthenticationUtil authUtil;

    private final IOrderService orderService;

    private final IOrderItemService orderItemService;

    /**
     * 创建订单
     * @param addressId 地址ID
     * @param cartItemIds 购物车商品ID列表
     * @param request HTTP请求
     * @return 订单信息
     */
    @Operation(summary = "创建订单", description = "从购物车选中商品创建订单")
    @PostMapping
    public R<Order> createOrder(@RequestParam Long addressId,
                         @RequestParam List<Long> cartItemIds,
                         HttpServletRequest request) {
        Long userId = authUtil.getUserIdFromRequest(request);
        Order order = orderService.createOrder(userId, addressId, cartItemIds);
        return R.ok(order);
    }

    /**
     * 模拟支付
     * @param orderNo 订单号
     * @param request HTTP请求
     * @return 支付结果
     */
    @Operation(summary = "模拟支付", description = "模拟订单支付")
    @PostMapping("/pay")
    public R<?> pay(@RequestParam String orderNo, HttpServletRequest request) {
        Long userId = authUtil.getUserIdFromRequest(request);
        orderService.mockPay(orderNo, userId);
        return R.ok("支付成功");
    }

    /**
     * 取消订单
     * @param orderNo 订单号
     * @param request HTTP请求
     * @return 取消结果
     */
    @Operation(summary = "取消订单", description = "取消未支付的订单")
    @PostMapping("/cancel")
    public R<?> cancel(@RequestParam String orderNo, HttpServletRequest request) {
        Long userId = authUtil.getUserIdFromRequest(request);
        orderService.cancelOrder(orderNo, userId);
        return R.ok("订单已取消");
    }

    /**
     * 确认收货
     * @param orderNo 订单号
     * @param request HTTP请求
     * @return 确认结果
     */
    @Operation(summary = "确认收货", description = "确认收到商品")
    @PostMapping("/receive")
    public R<?> receive(@RequestParam String orderNo, HttpServletRequest request) {
        Long userId = authUtil.getUserIdFromRequest(request);
        orderService.confirmReceive(orderNo, userId);
        return R.ok("已确认收货");
    }

    /**
     * 申请退款
     * @param orderNo 订单号
     * @param body 请求体，reason 为退款原因
     * @param request HTTP请求
     * @return 退款结果
     */
    @Operation(summary = "申请退款", description = "对已付款或已发货订单申请退款")
    @PostMapping("/{orderNo}/refund")
    public R<?> requestRefund(@PathVariable String orderNo,
                              @Valid @RequestBody(required = false) RefundRequest body,
                              HttpServletRequest request) {
        Long userId = authUtil.getUserIdFromRequest(request);
        String reason = body != null ? body.getReason() : null;
        orderService.requestRefund(orderNo, userId, reason);
        return R.ok("退款申请已提交");
    }

    /**
     * 获取用户订单列表（分页）
     * @param status 订单状态（可选，可重复传入以覆盖多个状态，如 4 已取消 + 5 已退款）
     * @param keyword 商品名称 / 订单号关键词（可选）
     * @param startDate 下单开始日期 yyyy-MM-dd（可选）
     * @param endDate 下单结束日期 yyyy-MM-dd（可选，含当天）
     * @param page 页码，从 1 开始（可选，默认 1）
     * @param size 每页大小（可选，默认 20，上限 100）
     * @param request HTTP请求
     * @return 订单分页结果，data 结构为 { records: [...], total, size, current, pages }
     */
    @Operation(summary = "获取用户订单列表", description = "分页获取当前用户的订单列表，支持状态筛选、关键词搜索与下单日期区间筛选")
    @GetMapping
    public R<Page<Map<String, Object>>> list(@RequestParam(required = false) List<Integer> status,
                  @RequestParam(required = false) String keyword,
                  @RequestParam(required = false) String startDate,
                  @RequestParam(required = false) String endDate,
                  @RequestParam(required = false) Integer page,
                  @RequestParam(required = false) Integer size,
                  HttpServletRequest request) {
        Long userId = authUtil.getUserIdFromRequest(request);
        Page<Order> orderPage = orderService.pageByUserId(userId, status, keyword, startDate, endDate, page, size);

        // 一次取回本页所有订单的明细并按订单ID分组，避免在循环里逐单查询（N+1）
        List<Long> orderIds = orderPage.getRecords().stream().map(Order::getId).toList();
        Map<Long, List<OrderItem>> itemsByOrderId = orderService.getOrderItemsByOrderIds(orderIds);

        Page<Map<String, Object>> resultPage =
                new Page<>(orderPage.getCurrent(), orderPage.getSize(), orderPage.getTotal());
        resultPage.setRecords(orderPage.getRecords().stream().map(order -> {
            Map<String, Object> item = new HashMap<>();
            item.put("id", order.getId());
            item.put("orderNo", order.getOrderNo());
            item.put("userId", order.getUserId());
            item.put("addressId", order.getAddressId());
            item.put("totalAmount", order.getTotalAmount());
            item.put("status", order.getStatus());
            item.put("payTime", order.getPayTime());
            item.put("deliveryTime", order.getDeliveryTime());
            item.put("finishTime", order.getFinishTime());
            item.put("cancelTime", order.getCancelTime());
            item.put("createTime", order.getCreateTime());
            item.put("orderItems", itemsByOrderId.getOrDefault(order.getId(), List.of()));
            return item;
        }).toList());
        return R.ok(resultPage);
    }

    /**
     * 获取用户各状态订单数量（订单页签角标）
     * <p>
     * 关键词与日期区间与列表接口一致，但**不含状态**——角标本身就是按状态拆分的。
     * 两者共用同一套过滤条件，保证角标数字与列表内容始终自洽。
     *
     * @param keyword 商品名称 / 订单号关键词（可选）
     * @param startDate 下单开始日期 yyyy-MM-dd（可选）
     * @param endDate 下单结束日期 yyyy-MM-dd（可选，含当天）
     * @param request HTTP请求
     * @return 状态码 → 订单数，如 { "0": 2, "3": 5 }；数量为 0 的状态码不出现
     */
    @Operation(summary = "获取用户订单状态统计", description = "按状态统计当前用户订单数量，受关键词与下单日期区间影响")
    @GetMapping("/stats")
    public R<Map<Integer, Long>> stats(@RequestParam(required = false) String keyword,
                                       @RequestParam(required = false) String startDate,
                                       @RequestParam(required = false) String endDate,
                                       HttpServletRequest request) {
        Long userId = authUtil.getUserIdFromRequest(request);
        return R.ok(orderService.countByStatus(userId, keyword, startDate, endDate));
    }

    /**
     * 获取订单详情
     * @param orderNo 订单号
     * @param request HTTP请求
     * @return 订单详情
     */
    @Operation(summary = "获取订单详情", description = "根据订单号获取订单详情")
    @GetMapping("/{orderNo}")
    public R<?> detail(@PathVariable String orderNo, HttpServletRequest request) {
        Long userId = authUtil.getUserIdFromRequest(request);
        Order order = orderService.getByOrderNo(orderNo, userId);
        if (order == null) {
            return R.error(ResultCode.NOT_FOUND, "订单不存在");
        }
        List<?> orderItems = orderItemService.listByOrderId(order.getId());
        java.util.Map<String, Object> result = new java.util.HashMap<>();
        result.put("order", order);
        result.put("items", orderItems);
        return R.ok(result);
    }
}
