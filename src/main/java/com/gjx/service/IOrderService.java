package com.gjx.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.gjx.entity.Order;

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
     * 获取用户订单列表
     * @param userId 用户ID
     * @param status 订单状态
     * @return 订单列表
     */
    List<Order> listByUserId(Long userId, Integer status);
    
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
     * @return 今日销售额
     */
    double getTodaySales();
    
    /**
     * 统计总订单数
     * @return 总订单数
     */
    long countTotalOrders();
    
    /**
     * 获取总销售额
     * @return 总销售额
     */
    double getTotalSales();
    
    /**
     * 获取指定日期的销售额
     * @param date 日期（格式：yyyy-MM-dd）
     * @return 销售额
     */
    double getSalesByDate(String date);
    
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
}