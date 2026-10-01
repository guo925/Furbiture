package com.gjx.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 通知类型枚举。
 *
 * <p><b>为什么需要它：</b>{@code notification.type} 是 {@code varchar(50)}，且带索引——
 * 说明它天然是要被「按类型筛选/分组」的字段。但此前 {@code INotificationService.push()}
 * 零调用者，类型值从来没有被真正定义过，谁想用都得自己现编一个字符串
 * （"order" / "ORDER" / "OrderDelivered" 各写各的），索引立刻失去意义。
 * 收敛成枚举后，写库的值只有这一组，前端也可以按同一组常量做图标与跳转分支。
 *
 * <p><b>命名约定：</b>{@code 领域_事件}。前端若要按类型显示不同图标，直接 switch 本枚举的 name。
 */
@Getter
@AllArgsConstructor
public enum NotificationTypeEnum {

    /** 买家已付款，通知订单涉及的商家尽快发货 */
    ORDER_PAID("新订单待发货"),

    /** 商家已发货，通知买家注意收货 */
    ORDER_DELIVERED("订单已发货"),

    /** 买家确认收货，通知商家订单已完成 */
    ORDER_COMPLETED("订单已完成"),

    /** 买家申请退款，通知商家处理 */
    ORDER_REFUNDED("订单退款申请"),

    /** 超时未支付被系统自动取消，通知买家 */
    ORDER_TIMEOUT_CANCELLED("订单超时取消"),

    /** 商家入驻申请通过，通知申请人 */
    MERCHANT_AUDIT_APPROVED("入驻申请已通过"),

    /** 商家入驻申请被拒，通知申请人 */
    MERCHANT_AUDIT_REJECTED("入驻申请未通过");

    /**
     * 该类型通知的默认标题。
     *
     * <p>调用方仍可传入自定义标题覆盖；此默认值的作用是让「同一种类型的通知标题一致」，
     * 避免同一事件在不同调用点写成「订单已发货」/「您的订单已经发货了」两种表述。
     */
    private final String defaultTitle;
}
