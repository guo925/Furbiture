package com.gjx.util;

import com.gjx.security.JwtTokenProvider;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class AuthenticationUtil {

    private static JwtTokenProvider tokenProvider;

    @Autowired
    public AuthenticationUtil(JwtTokenProvider provider) {
        AuthenticationUtil.tokenProvider = provider;
    }

    public static Long getUserIdFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            String token = bearerToken.substring(7);
            return tokenProvider.getUserIdFromToken(token);
        }
        return null;
    }

    public static String getUsernameFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            String token = bearerToken.substring(7);
            return tokenProvider.getUsernameFromToken(token);
        }
        return null;
    }
}
