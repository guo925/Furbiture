package com.gjx.dto.request;

import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 订单退款申请请求 DTO
 * <p>
 * 请求体可选（{@code required = false}），未携带时退款原因按 null 处理。
 */
@Data
public class RefundRequest {

    @Size(max = 200, message = "退款原因不能超过200个字符")
    private String reason;
}
