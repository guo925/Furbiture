package com.gjx.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 用户角色枚举
 * <p>
 * 用于替代代码中的魔法字符串 "USER"、"ADMIN"、"MERCHANT"，
 * 确保角色值的一致性，避免拼写错误。
 *
 * @author Furbiture Team
 * @since 2026-08-08
 */
@Getter
@AllArgsConstructor
public enum UserRoleEnum {

    /**
     * 普通用户：浏览商品、下单购物
     */
    USER("USER", "普通用户"),

    /**
     * 管理员：管理所有商品、订单、用户
     */
    ADMIN("ADMIN", "管理员"),

    /**
     * 商家：管理自有商品和订单
     */
    MERCHANT("MERCHANT", "商家");

    /**
     * 角色编码（对应数据库 role 字段值和 Spring Security authority）
     */
    private final String code;

    /**
     * 角色中文描述
     */
    private final String description;

    /**
     * 根据角色编码获取枚举实例
     *
     * @param code 角色编码
     * @return 对应的枚举实例，未匹配到时返回 null
     */
    public static UserRoleEnum fromCode(String code) {
        if (code == null || code.isEmpty()) {
            return null;
        }
        for (UserRoleEnum role : values()) {
            if (role.getCode().equalsIgnoreCase(code)) {
                return role;
            }
        }
        return null;
    }

    /**
     * 判断是否为管理员角色
     */
    public boolean isAdmin() {
        return this == ADMIN;
    }

    /**
     * 判断是否为商家角色
     */
    public boolean isMerchant() {
        return this == MERCHANT;
    }

    /**
     * 判断是否为普通用户角色
     */
    public boolean isUser() {
        return this == USER;
    }
}
