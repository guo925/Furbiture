package com.gjx.service.Impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.gjx.common.BusinessException;
import com.gjx.entity.*;
import com.gjx.enums.OrderStatusEnum;
import com.gjx.mapper.*;
import com.gjx.service.IOrderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
public class OrderServiceImpl extends ServiceImpl<OrderMapper, Order> implements IOrderService {

    @Autowired
    private CartMapper cartMapper;
    @Autowired
    private ProductMapper productMapper;
    @Autowired
    private OrderItemMapper orderItemMapper;
    @Autowired
    private AddressMapper addressMapper;

    @Override
    @Transactional
    public Order createOrder(Long userId, Long addressId, List<Long> cartItemIds) {
        // 1. 查询选中的购物车商品
        List<Cart> cartItems = cartMapper.selectBatchIds(cartItemIds);
        if (cartItems.isEmpty()) throw new BusinessException("请选择商品");

        // 2. 校验地址
        Address address = addressMapper.selectById(addressId);
        if (address == null || !address.getUserId().equals(userId)) {
            throw new BusinessException("收货地址无效");
        }

        // 3. 校验库存并锁定（使用悲观锁）
        BigDecimal total = BigDecimal.ZERO;
        for (Cart item : cartItems) {
            Product product = productMapper.selectForUpdate(item.getProductId());
            if (product.getStock() < item.getQuantity()) {
                throw new BusinessException("商品 " + product.getName() + " 库存不足");
            }
            total = total.add(product.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())));
        }

        // 4. 扣减库存
        for (Cart item : cartItems) {
            int rows = productMapper.decreaseStock(item.getProductId(), item.getQuantity());
            if (rows == 0) throw new BusinessException("扣减库存失败");
        }

        // 5. 创建订单
        Order order = new Order();
        order.setOrderNo(UUID.randomUUID().toString().replace("-", "").substring(0, 32));
        order.setUserId(userId);
        order.setAddressId(addressId);
        order.setTotalAmount(total);
        order.setStatus(OrderStatusEnum.PENDING_PAYMENT.getCode());
        save(order);
        log.info("[创建订单] orderNo={}, userId={}, amount={}", order.getOrderNo(), userId, total);

        // 6. 保存订单明细（复用步骤3中已查询的商品信息）
        for (Cart item : cartItems) {
            Product product = productMapper.selectById(item.getProductId());
            OrderItem orderItem = new OrderItem();
            orderItem.setOrderId(order.getId());
            orderItem.setProductId(product.getId());
            orderItem.setProductName(product.getName());
            orderItem.setProductImage(product.getMainImage());
            orderItem.setPrice(product.getPrice());
            orderItem.setQuantity(item.getQuantity());
            orderItemMapper.insert(orderItem);
        }

        // 7. 清空购物车选中的商品
        cartMapper.deleteBatchIds(cartItemIds);

        return order;
    }

    @Override
    @Transactional
    public void mockPay(String orderNo) {
        Order order = getOne(new LambdaQueryWrapper<Order>().eq(Order::getOrderNo, orderNo));
        if (order == null) throw new BusinessException("订单不存在");
        if (!order.getStatus().equals(OrderStatusEnum.PENDING_PAYMENT.getCode())) throw new BusinessException("订单状态不正确");
        order.setStatus(OrderStatusEnum.PAID.getCode());
        order.setPayTime(LocalDateTime.now());
        updateById(order);
    }

    @Override
    @Transactional
    public void cancelOrder(String orderNo) {
        Order order = getOne(new LambdaQueryWrapper<Order>().eq(Order::getOrderNo, orderNo));
        if (order == null) throw new BusinessException("订单不存在");
        if (!order.getStatus().equals(OrderStatusEnum.PENDING_PAYMENT.getCode())) throw new BusinessException("只有待付款订单可以取消");
        order.setStatus(OrderStatusEnum.CANCELLED.getCode());
        order.setCancelTime(LocalDateTime.now());
        updateById(order);

        // 恢复库存
        List<OrderItem> items = orderItemMapper.selectList(new LambdaQueryWrapper<OrderItem>().eq(OrderItem::getOrderId, order.getId()));
        for (OrderItem item : items) {
            productMapper.increaseStock(item.getProductId(), item.getQuantity());
        }
    }

    @Override
    @Transactional
    public void deliverOrder(String orderNo) {
        Order order = getOne(new LambdaQueryWrapper<Order>().eq(Order::getOrderNo, orderNo));
        if (order == null) throw new BusinessException("订单不存在");
        if (!order.getStatus().equals(OrderStatusEnum.PAID.getCode())) throw new BusinessException("只有已付款订单可以发货");
        order.setStatus(OrderStatusEnum.DELIVERED.getCode());
        order.setDeliveryTime(LocalDateTime.now());
        updateById(order);
    }

    @Override
    @Transactional
    public void confirmReceive(String orderNo) {
        Order order = getOne(new LambdaQueryWrapper<Order>().eq(Order::getOrderNo, orderNo));
        if (order == null) throw new BusinessException("订单不存在");
        if (!order.getStatus().equals(OrderStatusEnum.DELIVERED.getCode())) throw new BusinessException("只有已发货订单可以确认收货");
        order.setStatus(OrderStatusEnum.COMPLETED.getCode());
        order.setFinishTime(LocalDateTime.now());
        updateById(order);
    }

    @Override
    public List<Order> listByUserId(Long userId, Integer status) {
        LambdaQueryWrapper<Order> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Order::getUserId, userId);
        if (status != null) {
            queryWrapper.eq(Order::getStatus, status);
        }
        queryWrapper.orderByDesc(Order::getCreateTime);
        return list(queryWrapper);
    }

    @Override
    public Order getByOrderNo(String orderNo, Long userId) {
        LambdaQueryWrapper<Order> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Order::getOrderNo, orderNo);
        if (userId != null) {
            queryWrapper.eq(Order::getUserId, userId);
        }
        return getOne(queryWrapper);
    }

    @Override
    public Page<Order> adminListOrders(Integer page, Integer size, String orderNo, Integer status, Long userId, String startDate, String endDate) {
        LambdaQueryWrapper<Order> queryWrapper = new LambdaQueryWrapper<>();
        if (orderNo != null && !orderNo.isEmpty()) {
            queryWrapper.like(Order::getOrderNo, orderNo);
        }
        if (status != null) {
            queryWrapper.eq(Order::getStatus, status);
        }
        if (userId != null) {
            queryWrapper.eq(Order::getUserId, userId);
        }
        if (startDate != null && !startDate.isEmpty()) {
            LocalDateTime startDateTime = LocalDateTime.parse(startDate + "T00:00:00");
            queryWrapper.ge(Order::getCreateTime, startDateTime);
        }
        if (endDate != null && !endDate.isEmpty()) {
            LocalDateTime endDateTime = LocalDateTime.parse(endDate + "T23:59:59");
            queryWrapper.le(Order::getCreateTime, endDateTime);
        }
        queryWrapper.orderByDesc(Order::getCreateTime);
        return page(new Page<>(page, size), queryWrapper);
    }

    @Override
    public int countTodayOrders() {
        return baseMapper.countTodayOrders();
    }

    @Override
    public double getTodaySales() {
        String today = LocalDate.now().toString();
        String tomorrow = LocalDate.now().plusDays(1).toString();
        BigDecimal sales = baseMapper.selectTotalSalesByDateAndStatus(
                today, tomorrow, OrderStatusEnum.PAID.getCode());
        return sales != null ? sales.doubleValue() : 0;
    }

    @Override
    public long countTotalOrders() {
        return count();
    }

    @Override
    public double getTotalSales() {
        BigDecimal sales = baseMapper.selectTotalSalesByStatus(OrderStatusEnum.PAID.getCode());
        return sales != null ? sales.doubleValue() : 0;
    }

    @Override
    public double getSalesByDate(String date) {
        String nextDay = LocalDate.parse(date).plusDays(1).toString();
        BigDecimal sales = baseMapper.selectTotalSalesByDateAndStatus(
                date, nextDay, OrderStatusEnum.PAID.getCode());
        return sales != null ? sales.doubleValue() : 0;
    }

    @Override
    public int countOrdersByDate(String date) {
        String nextDay = LocalDate.parse(date).plusDays(1).toString();
        return baseMapper.countOrdersByDate(date, nextDay);
    }

    @Override
    public List<Map<String, Object>> getHotProducts(int limit) {
        // 使用 SQL GROUP BY 聚合替代全量加载到内存
        return orderItemMapper.getHotProducts(limit);
    }
}