package com.gjx.task;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.gjx.entity.Order;
import com.gjx.mapper.OrderMapper;
import com.gjx.service.IOrderService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link OrderTimeoutTask} 的扫描互斥行为测试。
 *
 * <p><b>为什么单独测这个：</b>该类的方法注释里写了两条对外承诺——
 * ①「多实例时只让一个实例真正扫描」②「Redis 故障时必须放行，绝不能因此停摆」。
 * 承诺写在注释里是不会被构建验证的，一旦有人把 catch 块改成 {@code return false}，
 * 代码照样编译、测试照样全绿，但线上会变成「Redis 抖一下，所有超时订单永久停留在待付款」。
 * 这里把两条承诺各钉一个测试。
 */
@ExtendWith(MockitoExtension.class)
class OrderTimeoutTaskTest {

    @Mock
    private OrderMapper orderMapper;

    @Mock
    private IOrderService orderService;

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private OrderTimeoutTask task;

    @Test
    @DisplayName("锁已被其他实例持有 → 本轮不扫描（一次数据库查询都不该发出）")
    void skipsScanWhenLockHeldByAnotherInstance() {
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        // setIfAbsent 返回 false = key 已存在 = 别的实例正在扫
        when(valueOperations.setIfAbsent(anyString(), anyString(), any(Duration.class)))
                .thenReturn(false);

        task.cancelTimeoutOrders();

        verify(orderMapper, never()).selectList(any(Wrapper.class));
    }

    @Test
    @DisplayName("抢到锁 → 正常扫描")
    void scansWhenLockAcquired() {
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.setIfAbsent(anyString(), anyString(), any(Duration.class)))
                .thenReturn(true);
        when(orderMapper.selectList(any(Wrapper.class))).thenReturn(List.of());

        task.cancelTimeoutOrders();

        verify(orderMapper).selectList(any(Wrapper.class));
    }

    @Test
    @DisplayName("★ Redis 抛异常 → 降级为直接扫描，绝不因缓存故障让超时任务停摆")
    void failsOpenWhenRedisIsDown() {
        when(stringRedisTemplate.opsForValue())
                .thenThrow(new RuntimeException("Redis connection refused"));
        when(orderMapper.selectList(any(Wrapper.class))).thenReturn(List.of());

        task.cancelTimeoutOrders();

        // 这条断言是本次修复的核心：锁只是减少重复查询的优化，不是正确性依赖
        // （幂等性由 Service 层的条件更新保证），所以取不到锁必须继续执行。
        verify(orderMapper).selectList(any(Wrapper.class));
    }

    @Test
    @DisplayName("Redis 返回 null（连接池被关停等边界）→ 同样放行")
    void failsOpenWhenRedisReturnsNull() {
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.setIfAbsent(anyString(), anyString(), any(Duration.class)))
                .thenReturn(null);
        when(orderMapper.selectList(any(Wrapper.class))).thenReturn(List.of());

        task.cancelTimeoutOrders();

        verify(orderMapper).selectList(any(Wrapper.class));
    }

    @Test
    @DisplayName("扫描到超时订单 → 逐单交给 Service 处理（异常不中断整批）")
    void delegatesEachTimedOutOrderToService() {
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.setIfAbsent(anyString(), anyString(), any(Duration.class)))
                .thenReturn(true);
        when(orderMapper.selectList(any(Wrapper.class)))
                .thenReturn(List.of(orderWithId(11L), orderWithId(12L)));

        task.cancelTimeoutOrders();

        verify(orderService).cancelTimeoutOrder(11L);
        verify(orderService).cancelTimeoutOrder(12L);
    }

    private Order orderWithId(Long id) {
        Order order = new Order();
        order.setId(id);
        order.setOrderNo("NO-" + id);
        return order;
    }
}
