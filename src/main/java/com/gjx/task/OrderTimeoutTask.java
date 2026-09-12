package com.gjx.task;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.gjx.entity.Order;
import com.gjx.enums.OrderStatusEnum;
import com.gjx.mapper.OrderMapper;
import com.gjx.service.IOrderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 订单超时自动取消定时任务
 * 每 5 分钟扫描一次，将超时未支付的订单自动取消并恢复库存
 * <p>
 * 说明：本任务不做方法级事务，逐单调用带事务的 Service 方法，
 * 避免一个订单异常导致整批扫描回滚；库存回补的幂等性由 Service 层的条件更新保证。
 */
@Slf4j
@Component
public class OrderTimeoutTask {

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private IOrderService orderService;

    /**
     * 订单支付超时时间（分钟），由 order.timeout-minutes 统一配置
     */
    @Value("${order.timeout-minutes:30}")
    private int timeoutMinutes;

    /**
     * 每 5 分钟执行一次，首次延迟 60 秒等待连接池就绪
     */
    @Scheduled(initialDelay = 60000, fixedRate = 300000)
    public void cancelTimeoutOrders() {
        LocalDateTime timeout = LocalDateTime.now().minusMinutes(timeoutMinutes);

        LambdaQueryWrapper<Order> query = new LambdaQueryWrapper<>();
        query.eq(Order::getStatus, OrderStatusEnum.PENDING_PAYMENT.getCode())
                .le(Order::getCreateTime, timeout);

        List<Order> timeoutOrders = orderMapper.selectList(query);

        if (timeoutOrders.isEmpty()) return;

        log.info("[订单超时扫描] 发现 {} 个超时未支付订单", timeoutOrders.size());

        for (Order order : timeoutOrders) {
            try {
                orderService.cancelTimeoutOrder(order.getId());
            } catch (Exception e) {
                log.error("[订单超时取消失败] orderNo={}", order.getOrderNo(), e);
            }
        }
    }
}
