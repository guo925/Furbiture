package com.gjx.controller.user;

import com.gjx.common.R;
import com.gjx.common.ResultCode;
import com.gjx.entity.User;
import com.gjx.security.JwtTokenProvider;
import com.gjx.service.IUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 认证管理控制器
 */
@RestController
@RequestMapping("/api/auth")
@Tag(name = "认证管理", description = "认证相关接口")
public class AuthController {

    @Autowired
    private JwtTokenProvider tokenProvider;

    @Autowired
    private IUserService userService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    /**
     * 用户登录
     * @param loginRequest 登录请求
     * @return JWT令牌和用户信息
     */
    @Operation(summary = "用户登录", description = "用户登录并获取JWT令牌")
    @PostMapping("/login")
    public R<Map<String, Object>> login(@RequestBody LoginRequest loginRequest) {
        try {
            // 获取用户信息
            User user = userService.findByUsername(loginRequest.getUsername());
            if (user == null) {
                return R.error(ResultCode.UNAUTHORIZED, "用户不存在");
            }

            // 验证密码（直接比较明文）
            if (!loginRequest.getPassword().equals(user.getPassword())) {
                return R.error(ResultCode.UNAUTHORIZED, "密码错误");
            }

            // 生成JWT令牌，包含userId
            String token = tokenProvider.generateToken(user.getUsername(), user.getId());

            user.setPassword(null);

            // 构建返回结果
            Map<String, Object> result = new HashMap<>();
            result.put("token", token);
            result.put("user", user);

            return R.ok(result);
        } catch (Exception e) {
            e.printStackTrace();
            return R.error(ResultCode.UNAUTHORIZED, "登录失败: " + e.getMessage());
        }
    }

    /**
     * 登录请求DTO
     */
    public static class LoginRequest {
        private String username;
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
     * @param user 用户信息
     * @return 注册结果
     */
    @Operation(summary = "用户注册", description = "用户注册")
    @PostMapping("/register")
    public R<?> register(@RequestBody User user) {
        try {
            // 检查用户名是否已存在
            if (userService.findByUsername(user.getUsername()) != null) {
                return R.error(ResultCode.PARAM_ERROR, "用户名已存在");
            }

            // 设置默认角色
            user.setRole("USER");

            // 保存用户
            userService.save(user);

            return R.ok("注册成功");
        } catch (Exception e) {
            return R.error(ResultCode.ERROR, "注册失败");
        }
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