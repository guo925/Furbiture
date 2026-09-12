package com.gjx.service.Impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.gjx.common.BusinessException;
import com.gjx.entity.*;
import com.gjx.enums.OrderStatusEnum;
import com.gjx.mapper.*;
import com.gjx.service.IOrderService;
import com.gjx.service.IProductService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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
    /**
     * 通过 Service 而非 Mapper 直接操作库存：库存变更是商品列表缓存的失效条件之一，
     * 走 Service 层可复用其 @CacheEvict，避免下单后前端仍看到旧库存
     */
    @Autowired
    private IProductService productService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Order createOrder(Long userId, Long addressId, List<Long> cartItemIds) {
        // 1. 查询选中的购物车商品
        List<Cart> cartItems = cartMapper.selectBatchIds(cartItemIds);
        // 归属校验：数量对不上（含不存在/已删除的 ID）或存在他人购物车项时一律拒绝，
        // 防止使用他人购物车 ID 下单并删除他人购物车数据
        if (cartItems.isEmpty() || cartItems.size() != cartItemIds.size()) {
            throw new BusinessException("购物车商品不存在");
        }
        if (cartItems.stream().anyMatch(item -> !Objects.equals(item.getUserId(), userId))) {
            throw new BusinessException("购物车商品不存在");
        }

        // 2. 校验地址
        Address address = addressMapper.selectById(addressId);
        if (address == null || !address.getUserId().equals(userId)) {
            throw new BusinessException("收货地址无效");
        }

        // 3. 校验库存并锁定（使用悲观锁）
        // 按商品ID排序后加锁，保证多个并发事务以相同顺序获取行锁，避免交叉加锁导致的死锁
        List<Cart> sortedItems = cartItems.stream()
                .sorted(Comparator.comparing(Cart::getProductId))
                .toList();

        BigDecimal total = BigDecimal.ZERO;
        Map<Long, Product> productSnapshot = new HashMap<>();
        for (Cart item : sortedItems) {
            // 防御性校验：购物车入口已限制数量为正，但历史遗留数据可能违反该约束。
            // 负数量会让下面的 `stock >= quantity` 恒真，且扣减 SQL 变成反向加库存
            if (item.getQuantity() == null || item.getQuantity() <= 0) {
                throw new BusinessException("商品数量不合法");
            }
            Product product = productMapper.selectForUpdate(item.getProductId());
            if (product == null) {
                throw new BusinessException("商品不存在");
            }
            if (product.getStock() < item.getQuantity()) {
                throw new BusinessException("商品 " + product.getName() + " 库存不足");
            }
            productSnapshot.put(product.getId(), product);
            total = total.add(product.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())));
        }

        // 4. 扣减库存（条件更新，库存不足时影响行数为 0）
        for (Cart item : sortedItems) {
            if (!productService.decreaseStock(item.getProductId(), item.getQuantity())) {
                throw new BusinessException("扣减库存失败");
            }
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

        // 6. 保存订单明细（复用步骤3中已加锁查询到的商品快照，避免重复查库）
        for (Cart item : sortedItems) {
            Product product = productSnapshot.get(item.getProductId());
            OrderItem orderItem = new OrderItem();
            orderItem.setOrderId(order.getId());
            orderItem.setProductId(product.getId());
            orderItem.setProductName(product.getName());
            orderItem.setProductImage(product.getMainImage());
            orderItem.setPrice(product.getPrice());
            orderItem.setQuantity(item.getQuantity());
            orderItemMapper.insert(orderItem);
        }

        // 7. 清空购物车选中的商品（限定当前用户，避免误删他人数据）
        cartMapper.delete(new LambdaQueryWrapper<Cart>()
                .in(Cart::getId, cartItemIds)
                .eq(Cart::getUserId, userId));

        return order;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void mockPay(String orderNo, Long userId) {
        // 归属校验：只能支付自己的订单
        Order order = getOwnedOrder(orderNo, userId);

        // 原子状态流转：仅当仍处于待付款时更新成功，避免并发重复支付
        LambdaUpdateWrapper<Order> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(Order::getId, order.getId())
                .eq(Order::getStatus, OrderStatusEnum.PENDING_PAYMENT.getCode())
                .set(Order::getStatus, OrderStatusEnum.PAID.getCode())
                .set(Order::getPayTime, LocalDateTime.now());
        if (baseMapper.update(null, updateWrapper) == 0) {
            throw new BusinessException("订单状态已变更，请刷新后重试");
        }
        log.info("[订单支付] orderNo={}, userId={}", orderNo, userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelOrder(String orderNo, Long userId) {
        // 归属校验：只能取消自己的订单
        Order order = getOwnedOrder(orderNo, userId);
        cancelPendingOrder(order, "只有待付款订单可以取消");
        log.info("[订单取消] orderNo={}, userId={}", orderNo, userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deliverOrder(String orderNo) {
        Order order = getByOrderNo(orderNo, null);
        if (order == null) throw new BusinessException("订单不存在");

        LambdaUpdateWrapper<Order> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(Order::getId, order.getId())
                .eq(Order::getStatus, OrderStatusEnum.PAID.getCode())
                .set(Order::getStatus, OrderStatusEnum.DELIVERED.getCode())
                .set(Order::getDeliveryTime, LocalDateTime.now());
        if (baseMapper.update(null, updateWrapper) == 0) {
            throw new BusinessException("只有已付款订单可以发货");
        }
        log.info("[订单发货] orderNo={}", orderNo);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deliverOrderByMerchant(String orderNo, Long merchantId) {
        Order order = getByOrderNo(orderNo, null);
        if (order == null) throw new BusinessException("订单不存在");
        // 归属校验：订单必须包含该商家的商品，否则商家可操作平台上任意订单
        if (!orderContainsMerchantProduct(order.getId(), merchantId)) {
            throw new BusinessException("订单不存在");
        }

        LambdaUpdateWrapper<Order> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(Order::getId, order.getId())
                .eq(Order::getStatus, OrderStatusEnum.PAID.getCode())
                .set(Order::getStatus, OrderStatusEnum.DELIVERED.getCode())
                .set(Order::getDeliveryTime, LocalDateTime.now());
        if (baseMapper.update(null, updateWrapper) == 0) {
            throw new BusinessException("只有已付款订单可以发货");
        }
        log.info("[商家发货] orderNo={}, merchantId={}", orderNo, merchantId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void confirmReceive(String orderNo, Long userId) {
        // 归属校验：只能确认自己的订单
        Order order = getOwnedOrder(orderNo, userId);

        LambdaUpdateWrapper<Order> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(Order::getId, order.getId())
                .eq(Order::getStatus, OrderStatusEnum.DELIVERED.getCode())
                .set(Order::getStatus, OrderStatusEnum.COMPLETED.getCode())
                .set(Order::getFinishTime, LocalDateTime.now());
        if (baseMapper.update(null, updateWrapper) == 0) {
            throw new BusinessException("只有已发货订单可以确认收货");
        }
        log.info("[确认收货] orderNo={}, userId={}", orderNo, userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void requestRefund(String orderNo, Long userId, String reason) {
        // 归属校验：只能对自己的订单申请退款
        Order order = getOwnedOrder(orderNo, userId);

        Integer currentStatus = order.getStatus();
        boolean unshipped = OrderStatusEnum.PAID.getCode().equals(currentStatus);
        boolean shipped = OrderStatusEnum.DELIVERED.getCode().equals(currentStatus);
        if (!unshipped && !shipped) {
            throw new BusinessException("只有已付款或已发货订单可以申请退款");
        }

        LambdaUpdateWrapper<Order> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(Order::getId, order.getId())
                // 以当前状态作为条件，保证并发下不会与支付/发货/取消互相覆盖
                .eq(Order::getStatus, currentStatus)
                .set(Order::getStatus, OrderStatusEnum.REFUNDED.getCode())
                .set(Order::getRefundAmount, order.getTotalAmount())
                .set(Order::getRefundReason, reason)
                .set(Order::getRefundTime, LocalDateTime.now());
        if (baseMapper.update(null, updateWrapper) == 0) {
            throw new BusinessException("订单状态已变更，请刷新后重试");
        }

        // 未发货的订单退款需归还库存；已发货订单须先完成退货入库，此处不回补，
        // 避免"货还在用户手上、库存却已恢复"导致超卖
        if (unshipped) {
            restoreStock(order.getId());
        }
        log.info("[订单退款] orderNo={}, userId={}, amount={}, reason={}",
                orderNo, userId, order.getTotalAmount(), reason);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelTimeoutOrder(Long orderId) {
        Order order = getById(orderId);
        if (order == null) return;
        // 条件更新保证同一订单只会被成功取消一次：已被用户取消或已支付的订单会更新失败并直接跳过，
        // 从而避免多实例定时任务并发执行时重复回补库存
        cancelPendingOrder(order, null);
    }

    /**
     * 原子取消待付款订单并恢复库存
     *
     * @param order           订单
     * @param failureMessage  状态不匹配时的提示；为 null 表示由调用方（定时任务）静默跳过
     */
    private void cancelPendingOrder(Order order, String failureMessage) {
        LambdaUpdateWrapper<Order> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(Order::getId, order.getId())
                .eq(Order::getStatus, OrderStatusEnum.PENDING_PAYMENT.getCode())
                .set(Order::getStatus, OrderStatusEnum.CANCELLED.getCode())
                .set(Order::getCancelTime, LocalDateTime.now());
        if (baseMapper.update(null, updateWrapper) == 0) {
            if (failureMessage != null) {
                throw new BusinessException(failureMessage);
            }
            return;
        }
        restoreStock(order.getId());
    }

    /**
     * 恢复订单中所有商品的库存
     * <p>
     * 只应在状态流转成功（影响行数为 1）后调用，否则会造成库存重复回补。
     */
    private void restoreStock(Long orderId) {
        List<OrderItem> items = orderItemMapper.selectList(
                new LambdaQueryWrapper<OrderItem>().eq(OrderItem::getOrderId, orderId));
        for (OrderItem item : items) {
            productService.increaseStock(item.getProductId(), item.getQuantity());
        }
    }

    /**
     * 查询订单并校验归属
     * <p>
     * 用户不存在或不属于该用户时，统一抛出"订单不存在"，避免通过错误信息枚举他人订单号。
     */
    private Order getOwnedOrder(String orderNo, Long userId) {
        if (userId == null) {
            throw new BusinessException("未获取到登录用户");
        }
        Order order = getByOrderNo(orderNo, userId);
        if (order == null) {
            throw new BusinessException("订单不存在");
        }
        return order;
    }

    /**
     * 判断订单是否包含指定商家的商品
     */
    private boolean orderContainsMerchantProduct(Long orderId, Long merchantId) {
        List<OrderItem> items = orderItemMapper.selectList(
                new LambdaQueryWrapper<OrderItem>().eq(OrderItem::getOrderId, orderId));
        if (items.isEmpty()) {
            return false;
        }
        List<Long> productIds = items.stream().map(OrderItem::getProductId).distinct().toList();
        return productMapper.selectCount(new LambdaQueryWrapper<Product>()
                .in(Product::getId, productIds)
                .eq(Product::getMerchantId, merchantId)) > 0;
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
