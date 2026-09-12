package com.gjx.controller.admin;

import com.gjx.common.R;
import com.gjx.common.ResultCode;
import com.gjx.entity.User;
import com.gjx.enums.UserRoleEnum;
import com.gjx.security.JwtTokenProvider;
import com.gjx.security.LoginAttemptService;
import com.gjx.service.IUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.PostConstruct;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

/**
 * 管理员认证控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/admin/auth")
@Tag(name = "管理员认证", description = "管理员认证相关接口")
public class AdminAuthController {

    /**
     * 统一登录失败文案，避免管理员账号被枚举
     */
    private static final String LOGIN_FAILED_MESSAGE = "用户名或密码错误";

    /**
     * 锁定提示文案
     */
    private static final String ACCOUNT_LOCKED_MESSAGE = "失败次数过多，账号已锁定，请15分钟后重试";

    @Autowired
    private JwtTokenProvider tokenProvider;

    @Autowired
    private IUserService userService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private LoginAttemptService loginAttemptService;

    /**
     * 用户名不存在时执行等价的 BCrypt 计算，消除响应时间差异（时序侧信道）
     */
    private String dummyPasswordHash;

    @PostConstruct
    public void initDummyHash() {
        dummyPasswordHash = passwordEncoder.encode("timing-equalization-placeholder");
    }

    /**
     * 管理员登录
     * <p>
     * 入参由 {@code @RequestParam} 改为请求体：原先用户名密码会出现在 URL query 中，
     * 从而被 Nginx access_log、浏览器历史、代理服务等记录下来，等同于明文泄露管理员口令。
     *
     * @param loginRequest 登录请求
     * @return JWT 令牌
     */
    @Operation(summary = "管理员登录", description = "管理员登录并获取JWT令牌")
    @PostMapping("/login")
    public R<String> login(@Valid @RequestBody LoginRequest loginRequest) {
        String username = loginRequest.getUsername();

        if (loginAttemptService.isBlocked(username)) {
            log.warn("[管理员登录拒绝] 账号已锁定 username={}", username);
            return R.error(ResultCode.FORBIDDEN, ACCOUNT_LOCKED_MESSAGE);
        }

        User user = userService.findByUsername(username);
        boolean passwordMatched = passwordEncoder.matches(
                loginRequest.getPassword(),
                user != null ? user.getPassword() : dummyPasswordHash);

        if (user == null || !passwordMatched) {
            loginAttemptService.recordFailure(username);
            log.warn("[管理员登录失败] username={}", username);
            return R.error(ResultCode.UNAUTHORIZED, LOGIN_FAILED_MESSAGE);
        }

        // 验证是否为管理员（使用枚举而非硬编码字符串，避免与角色定义脱节）
        if (!UserRoleEnum.ADMIN.getCode().equals(user.getRole())) {
            log.warn("[管理员登录拒绝] 非管理员账号 username={}, role={}", username, user.getRole());
            return R.error(ResultCode.FORBIDDEN, "权限不足，只有管理员可以登录");
        }

        loginAttemptService.clear(username);
        String token = tokenProvider.generateToken(user.getUsername(), user.getId());
        log.info("[管理员登录成功] username={}, userId={}", user.getUsername(), user.getId());

        return R.ok(token);
    }

    /**
     * 管理员登出
     * @return 操作结果
     */
    @Operation(summary = "管理员登出", description = "管理员登出")
    @PostMapping("/logout")
    public R<?> logout() {
        return R.ok("登出成功");
    }

    /**
     * 管理员登录请求DTO
     */
    public static class LoginRequest {
        @NotBlank(message = "用户名不能为空")
        private String username;

        @NotBlank(message = "密码不能为空")
        private String password;

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }
    }
}
