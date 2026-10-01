package com.gjx.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 管理员更新用户请求 DTO
 * <p>
 * 密码为可选字段：为空表示不改密码，非空则服务端重新加密写入。
 */
@Data
public class AdminUserUpdateRequest {

    @Size(min = 3, max = 20, message = "用户名长度3-20位")
    private String username;

    /** 留空表示不修改密码 */
    @Size(min = 6, max = 20, message = "密码长度6-20位")
    private String password;

    @Pattern(regexp = "^$|^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;

    @Email(message = "邮箱格式不正确")
    private String email;

    private String role;

    private String avatar;
}
