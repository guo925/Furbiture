package com.gjx.task;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.gjx.entity.Order;
import com.gjx.entity.OrderItem;
import com.gjx.enums.OrderStatusEnum;
import com.gjx.mapper.OrderItemMapper;
import com.gjx.mapper.OrderMapper;
import com.gjx.mapper.ProductMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 订单超时自动取消定时任务
 * 每 5 分钟扫描一次，将超过 30 分钟未支付的订单自动取消并恢复库存
 */
@Slf4j
@Component
public class OrderTimeoutTask {

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private OrderItemMapper orderItemMapper;

    @Autowired
    private ProductMapper productMapper;

    private static final int TIMEOUT_MINUTES = 30;

    /**
     * 每 5 分钟执行一次，首次延迟 60 秒等待连接池就绪
     */
    @Scheduled(initialDelay = 60000, fixedRate = 300000)
    @Transactional
    public void cancelTimeoutOrders() {
        LocalDateTime timeout = LocalDateTime.now().minusMinutes(TIMEOUT_MINUTES);

        LambdaQueryWrapper<Order> query = new LambdaQueryWrapper<>();
        query.eq(Order::getStatus, OrderStatusEnum.PENDING_PAYMENT.getCode())
                .le(Order::getCreateTime, timeout);

        List<Order> timeoutOrders = orderMapper.selectList(query);

        if (timeoutOrders.isEmpty()) return;

        log.info("[订单超时扫描] 发现 {} 个超时未支付订单", timeoutOrders.size());

        for (Order order : timeoutOrders) {
            try {
                cancelOrder(order);
            } catch (Exception e) {
                log.error("[订单超时取消失败] orderNo={}", order.getOrderNo(), e);
            }
        }
    }

    private void cancelOrder(Order order) {
        order.setStatus(OrderStatusEnum.CANCELLED.getCode());
        order.setCancelTime(LocalDateTime.now());
        orderMapper.updateById(order);

        // 恢复库存
        List<OrderItem> items = orderItemMapper.selectList(
                new LambdaQueryWrapper<OrderItem>().eq(OrderItem::getOrderId, order.getId()));
        for (OrderItem item : items) {
            productMapper.increaseStock(item.getProductId(), item.getQuantity());
        }

        log.info("[订单超时取消] orderNo={}, 已恢复 {} 件商品库存", order.getOrderNo(), items.size());
    }
}
