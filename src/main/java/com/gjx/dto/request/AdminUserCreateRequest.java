package com.gjx.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 管理员创建用户请求 DTO
 * <p>
 * 原先直接以 {@code User} 实体接参，客户端可注入 {@code id} / {@code createTime}。
 * 密码由服务端加密后写入，DTO 中只承载明文入参。
 */
@Data
public class AdminUserCreateRequest {

    @NotBlank(message = "用户名不能为空")
    @Size(min = 3, max = 20, message = "用户名长度3-20位")
    private String username;

    @Size(min = 6, max = 20, message = "密码长度6-20位")
    private String password;

    @Pattern(regexp = "^$|^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;

    @Email(message = "邮箱格式不正确")
    private String email;

    /** 角色：USER / MERCHANT / ADMIN，为空时默认 USER */
    private String role;

    private String avatar;
}
