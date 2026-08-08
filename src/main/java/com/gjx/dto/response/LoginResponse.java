package com.gjx.dto.response;

import lombok.Builder;
import lombok.Data;

/**
 * 登录响应 DTO
 */
@Data
@Builder
public class LoginResponse {

    private String token;
    private UserVO user;
}
