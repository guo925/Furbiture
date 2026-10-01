package com.gjx.util;

import com.gjx.security.JwtTokenProvider;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.io.DeserializationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * {@link AuthenticationUtil} 的「解析失败按未认证处理」契约测试。
 *
 * <p><b>为什么单独测这个：</b>本类 javadoc 承诺「未认证时返回 null，**不在此处抛异常**，
 * 以便认证过滤器放行的公开接口也能复用本方法而不被误伤」。这条承诺曾经是空话——
 * 畸形 token 会让 jjwt 抛 {@code DeserializationException}。
 *
 * <p>受保护接口上这个异常到不了 Controller（鉴权阶段就 403 了），所以问题一直不可见；
 * 但**放行清单里的 {@code GET /api/users/current}**（前端用它探测会话）会一路走到
 * Controller 再调本方法，于是「不带头 200、带过期头 500」。同一个接口两种状态码，
 * 显然是缺陷。
 *
 * <p>这里用**真实的** jjwt 异常类型而不是自造的 RuntimeException：继承链是
 * {@code DeserializationException → SerialException → io.jsonwebtoken.io.IOException → JwtException}，
 * 中间隔了两层，只 catch {@code ExpiredJwtException} 之类是拦不住的。
 */
@ExtendWith(MockitoExtension.class)
class AuthenticationUtilTest {

    private static final String BEARER = "Bearer ";

    @Mock
    private JwtTokenProvider tokenProvider;

    @Test
    @DisplayName("畸形 token → 返回 null，不抛异常（token 内容是非法 JSON 时的真实异常类型）")
    void malformedTokenFallsBackToAnonymous() {
        MockHttpServletRequest request = requestWithToken("bad.token.x");
        when(tokenProvider.getUserIdFromToken("bad.token.x"))
                .thenThrow(new DeserializationException("Unrecognized token 'm'"));

        assertThat(new AuthenticationUtil(tokenProvider).getUserIdFromRequest(request)).isNull();
    }

    @Test
    @DisplayName("过期 token 取用户名 → 同样返回 null，不抛异常")
    void expiredTokenFallsBackToAnonymous() {
        MockHttpServletRequest request = requestWithToken("expired.token");
        when(tokenProvider.getUsernameFromToken("expired.token"))
                .thenThrow(new ExpiredJwtException(null, null, "JWT expired"));

        assertThat(new AuthenticationUtil(tokenProvider).getUsernameFromRequest(request)).isNull();
    }

    @Test
    @DisplayName("结构不合法的 token（MalformedJwtException）→ 同样返回 null")
    void structurallyInvalidTokenFallsBackToAnonymous() {
        MockHttpServletRequest request = requestWithToken("not-a-jwt");
        when(tokenProvider.getUserIdFromToken("not-a-jwt"))
                .thenThrow(new MalformedJwtException("Malformed JWT"));

        assertThat(new AuthenticationUtil(tokenProvider).getUserIdFromRequest(request)).isNull();
    }

    @Test
    @DisplayName("未带头 → 返回 null，且根本不会去调用解析器")
    void absentHeaderSkipsParsing() {
        MockHttpServletRequest request = new MockHttpServletRequest();

        assertThat(new AuthenticationUtil(tokenProvider).getUserIdFromRequest(request)).isNull();
        verifyNoInteractions(tokenProvider);
    }

    @Test
    @DisplayName("过滤器已写入 request attribute → 直接用，省掉第二次 JWT 解析")
    void requestAttributeTakesPriorityOverParsing() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute("userId", 7L);

        assertThat(new AuthenticationUtil(tokenProvider).getUserIdFromRequest(request)).isEqualTo(7L);
        verifyNoInteractions(tokenProvider);
    }

    @Test
    @DisplayName("合法 token → 正常解析出用户")
    void validTokenResolves() {
        MockHttpServletRequest request = requestWithToken("good.token");
        when(tokenProvider.getUserIdFromToken("good.token")).thenReturn(42L);

        assertThat(new AuthenticationUtil(tokenProvider).getUserIdFromRequest(request)).isEqualTo(42L);
        verify(tokenProvider).getUserIdFromToken("good.token");
    }

    private static MockHttpServletRequest requestWithToken(String token) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", BEARER + token);
        return request;
    }
}
