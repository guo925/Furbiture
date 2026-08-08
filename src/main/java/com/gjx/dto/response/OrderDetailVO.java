package com.gjx.dto.response;

import com.gjx.entity.Order;
import com.gjx.entity.OrderItem;
import com.gjx.entity.Address;
import com.gjx.enums.OrderStatusEnum;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 订单详情 VO — 供前端订单详情页使用
 * <p>
 * 聚合订单信息、商品明细、收货地址、物流状态。
 */
@Data
@Builder
public class OrderDetailVO {

    private Long id;
    private String orderNo;
    private Long userId;
    private Integer status;
    private String statusText;
    private BigDecimal totalAmount;
    private LocalDateTime createTime;
    private LocalDateTime payTime;
    private LocalDateTime deliveryTime;
    private LocalDateTime finishTime;
    private LocalDateTime cancelTime;

    /** 收货地址信息 */
    private AddressInfo address;

    /** 订单商品明细 */
    private List<OrderItemInfo> items;

    @Data
    @Builder
    public static class AddressInfo {
        private String receiver;
        private String phone;
        private String fullAddress;
    }

    @Data
    @Builder
    public static class OrderItemInfo {
        private Long id;
        private Long productId;
        private String productName;
        private String productImage;
        private BigDecimal price;
        private Integer quantity;
        private BigDecimal totalPrice;
    }

    /**
     * 从 Order 实体 + 关联数据构建 VO
     */
    public static OrderDetailVO from(Order order, List<OrderItem> items, Address address) {
        OrderStatusEnum statusEnum = OrderStatusEnum.fromCode(order.getStatus());

        return OrderDetailVO.builder()
                .id(order.getId())
                .orderNo(order.getOrderNo())
                .userId(order.getUserId())
                .status(order.getStatus())
                .statusText(statusEnum != null ? statusEnum.getDescription() : "未知")
                .totalAmount(order.getTotalAmount())
                .createTime(order.getCreateTime())
                .payTime(order.getPayTime())
                .deliveryTime(order.getDeliveryTime())
                .finishTime(order.getFinishTime())
                .cancelTime(order.getCancelTime())
                .address(address != null ? AddressInfo.builder()
                        .receiver(address.getName())
                        .phone(address.getPhone())
                        .fullAddress(address.getProvince() + address.getCity()
                                + address.getDistrict() + address.getDetailAddress())
                        .build() : null)
                .items(items != null ? items.stream().map(item -> OrderItemInfo.builder()
                        .id(item.getId())
                        .productId(item.getProductId())
                        .productName(item.getProductName())
                        .productImage(item.getProductImage())
                        .price(item.getPrice())
                        .quantity(item.getQuantity())
                        .totalPrice(item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                        .build()).collect(Collectors.toList()) : null)
                .build();
    }
}
