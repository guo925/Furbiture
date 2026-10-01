package com.gjx.dto.request;

import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 商家入驻审核-拒绝请求 DTO
 */
@Data
public class AuditRejectRequest {

    @Size(max = 200, message = "拒绝原因不能超过200个字符")
    private String reason;
}
