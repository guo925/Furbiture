package com.gjx.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * 用户更新个人信息请求 DTO
 * <p>
 * 仅允许用户更新非敏感字段（邮箱、手机号、头像），
 * 防止用户通过请求体篡改 role、password 等敏感字段实现越权。
 */
@Data
public class UpdateUserRequest {

    @Email(message = "邮箱格式不正确")
    private String email;

    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;

    private String avatar;
}
