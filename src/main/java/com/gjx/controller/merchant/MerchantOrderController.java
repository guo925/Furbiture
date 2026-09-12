package com.gjx.controller.merchant;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.gjx.common.R;
import com.gjx.common.ResultCode;
import com.gjx.entity.*;
import com.gjx.enums.OrderStatusEnum;
import com.gjx.mapper.OrderItemMapper;
import com.gjx.mapper.OrderMapper;
import com.gjx.service.IOrderService;
import com.gjx.service.IProductService;
import com.gjx.service.IUserService;
import com.gjx.util.AuthenticationUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 商家订单管理控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/merchant")
@Tag(name = "商家订单管理", description = "商家订单查询和状态更新接口")
public class MerchantOrderController {

    @Autowired
    private IProductService productService;

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private OrderItemMapper orderItemMapper;

    @Autowired
    private IUserService userService;

    @Autowired
    private IOrderService orderService;

    @Operation(summary = "获取订单列表", description = "获取包含商家商品的订单分页列表")
    @GetMapping("/orders")
    public R<?> getOrders(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) Integer status,
            HttpServletRequest request) {
        Long merchantId = getMerchantId(request);

        List<Product> merchantProducts = productService.list(
                new LambdaQueryWrapper<Product>().eq(Product::getMerchantId, merchantId));
        List<Long> productIds = merchantProducts.stream().map(Product::getId).toList();

        if (productIds.isEmpty()) return R.ok(new Page<>(page, size));

        List<Long> orderIds = orderItemMapper.selectList(
                new LambdaQueryWrapper<OrderItem>().in(OrderItem::getProductId, productIds))
                .stream().map(OrderItem::getOrderId).distinct().toList();

        if (orderIds.isEmpty()) return R.ok(new Page<>(page, size));

        LambdaQueryWrapper<Order> orderQuery = new LambdaQueryWrapper<>();
        orderQuery.in(Order::getId, orderIds);
        if (status != null) orderQuery.eq(Order::getStatus, status);
        orderQuery.orderByDesc(Order::getCreateTime);

        return R.ok(orderMapper.selectPage(new Page<>(page, size), orderQuery));
    }

    @Operation(summary = "获取订单详情", description = "获取订单详细信息（仅返回商家自己的商品项）")
    @GetMapping("/orders/{orderNo}")
    public R<Map<String, Object>> getOrderDetail(@PathVariable String orderNo, HttpServletRequest request) {
        Long merchantId = getMerchantId(request);

        Order order = orderMapper.selectOne(
                new LambdaQueryWrapper<Order>().eq(Order::getOrderNo, orderNo));
        if (order == null) return R.error(ResultCode.NOT_FOUND, "订单不存在");

        List<OrderItem> allItems = orderItemMapper.selectList(
                new LambdaQueryWrapper<OrderItem>().eq(OrderItem::getOrderId, order.getId()));

        List<Long> merchantProductIds = productService.list(
                new LambdaQueryWrapper<Product>().eq(Product::getMerchantId, merchantId))
                .stream().map(Product::getId).toList();

        List<OrderItem> merchantItems = allItems.stream()
                .filter(item -> merchantProductIds.contains(item.getProductId())).toList();

        // 归属校验：订单不含本商家商品时，视为不存在，避免商家通过订单号探测/查看他人订单
        if (merchantItems.isEmpty()) return R.error(ResultCode.NOT_FOUND, "订单不存在");

        Map<String, Object> result = new HashMap<>();
        result.put("order", order);
        result.put("orderItems", merchantItems);
        return R.ok(result);
    }

    @Operation(summary = "更新订单状态", description = "商家更新订单状态（当前仅支持发货）")
    @PutMapping("/orders/{orderNo}/status")
    public R<?> updateOrderStatus(@PathVariable String orderNo, @RequestBody Map<String, Integer> body, HttpServletRequest request) {
        Long merchantId = getMerchantId(request);
        Integer newStatus = body.get("status");
        if (newStatus == null) return R.error(ResultCode.PARAM_ERROR, "状态不能为空");

        // 状态白名单：商家仅可执行"发货"，其余流转由用户（确认收货）或管理员负责，
        // 避免商家自行把订单改成已完成/已退款等状态
        if (!OrderStatusEnum.DELIVERED.getCode().equals(newStatus)) {
            return R.error(ResultCode.FORBIDDEN, "商家仅可执行发货操作");
        }

        // 归属校验与状态流转在 Service 层统一处理（订单必须含本商家商品，且当前为已付款）
        orderService.deliverOrderByMerchant(orderNo, merchantId);
        log.info("[商家更新订单状态] orderNo={}, newStatus={}, merchantId={}", orderNo, newStatus, merchantId);
        return R.ok("更新成功");
    }

    private Long getMerchantId(HttpServletRequest request) {
        return userService.findByUsername(AuthenticationUtil.getUsernameFromRequest(request)).getId();
    }
}
