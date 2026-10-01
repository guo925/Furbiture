package com.gjx.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 收货地址 新增/更新 请求 DTO
 * <p>
 * 原先直接以 {@code Address} 实体接收请求体，客户端可借机注入 {@code id} / {@code userId}
 * 越权改写他人地址。改为 DTO 后只暴露业务字段，{@code userId} 由服务端从令牌中覆写。
 */
@Data
public class AddressRequest {

    @NotBlank(message = "收货人不能为空")
    @Size(max = 20, message = "收货人姓名不能超过20个字符")
    private String name;

    @NotBlank(message = "手机号不能为空")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;

    @NotBlank(message = "省份不能为空")
    @Size(max = 50, message = "省份不能超过50个字符")
    private String province;

    @NotBlank(message = "城市不能为空")
    @Size(max = 50, message = "城市不能超过50个字符")
    private String city;

    @NotBlank(message = "区县不能为空")
    @Size(max = 50, message = "区县不能超过50个字符")
    private String district;

    @NotBlank(message = "详细地址不能为空")
    @Size(max = 120, message = "详细地址不能超过120个字符")
    private String detailAddress;

    /**
     * 是否默认地址：1 默认 0 非默认，不传按 0 处理
     */
    private Integer isDefault;
}
