package com.gjx.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.gjx.entity.Order;
import com.gjx.entity.OrderItem;
import com.gjx.enums.OrderStatusEnum;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 订单服务接口
 */
public interface IOrderService extends IService<Order> {
    /**
     * 创建订单
     * @param userId 用户ID
     * @param addressId 地址ID
     * @param cartItemIds 购物车商品ID列表
     * @return 订单
     */
    Order createOrder(Long userId, Long addressId, List<Long> cartItemIds);
    
    /**
     * 模拟支付
     * @param orderNo 订单号
     * @param userId 当前用户ID（用于校验订单归属）
     */
    void mockPay(String orderNo, Long userId);

    /**
     * 取消订单
     * @param orderNo 订单号
     * @param userId 当前用户ID（用于校验订单归属）
     */
    void cancelOrder(String orderNo, Long userId);

    /**
     * 发货（管理员操作，不需要归属校验）
     * @param orderNo 订单号
     */
    void deliverOrder(String orderNo);

    /**
     * 商家发货（需要校验订单包含该商家的商品）
     * @param orderNo 订单号
     * @param merchantId 商家ID
     */
    void deliverOrderByMerchant(String orderNo, Long merchantId);

    /**
     * 确认收货
     * @param orderNo 订单号
     * @param userId 当前用户ID（用于校验订单归属）
     */
    void confirmReceive(String orderNo, Long userId);

    /**
     * 申请退款
     * @param orderNo 订单号
     * @param userId 当前用户ID（用于校验订单归属）
     * @param reason 退款原因
     */
    void requestRefund(String orderNo, Long userId, String reason);

    /**
     * 超时自动取消订单（定时任务调用，原子操作，重复执行不会重复回补库存）
     * @param orderId 订单ID
     */
    void cancelTimeoutOrder(Long orderId);
    
    /**
     * 分页获取用户订单列表
     * <p>
     * 原 listByUserId 会把该用户全部订单一次性查出来，订单量大时既有全表扫描风险，
     * 又让调用方在循环里逐单查明细（N+1）。改为分页后单次返回量有上限。
     *
     * <p>关键词与日期区间同样在服务端过滤：若留在调用方做，筛选只会作用于"当前这一页"，
     * 用户看到的结果会随翻页而变（同一条件在不同页命中不同订单）。
     *
     * @param userId    用户ID
     * @param statuses  订单状态集合（可选）。用集合而非单值，是为了让「退款/售后」这类
     *                  一个页签覆盖多个状态（4 已取消 + 5 已退款）的场景也能走服务端筛选
     * @param keyword   商品名称 / 订单号关键词（可选），命中其一即可
     * @param startDate 下单开始日期 yyyy-MM-dd（可选）
     * @param endDate   下单结束日期 yyyy-MM-dd（可选，含当天）
     * @param page      页码，从 1 开始；为空或非法时按 1 处理
     * @param size      每页大小；为空或非法时使用默认值，且不超过上限
     * @return 订单分页结果
     */
    Page<Order> pageByUserId(Long userId, List<Integer> statuses, String keyword,
                            String startDate, String endDate, Integer page, Integer size);

    /**
     * 统计用户各状态订单数量
     * <p>
     * 供订单页签角标使用。必须与 {@link #pageByUserId} 用同一套过滤条件（归属 + 关键词 + 日期），
     * 否则角标数字与列表内容会对不上——同一批筛选条件下，两者必须始终自洽。
     *
     * <p>返回 Map 而非 VO：状态码由 {@code OrderStatusEnum} 定义且可能增加，
     * 用「状态码 → 数量」的映射可以新增状态而不动签名；缺失的状态码表示数量为 0。
     *
     * @param userId    用户ID
     * @param keyword   商品名称 / 订单号关键词（可选）
     * @param startDate 下单开始日期 yyyy-MM-dd（可选）
     * @param endDate   下单结束日期 yyyy-MM-dd（可选，含当天）
     * @return key 为订单状态码，value 为该状态订单数；无订单的状态码不会出现在 key 中
     */
    Map<Integer, Long> countByStatus(Long userId, String keyword, String startDate, String endDate);

    /**
     * 批量查询订单明细并按订单ID分组
     * <p>
     * 供订单列表接口一次取回整页订单的明细，避免在循环中逐单查询（N+1）。
     *
     * @param orderIds 订单ID集合
     * @return key 为订单ID，value 为该订单的明细列表；无明细的订单不会出现在 key 中
     */
    Map<Long, List<OrderItem>> getOrderItemsByOrderIds(List<Long> orderIds);
    
    /**
     * 根据订单号获取订单
     * @param orderNo 订单号
     * @param userId 用户ID（非管理员时使用）
     * @return 订单
     */
    Order getByOrderNo(String orderNo, Long userId);
    
    /**
     * 获取订单列表（管理员端）
     * @param page 页码
     * @param size 每页大小
     * @param orderNo 订单号
     * @param status 订单状态
     * @param userId 用户ID
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 订单列表
     */
    Page<Order> adminListOrders(Integer page, Integer size, String orderNo, Integer status, Long userId, String startDate, String endDate);
    
    /**
     * 统计今日订单数
     * @return 今日订单数
     */
    int countTodayOrders();
    
    /**
     * 获取今日销售额
     * @return 今日销售额（无数据时为 BigDecimal.ZERO，绝不为 null）
     */
    BigDecimal getTodaySales();
    
    /**
     * 统计总订单数
     * @return 总订单数
     */
    long countTotalOrders();
    
    /**
     * 获取总销售额
     * @return 总销售额（无数据时为 BigDecimal.ZERO，绝不为 null）
     */
    BigDecimal getTotalSales();
    
    /**
     * 获取指定日期的销售额
     * @param date 日期（格式：yyyy-MM-dd）
     * @return 销售额（无数据时为 BigDecimal.ZERO，绝不为 null）
     */
    BigDecimal getSalesByDate(String date);
    
    /**
     * 统计指定日期的订单数
     * @param date 日期（格式：yyyy-MM-dd）
     * @return 订单数
     */
    int countOrdersByDate(String date);
    
    /**
     * 获取热门商品
     * @param limit 限制数量
     * @return 热门商品列表
     */
    List<Map<String, Object>> getHotProducts(int limit);

    /**
     * 管理员应急通道：把订单强制变更为目标状态，并补齐该状态应有的副作用。
     *
     * <p><b>为什么放在 Service 而不是 Controller：</b>状态变更与库存回补必须原子完成，
     * 而 ArchUnit 规则禁止 Controller 带 {@code @Transactional}。这段逻辑原先写在
     * {@code AdminOrderController} 里且只改 {@code order.status}，于是「已取消 / 已退款」的订单
     * 库存永不回补（永久少一份）、退款金额与退款时间不落库（对账对不上）。
     *
     * <p><b>库存回补口径</b>（与 {@link #requestRefund} 保持一致，只回补「货还没出库」的流转）：
     * <ul>
     *   <li>→ 已取消：来源必然是待付款，库存在下单时已扣，故必须回补。</li>
     *   <li>→ 已退款：来源是已付款（未发货）时回补；来源是已发货 / 已完成时<b>不回补</b>——
     *       货还在买家手上而库存已恢复，会造成超卖。</li>
     * </ul>
     *
     * @param orderId      订单ID
     * @param targetStatus 目标状态（调用方须已校验它是枚举内的合法值）
     * @return {@code true} 实际发生了变更；{@code false} 当前状态已等于目标状态（幂等，无写入）
     * @throws com.gjx.common.BusinessException 订单不存在 / 当前状态异常 / 迁移不被允许 / 并发冲突
     */
    boolean adminChangeStatus(Long orderId, OrderStatusEnum targetStatus);
}