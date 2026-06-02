package com.gjx.controller.admin;

import com.gjx.common.R;
import com.gjx.common.ResultCode;
import com.gjx.entity.User;
import com.gjx.security.JwtTokenProvider;
import com.gjx.service.IUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/auth")
@Tag(name = "管理员认证", description = "管理员认证相关接口")
public class AdminAuthController {

    @Autowired
    private JwtTokenProvider tokenProvider;

    @Autowired
    private IUserService userService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Operation(summary = "管理员登录", description = "管理员登录并获取JWT令牌")
    @PostMapping("/login")
    public R<String> login(@RequestParam String username, @RequestParam String password) {
        try {
            // 获取用户信息
            User user = userService.findByUsername(username);
            if (user == null) {
                return R.error(ResultCode.UNAUTHORIZED, "用户不存在");
            }

            // 验证密码
            if (!passwordEncoder.matches(password, user.getPassword())) {
                return R.error(ResultCode.UNAUTHORIZED, "密码错误");
            }

            // 验证是否为管理员
            if (!"ADMIN".equals(user.getRole())) {
                return R.error(ResultCode.FORBIDDEN, "权限不足，只有管理员可以登录");
            }

            // 生成JWT令牌
            String token = tokenProvider.generateToken(user.getUsername(), user.getId());

            return R.ok(token);
        } catch (Exception e) {
            return R.error(ResultCode.UNAUTHORIZED, "登录失败: " + e.getMessage());
        }
    }

    @Operation(summary = "管理员登出", description = "管理员登出")
    @PostMapping("/logout")
    public R<?> logout() {
        return R.ok("登出成功");
    }
}
