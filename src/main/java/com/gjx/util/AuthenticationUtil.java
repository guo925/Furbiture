package com.gjx.util;

import com.gjx.security.JwtTokenProvider;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 认证工具类
 * <p>
 * 优先从 request attribute 读取用户信息（由 JwtAuthenticationFilter 设置），
 * 避免重复解析 JWT Token，减少计算开销。
 */
@Component
public class AuthenticationUtil {

    private static JwtTokenProvider tokenProvider;

    @Autowired
    public AuthenticationUtil(JwtTokenProvider provider) {
        AuthenticationUtil.tokenProvider = provider;
    }

    /**
     * 从请求中获取当前用户ID
     * <p>
     * 优先读取 request attribute（JwtAuthenticationFilter 已解析），
     * 降级到直接解析 JWT Token（兼容非 Filter 路径的调用）。
     */
    public static Long getUserIdFromRequest(HttpServletRequest request) {
        // 优先从 request attribute 读取（避免重复解析 JWT）
        Object userIdAttr = request.getAttribute("userId");
        if (userIdAttr instanceof Long) {
            return (Long) userIdAttr;
        }
        // 降级：自行解析 JWT
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            String token = bearerToken.substring(7);
            return tokenProvider.getUserIdFromToken(token);
        }
        return null;
    }

    /**
     * 从请求中获取当前用户名
     */
    public static String getUsernameFromRequest(HttpServletRequest request) {
        Object usernameAttr = request.getAttribute("username");
        if (usernameAttr instanceof String) {
            return (String) usernameAttr;
        }
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            String token = bearerToken.substring(7);
            return tokenProvider.getUsernameFromToken(token);
        }
        return null;
    }
}
