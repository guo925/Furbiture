package com.gjx.service.Impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.gjx.entity.Order;
import com.gjx.entity.OrderItem;
import com.gjx.entity.Product;
import com.gjx.mapper.OrderItemMapper;
import com.gjx.mapper.OrderMapper;
import com.gjx.service.IMerchantOrderService;
import com.gjx.service.IProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 商家订单服务实现。
 *
 * <p>本类承载原先写在 {@code MerchantOrderController} 中的查询编排：归属判定
 * （订单必须含本商家商品）、分页与排序规则。实现上与原 Controller 逐句对齐，保证行为不变。
 * 归属判定统一经 {@code product.merchant_id → order_item.product_id → order.id}，
 * 不使用 {@code order.merchant_id} 列（见项目红线 §六-2）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MerchantOrderServiceImpl implements IMerchantOrderService {

    private final IProductService productService;
    private final OrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;

    @Override
    public Page<Order> pageMerchantOrders(Long merchantId, Integer page, Integer size, Integer status) {
        List<Long> productIds = merchantProductIds(merchantId);
        // 商家无商品：直接返回空页，省去后续两次查询
        if (productIds.isEmpty()) {
            return new Page<>(page, size);
        }

        List<Long> orderIds = orderItemMapper.selectList(
                        new LambdaQueryWrapper<OrderItem>().in(OrderItem::getProductId, productIds))
                .stream().map(OrderItem::getOrderId).distinct().toList();
        // 这些商品尚未产生任何订单：同为空页
        if (orderIds.isEmpty()) {
            return new Page<>(page, size);
        }

        LambdaQueryWrapper<Order> orderQuery = new LambdaQueryWrapper<>();
        orderQuery.in(Order::getId, orderIds);
        if (status != null) {
            orderQuery.eq(Order::getStatus, status);
        }
        orderQuery.orderByDesc(Order::getCreateTime);
        return orderMapper.selectPage(new Page<>(page, size), orderQuery);
    }

    @Override
    public Optional<Map<String, Object>> getOrderDetail(Long merchantId, String orderNo) {
        Order order = orderMapper.selectOne(
                new LambdaQueryWrapper<Order>().eq(Order::getOrderNo, orderNo));
        if (order == null) {
            return Optional.empty();
        }

        List<OrderItem> allItems = orderItemMapper.selectList(
                new LambdaQueryWrapper<OrderItem>().eq(OrderItem::getOrderId, order.getId()));

        List<Long> merchantProductIds = merchantProductIds(merchantId);
        List<OrderItem> merchantItems = allItems.stream()
                .filter(item -> merchantProductIds.contains(item.getProductId())).toList();

        // 归属校验：订单不含本商家商品时，视为不存在，避免商家通过订单号探测/查看他人订单
        if (merchantItems.isEmpty()) {
            return Optional.empty();
        }

        Map<String, Object> result = new HashMap<>();
        result.put("order", order);
        result.put("orderItems", merchantItems);
        return Optional.of(result);
    }

    /**
     * 取本商家全部商品 ID（归属判定起点：{@code product.merchant_id}）。
     */
    private List<Long> merchantProductIds(Long merchantId) {
        return productService.list(new LambdaQueryWrapper<Product>().eq(Product::getMerchantId, merchantId))
                .stream().map(Product::getId).toList();
    }
}
