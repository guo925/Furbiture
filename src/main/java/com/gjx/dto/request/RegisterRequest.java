package com.gjx.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 注册请求 DTO
 */
@Data
public class RegisterRequest {

    @NotBlank(message = "用户名不能为空")
    @Size(min = 3, max = 20, message = "用户名长度3-20位")
    private String username;

    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 20, message = "密码长度6-20位")
    private String password;

    /**
     * 手机号：允许为空（用户可不填），填写时必须是合法的大陆手机号。
     * 正则中的 {@code ^$} 分支就是为"不填"这一合法场景准备的——
     * 若只写手机号正则，前端传空字符串会被判定为格式错误，导致注册被误拦。
     */
    @Pattern(regexp = "^$|^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;

    private String email;
}
