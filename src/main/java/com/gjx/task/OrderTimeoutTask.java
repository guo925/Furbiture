package com.gjx.task;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.gjx.entity.Order;
import com.gjx.enums.OrderStatusEnum;
import com.gjx.mapper.OrderMapper;
import com.gjx.service.IOrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * 订单超时自动取消定时任务
 * 每 5 分钟扫描一次，将超时未支付的订单自动取消并恢复库存
 * <p>
 * 说明：本任务不做方法级事务，逐单调用带事务的 Service 方法，
 * 避免一个订单异常导致整批扫描回滚；库存回补的幂等性由 Service 层的条件更新保证。
 * <p>
 * <b>多实例部署：</b>见 {@link #tryAcquireScanLock()} 的说明——同一时刻只让一个实例真正扫描，
 * 但该锁是**可选优化而非正确性依赖**，获取失败时一律放行。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderTimeoutTask {

    /** 扫描互斥锁的 Redis key */
    private static final String SCAN_LOCK_KEY = "furbiture:task:order-timeout-scan";

    /**
     * 锁的存活时间。取 4 分钟，略短于 5 分钟的调度间隔：
     * 这样即使持有者崩溃且来不及释放，下一个调度周期也能拿到锁，任务不会永久停摆。
     */
    private static final Duration SCAN_LOCK_TTL = Duration.ofMinutes(4);

    /** 本实例标识，仅用于在 Redis 里看出锁被谁持有，便于排查 */
    private final String instanceId = UUID.randomUUID().toString();

    private final OrderMapper orderMapper;

    private final IOrderService orderService;

    private final StringRedisTemplate stringRedisTemplate;

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
        if (!tryAcquireScanLock()) {
            log.debug("[订单超时扫描] 其他实例正在扫描，本次跳过");
            return;
        }

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

    /**
     * 尝试获取本轮扫描的互斥锁（Redis {@code SET NX EX}，原子）。
     *
     * <p><b>为什么这个锁必须 fail-open：</b>多实例部署时，每个实例都会跑这个
     * {@code @Scheduled}，于是 N 个实例每 5 分钟重复查一遍同一批超时订单。
     * 但这**不会造成数据错误**——真正的取消动作在 Service 层用「状态条件下沉进 WHERE +
     * 判断影响行数」做了原子认领，同一订单只可能被取消一次，库存也只回补一次。
     * 也就是说，这个锁省掉的只是**无谓的重复查询**，它不承担任何正确性职责。
     *
     * <p>正因如此，Redis 不可用时必须**返回 true 放行**而不是卡住任务：
     * 若让缓存故障决定任务是否执行，一次 Redis 抖动就会让所有超时订单永远停留在「待付款」，
     * 把「少扫几次」的小问题升级成「业务规则失效」的大故障。
     *
     * <p>不显式释放锁，靠 TTL 自然过期：调度间隔 5 分钟、TTL 4 分钟，
     * 锁必然在下一轮之前失效，省掉「判断锁是否仍属于自己再删除」的复杂度
     * （那一步若用非原子的「先比较再删除」实现，反而会引入新的竞态）。
     */
    private boolean tryAcquireScanLock() {
        try {
            Boolean acquired = stringRedisTemplate.opsForValue()
                    .setIfAbsent(SCAN_LOCK_KEY, instanceId, SCAN_LOCK_TTL);
            return !Boolean.FALSE.equals(acquired);
        } catch (Exception e) {
            log.warn("[订单超时扫描] 获取互斥锁失败，降级为直接执行（幂等性由条件更新保证）: {}", e.getMessage());
            return true;
        }
    }
}
