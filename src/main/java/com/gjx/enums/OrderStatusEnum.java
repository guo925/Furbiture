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

    /**
     * 判断当前状态是否允许迁移到目标状态。
     *
     * <p><b>为什么需要它：</b>订单状态不能"想改就改"。若只校验"目标值在枚举内"，
     * 管理员仍可把「已取消」改成「已付款」——这会凭空制造出一笔从未发生过的交易，
     * 破坏按 status=PAID 统计的销售额与对账。
     *
     * <p>允许的迁移（正向流转 + 退款支线）：
     * <pre>
     * 待付款(0) → 已付款(1) | 已取消(4)
     * 已付款(1) → 已发货(2) | 已退款(5)
     * 已发货(2) → 已完成(3) | 已退款(5)
     * 已完成(3) → 已退款(5)
     * 已取消(4) / 已退款(5) → 终态，不可再流转
     * </pre>
     *
     * <p><b>关于终态：</b>「已取消」「已退款」被设计为终态。如果确实需要纠正一笔错误取消，
     * 不要放宽这里的规则，而应该走一个**独立的、带审计日志的**纠错操作——
     * 否则任何误操作都能无声地改写历史交易。
     *
     * @param target 目标状态
     * @return 允许迁移返回 true
     */
    public boolean canTransitionTo(OrderStatusEnum target) {
        if (target == null) {
            return false;
        }
        return switch (this) {
            case PENDING_PAYMENT -> target == PAID || target == CANCELLED;
            case PAID -> target == DELIVERED || target == REFUNDED;
            case DELIVERED -> target == COMPLETED || target == REFUNDED;
            case COMPLETED -> target == REFUNDED;
            case CANCELLED, REFUNDED -> false;   // 终态
        };
    }
}
