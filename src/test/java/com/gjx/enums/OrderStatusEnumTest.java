package com.gjx.enums;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 订单状态迁移规则测试。
 *
 * <p><b>为什么需要这个测试：</b>
 * 管理员可以手工改订单状态（应急通道）。原先只校验"目标值在枚举内"，
 * 于是「已取消(4) → 已付款(1)」这种迁移是被放行的——它会**凭空制造出一笔从未发生的交易**，
 * 让按 {@code status=PAID} 统计的销售额与对账全部失真。
 *
 * <p>本测试把"哪些迁移合法"变成可执行断言。若有人放宽了 {@code canTransitionTo} 的规则，
 * 这些用例会立刻变红。
 */
class OrderStatusEnumTest {

    @Nested
    @DisplayName("fromCode：状态码解析")
    class FromCode {

        @ParameterizedTest
        @CsvSource({"0, PENDING_PAYMENT", "1, PAID", "2, DELIVERED", "3, COMPLETED", "4, CANCELLED", "5, REFUNDED"})
        @DisplayName("合法状态码应解析出对应枚举")
        void shouldResolveKnownCodes(int code, OrderStatusEnum expected) {
            assertEquals(expected, OrderStatusEnum.fromCode(code));
        }

        @Test
        @DisplayName("非法状态码应返回 null（而不是抛异常或默默返回某个默认值）")
        void shouldReturnNullForUnknownCode() {
            assertNull(OrderStatusEnum.fromCode(999));
            assertNull(OrderStatusEnum.fromCode(-1));
            assertNull(OrderStatusEnum.fromCode(null));
        }
    }

    @Nested
    @DisplayName("canTransitionTo：状态迁移规则")
    class Transition {

        @ParameterizedTest
        @CsvSource({
            "PENDING_PAYMENT, PAID",          // 正常支付
            "PENDING_PAYMENT, CANCELLED",     // 待付款可取消
            "PAID, DELIVERED",                // 正常发货
            "PAID, REFUNDED",                 // 已付款可退款
            "DELIVERED, COMPLETED",           // 正常确认收货
            "DELIVERED, REFUNDED",            // 已发货可退款
            "COMPLETED, REFUNDED",            // 已完成可退款
        })
        @DisplayName("合法迁移应放行")
        void shouldAllowLegalTransitions(OrderStatusEnum from, OrderStatusEnum to) {
            assertTrue(from.canTransitionTo(to), from + " -> " + to + " 应当被允许");
        }

        /**
         * 这是本测试的核心：这些迁移都会破坏数据完整性。
         * 尤其是 CANCELLED -> PAID —— 它等价于"把一笔已取消的订单重新变成已付款"，凭空产生营收。
         */
        @ParameterizedTest
        @CsvSource({
            "CANCELLED, PAID",         // ★ 凭空造出交易，破坏对账
            "CANCELLED, DELIVERED",
            "CANCELLED, COMPLETED",
            "REFUNDED, PAID",          // ★ 已退款又变回已付款
            "REFUNDED, COMPLETED",
            "PAID, COMPLETED",         // 跳步：必须先发货
            "PENDING_PAYMENT, DELIVERED",  // 跳步：没付款就发货
            "PENDING_PAYMENT, COMPLETED",
            "DELIVERED, PAID",         // 逆向流转
            "COMPLETED, DELIVERED",
        })
        @DisplayName("非法迁移必须被拒绝（含跳步、逆向、终态复活）")
        void shouldRejectIllegalTransitions(OrderStatusEnum from, OrderStatusEnum to) {
            assertFalse(from.canTransitionTo(to), from + " -> " + to + " 必须被拒绝");
        }

        @ParameterizedTest
        @EnumSource(value = OrderStatusEnum.class, names = {"CANCELLED", "REFUNDED"})
        @DisplayName("终态不可再流转到任何状态")
        void terminalStatesHaveNoOutgoingTransition(OrderStatusEnum terminal) {
            for (OrderStatusEnum target : OrderStatusEnum.values()) {
                assertFalse(terminal.canTransitionTo(target),
                        terminal + " 是终态，不应能迁移到 " + target);
            }
        }

        @ParameterizedTest
        @EnumSource(OrderStatusEnum.class)
        @DisplayName("不允许迁移到自身（原地不动应由调用方走幂等分支处理）")
        void shouldNotAllowSelfTransition(OrderStatusEnum status) {
            assertFalse(status.canTransitionTo(status));
        }

        @Test
        @DisplayName("目标为 null 应返回 false，而不是抛 NPE")
        void shouldReturnFalseForNullTarget() {
            assertFalse(OrderStatusEnum.PENDING_PAYMENT.canTransitionTo(null));
        }
    }
}
