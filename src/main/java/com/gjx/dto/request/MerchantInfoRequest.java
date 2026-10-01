package com.gjx.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * 商家资料更新请求 DTO
 * <p>
 * 前端提交 {@code {username, email, phone}}；{@code name} 为兼容旧客户端的等价字段。
 * 手机号允许留空（正则含 {@code ^$} 分支），避免商家清空手机号时被误拦。
 */
@Data
public class MerchantInfoRequest {

    private String username;

    /** 兼容前端以 name 提交用户名的历史用法 */
    private String name;

    @Email(message = "邮箱格式不正确")
    private String email;

    @Pattern(regexp = "^$|^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;
}
