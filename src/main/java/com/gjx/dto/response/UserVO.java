package com.gjx.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户视图对象 — 不含密码等敏感字段
 */
@Data
@Builder
public class UserVO {

    private Long id;

    private String username;

    private String phone;

    private String email;

    private String avatar;

    private String role;

    private LocalDateTime createTime;

    public static UserVO from(com.gjx.entity.User user) {
        return UserVO.builder()
                .id(user.getId())
                .username(user.getUsername())
                .phone(user.getPhone())
                .email(user.getEmail())
                .avatar(user.getAvatar())
                .role(user.getRole())
                .createTime(user.getCreateTime())
                .build();
    }
}
