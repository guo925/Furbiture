package com.gjx.dto.response;

import com.gjx.entity.Order;
import com.gjx.enums.OrderStatusEnum;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单视图对象 — 含状态文本描述
 */
@Data
@Builder
public class OrderVO {

    private Long id;
    private String orderNo;
    private Long userId;
    private Long addressId;
    private BigDecimal totalAmount;
    private Integer status;
    private String statusText;
    private LocalDateTime payTime;
    private LocalDateTime deliveryTime;
    private LocalDateTime finishTime;
    private LocalDateTime cancelTime;
    private LocalDateTime createTime;

    public static OrderVO from(Order order) {
        OrderStatusEnum statusEnum = OrderStatusEnum.fromCode(order.getStatus());
        return OrderVO.builder()
                .id(order.getId())
                .orderNo(order.getOrderNo())
                .userId(order.getUserId())
                .addressId(order.getAddressId())
                .totalAmount(order.getTotalAmount())
                .status(order.getStatus())
                .statusText(statusEnum != null ? statusEnum.getDescription() : "未知")
                .payTime(order.getPayTime())
                .deliveryTime(order.getDeliveryTime())
                .finishTime(order.getFinishTime())
                .cancelTime(order.getCancelTime())
                .createTime(order.getCreateTime())
                .build();
    }
}
