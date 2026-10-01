package com.gjx.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 订单状态变更请求 DTO
 * <p>
 * 管理员与商家端的「更新订单状态」接口共用；原先管理员用内部类、商家用 Map
 * 接参且都缺少非空校验。
 */
@Data
public class OrderStatusRequest {

    @NotNull(message = "状态不能为空")
    private Integer status;
}
