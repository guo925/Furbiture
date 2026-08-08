package com.gjx.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 订单状态枚举
 * <p>
 * 用于替代代码中的魔法数字 0-5，提高可读性和可维护性。
 * 使用示例：OrderStatusEnum.PENDING_PAYMENT.getCode() 替代直接写 0
 *
 * @author Furbiture Team
 * @since 2026-08-08
 */
@Getter
@AllArgsConstructor
public enum OrderStatusEnum {

    /**
     * 待付款：用户已下单但尚未支付
     */
    PENDING_PAYMENT(0, "待付款"),

    /**
     * 已付款：用户已支付，等待商家发货
     */
    PAID(1, "已付款"),

    /**
     * 已发货：商家已发货，等待用户确认收货
     */
    DELIVERED(2, "已发货"),

    /**
     * 已完成：用户已确认收货，订单完成
     */
    COMPLETED(3, "已完成"),

    /**
     * 已取消：订单已取消（仅待付款状态可取消）
     */
    CANCELLED(4, "已取消"),

    /**
     * 已退款：订单已退款
     */
    REFUNDED(5, "已退款");

    /**
     * 状态码（对应数据库 status 字段值）
     */
    private final Integer code;

    /**
     * 状态中文描述
     */
    private final String description;

    /**
     * 根据状态码获取枚举实例
     *
     * @param code 状态码
     * @return 对应的枚举实例，未匹配到时返回 null
     */
    public static OrderStatusEnum fromCode(Integer code) {
        if (code == null) {
            return null;
        }
        for (OrderStatusEnum status : values()) {
            if (status.getCode().equals(code)) {
                return status;
            }
        }
        return null;
    }

    /**
     * 判断是否可以取消（仅待付款状态可取消）
     */
    public boolean canCancel() {
        return this == PENDING_PAYMENT;
    }

    /**
     * 判断是否可以支付（仅待付款状态可支付）
     */
    public boolean canPay() {
        return this == PENDING_PAYMENT;
    }

    /**
     * 判断是否可以发货（仅已付款状态可发货）
     */
    public boolean canDeliver() {
        return this == PAID;
    }

    /**
     * 判断是否可以确认收货（仅已发货状态可确认收货）
     */
    public boolean canConfirmReceive() {
        return this == DELIVERED;
    }
}
