package com.gjx.controller.user;

import com.gjx.common.R;
import com.gjx.entity.Order;
import com.gjx.service.IOrderService;
import com.gjx.service.IOrderItemService;
import com.gjx.util.AuthenticationUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
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
public class OrderController {

    @Autowired
    private IOrderService orderService;

    @Autowired
    private IOrderItemService orderItemService;

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
        Long userId = AuthenticationUtil.getUserIdFromRequest(request);
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
        Long userId = AuthenticationUtil.getUserIdFromRequest(request);
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
        Long userId = AuthenticationUtil.getUserIdFromRequest(request);
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
        Long userId = AuthenticationUtil.getUserIdFromRequest(request);
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
                              @RequestBody(required = false) Map<String, String> body,
                              HttpServletRequest request) {
        Long userId = AuthenticationUtil.getUserIdFromRequest(request);
        String reason = body != null ? body.get("reason") : null;
        orderService.requestRefund(orderNo, userId, reason);
        return R.ok("退款申请已提交");
    }

    /**
     * 获取用户订单列表
     * @param status 订单状态（可选）
     * @param request HTTP请求
     * @return 订单列表
     */
    @Operation(summary = "获取用户订单列表", description = "获取当前用户的订单列表，支持状态筛选")
    @GetMapping
    public R<List<Map<String, Object>>> list(@RequestParam(required = false) Integer status,
                  HttpServletRequest request) {
        Long userId = AuthenticationUtil.getUserIdFromRequest(request);
        List<Order> orders = orderService.listByUserId(userId, status);
        List<Map<String, Object>> result = orders.stream().map(order -> {
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
            item.put("orderItems", orderItemService.listByOrderId(order.getId()));
            return item;
        }).toList();
        return R.ok(result);
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
        Long userId = AuthenticationUtil.getUserIdFromRequest(request);
        Order order = orderService.getByOrderNo(orderNo, userId);
        if (order == null) {
            return R.error("订单不存在");
        }
        List<?> orderItems = orderItemService.listByOrderId(order.getId());
        java.util.Map<String, Object> result = new java.util.HashMap<>();
        result.put("order", order);
        result.put("items", orderItems);
        return R.ok(result);
    }
}
