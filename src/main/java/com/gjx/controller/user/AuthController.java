package com.gjx.controller.user;

import com.gjx.common.R;
import com.gjx.common.ResultCode;
import com.gjx.dto.request.RegisterRequest;
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
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 认证管理控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/auth")
@Tag(name = "认证管理", description = "认证相关接口")
public class AuthController {

    /**
     * 统一登录失败文案
     * <p>
     * 刻意不区分"用户不存在"和"密码错误"：若分开提示，攻击者可以借此枚举出
     * 系统里存在哪些账号（用户名枚举），为后续撞库提供精准目标。
     */
    private static final String LOGIN_FAILED_MESSAGE = "用户名或密码错误";

    @Autowired
    private JwtTokenProvider tokenProvider;

    @Autowired
    private IUserService userService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private LoginAttemptService loginAttemptService;

    /**
     * 用于用户名不存在时执行一次等价的 BCrypt 校验
     * <p>
     * 如果不做这一步，"用户不存在"会立刻返回，而"密码错误"要等 BCrypt 计算完（约 100ms），
     * 攻击者仅凭响应时间就能判断账号是否存在——这被称为时序侧信道。
     */
    private String dummyPasswordHash;

    @PostConstruct
    public void initDummyHash() {
        dummyPasswordHash = passwordEncoder.encode("timing-equalization-placeholder");
    }

    /**
     * 用户登录
     * @param loginRequest 登录请求
     * @return JWT令牌和用户信息
     */
    @Operation(summary = "用户登录", description = "用户登录并获取JWT令牌")
    @PostMapping("/login")
    public R<Map<String, Object>> login(@Valid @RequestBody LoginRequest loginRequest) {
        String username = loginRequest.getUsername();

        // 1. 先判断是否处于锁定期，被锁的账号直接拒绝，不再消耗 BCrypt 计算
        if (loginAttemptService.isBlocked(username)) {
            log.warn("[登录拒绝] 账号已锁定 username={}", username);
            return R.error(ResultCode.FORBIDDEN, "失败次数过多，账号已锁定，请15分钟后重试");
        }

        // 2. 校验用户与密码：用户不存在时也走一次 BCrypt，保证两条分支耗时一致
        User user = userService.findByUsername(username);
        boolean passwordMatched = passwordEncoder.matches(
                loginRequest.getPassword(),
                user != null ? user.getPassword() : dummyPasswordHash);

        if (user == null || !passwordMatched) {
            loginAttemptService.recordFailure(username);
            log.warn("[登录失败] username={}", username);
            return R.error(ResultCode.UNAUTHORIZED, LOGIN_FAILED_MESSAGE);
        }

        // 3. 登录成功，清空失败计数
        loginAttemptService.clear(username);
        String token = tokenProvider.generateToken(user.getUsername(), user.getId());
        log.info("[登录成功] username={}, userId={}", user.getUsername(), user.getId());

        user.setPassword(null);

        Map<String, Object> result = new HashMap<>();
        result.put("token", token);
        result.put("user", user);

        return R.ok(result);
    }

    /**
     * 登录请求DTO
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

    /**
     * 用户注册
     * @param registerRequest 注册请求（含格式校验）
     * @return 注册结果
     */
    @Operation(summary = "用户注册", description = "用户注册")
    @PostMapping("/register")
    public R<?> register(@Valid @RequestBody RegisterRequest registerRequest) {
        // 检查用户名是否已存在
        if (userService.findByUsername(registerRequest.getUsername()) != null) {
            return R.error(ResultCode.PARAM_ERROR, "用户名已存在");
        }

        // 显式构造实体并逐字段赋值，避免把请求体直接当实体使用——
        // 否则客户端可以自行传入 role 等字段实现提权（越权赋值漏洞）
        User user = new User();
        user.setUsername(registerRequest.getUsername());
        user.setPassword(passwordEncoder.encode(registerRequest.getPassword()));
        user.setPhone(registerRequest.getPhone());
        user.setEmail(registerRequest.getEmail());
        user.setRole(UserRoleEnum.USER.getCode());

        userService.save(user);
        log.info("[用户注册] username={}", user.getUsername());

        return R.ok("注册成功");
    }

    /**
     * 用户登出
     * @return 操作结果
     */
    @Operation(summary = "用户登出", description = "用户登出")
    @PostMapping("/logout")
    public R<?> logout() {
        // 清除认证上下文
        SecurityContextHolder.clearContext();
        return R.ok("登出成功");
    }
}
