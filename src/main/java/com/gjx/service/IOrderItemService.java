package com.gjx.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.gjx.entity.OrderItem;

import java.util.List;

/**
 * 订单详情服务接口
 */
public interface IOrderItemService extends IService<OrderItem> {
    /**
     * 根据订单ID获取订单详情列表
     * @param orderId 订单ID
     * @return 订单详情列表
     */
    List<OrderItem> listByOrderId(Long orderId);
}