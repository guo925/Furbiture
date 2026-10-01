package com.gjx.dto.response;

import com.gjx.entity.Order;
import com.gjx.entity.OrderItem;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 用户端订单列表项出参。
 *
 * <p><b>为什么需要它：</b>此前 {@code OrderController.list} 是在 Controller 里用
 * {@code HashMap} 手工 {@code put} 出 12 个字段（含嵌套的 {@code orderItems}）。
 * 那种写法有两个问题：一是字段名是字符串字面量，写错了编译期不报错、前端静默拿不到值
 * （本项目已多次出现"看起来实现了、实际没生效"）；二是结构组装属展示层职责，
 * 落在 Controller 里让它超出了「请求接收 + 参数校验」的分工，也违反「出参用 VO」的规范。
 *
 * <p><b>关于 {@code orderItems} 直接持有实体：</b>这是刻意的。{@link OrderItem} 只有
 * 商品快照字段（名称 / 图片 / 单价 / 数量），不含任何敏感信息；再包一层 1:1 的
 * {@code OrderItemVO} 只是搬运而没有收益。更重要的是<b>响应 JSON 形状必须与重构前逐字段一致</b>
 * （前端 {@code views/user/OrdersView.vue} 的 {@code normalizeOrder} 依赖 {@code orderItems} 这个键名与
 * 内部字段名），因此这里不复用、也不新增任何字段。
 *
 * <p>注意本 VO <b>不含</b> {@code statusText}：状态文案统一由前端
 * {@code constants/orderStatus.js} 提供，后端再给一份就会出现"同一订单两个词"。
 */
@Data
@Builder
public class OrderListItemVO {

    private Long id;
    private String orderNo;
    private Long userId;
    private Long addressId;
    private BigDecimal totalAmount;
    private Integer status;
    private LocalDateTime payTime;
    private LocalDateTime deliveryTime;
    private LocalDateTime finishTime;
    private LocalDateTime cancelTime;
    private LocalDateTime createTime;
    private List<OrderItem> orderItems;

    /**
     * 由订单实体与本单明细构建出参。
     *
     * @param order      订单实体
     * @param orderItems 该订单的明细；无明细时传空集合，不要传 {@code null}
     */
    public static OrderListItemVO from(Order order, List<OrderItem> orderItems) {
        return OrderListItemVO.builder()
                .id(order.getId())
                .orderNo(order.getOrderNo())
                .userId(order.getUserId())
                .addressId(order.getAddressId())
                .totalAmount(order.getTotalAmount())
                .status(order.getStatus())
                .payTime(order.getPayTime())
                .deliveryTime(order.getDeliveryTime())
                .finishTime(order.getFinishTime())
                .cancelTime(order.getCancelTime())
                .createTime(order.getCreateTime())
                .orderItems(orderItems)
                .build();
    }
}
