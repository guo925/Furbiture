package com.gjx.util;

import com.gjx.security.JwtTokenProvider;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.function.Function;

/**
 * 当前登录用户的取用工具（Spring Bean）。
 *
 * <p><b>为什么是 Bean 而不是静态工具类：</b>
 * 本类原先写成「静态方法 + {@code private static JwtTokenProvider} + {@code @Autowired} 构造器把
 * provider 赋给静态字段」。那是个反模式，具体坏在三处：
 * <ol>
 *   <li><b>不可测试</b>——静态字段只能靠 Spring 容器装配，单元测试里想换一个假的 provider
 *       要么反射改静态字段、要么起整个容器，没有第三条路。</li>
 *   <li><b>跨容器污染</b>——静态字段属于 JVM 而非某个 ApplicationContext。同一 JVM 里存在
 *       两个上下文（比如测试分片、@DirtiesContext）时，后启动的会覆盖先启动的，
 *       先启动的那个上下文后续调用会拿到**另一个上下文的** provider。</li>
 *   <li><b>构造器起不到注入的作用</b>——Spring 为 {@code @Component} 建实例，
 *       而构造器真正做的只是"把参数搬到静态字段上"，实例本身被丢弃。注入点的存在
 *       只是为了触发 Spring 调用构造器，语义上是一种"骗过容器"的写法。</li>
 * </ol>
 * 改成实例方法后，依赖经构造器进入 {@code final} 字段，调用方注入本类即可——
 * 与项目里其他所有类保持同一种依赖风格（构造器注入，见 {@code .claude/CLAUDE.md} 分层约定）。
 *
 * <p><b>取值优先级：</b>先读 request attribute（{@code JwtAuthenticationFilter} 解析 JWT 后写入，
 * 见 {@link com.gjx.security.JwtAuthenticationFilter}），命中即返回，省掉一次 JWT 解析；
 * 未命中再降级为自行解析 {@code Authorization} 头，兼容未经该 Filter 的调用路径。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AuthenticationUtil {

    private final JwtTokenProvider tokenProvider;

    /**
     * 从请求中获取当前用户ID。
     *
     * @return 未认证时返回 {@code null}（调用方负责判定，不在此处抛异常，
     *         以便认证过滤器放行的公开接口也能复用本方法而不被误伤）
     */
    public Long getUserIdFromRequest(HttpServletRequest request) {
        Object userIdAttr = request.getAttribute("userId");
        if (userIdAttr instanceof Long userId) {
            return userId;
        }
        return parseBearerTokenQuietly(request, tokenProvider::getUserIdFromToken);
    }

    /**
     * 从请求中获取当前用户名。
     */
    public String getUsernameFromRequest(HttpServletRequest request) {
        Object usernameAttr = request.getAttribute("username");
        if (usernameAttr instanceof String username) {
            return username;
        }
        return parseBearerTokenQuietly(request, tokenProvider::getUsernameFromToken);
    }

    /**
     * 降级解析 {@code Authorization} 头中的 token，**解析失败一律按未认证处理**。
     *
     * <p>异常必须在这里吞掉，否则类注释承诺的「不在此处抛异常」就是空话：畸形或已过期的
     * token 会让 jjwt 抛 {@code DeserializationException} / {@code ExpiredJwtException}。
     * 受保护接口上这些请求在鉴权阶段就被挡成 403、到不了 Controller；但
     * **放行清单里的公开接口**（如 {@code GET /api/users/current}，前端用它探测会话）
     * 会一路走到 Controller 再调用本方法——不吞异常就是 500，而正确行为是
     * 「等同未登录」。同一个接口「不带头 200、带过期头 500」显然是缺陷。
     *
     * @param request HTTP 请求
     * @param parser  由裸 token 取值的解析函数
     * @return 解析成功返回对应值；无 token 或解析失败返回 {@code null}
     */
    private <T> T parseBearerTokenQuietly(HttpServletRequest request, Function<String, T> parser) {
        String token = resolveBearerToken(request);
        if (token == null) {
            return null;
        }
        try {
            return parser.apply(token);
        } catch (JwtException | IllegalArgumentException e) {
            // 只记「解析失败」这件事，不记 token 内容（红线：日志不得打印凭据）
            log.debug("[认证] Authorization 头中的 token 无法解析，按未认证处理：{}",
                    e.getClass().getSimpleName());
            return null;
        }
    }

    /**
     * 从 {@code Authorization: Bearer <token>} 头中取出裸 token。
     *
     * <p>提取出来是为了让两个取值方法共用同一段解析逻辑——原先它是复制了两遍的。
     */
    private String resolveBearerToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}
