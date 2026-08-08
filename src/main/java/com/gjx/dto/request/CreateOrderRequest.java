package com.gjx.dto.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * 创建订单请求 DTO
 */
@Data
public class CreateOrderRequest {

    @NotNull(message = "收货地址不能为空")
    private Long addressId;

    @NotEmpty(message = "请选择商品")
    private List<Long> cartItemIds;
}
