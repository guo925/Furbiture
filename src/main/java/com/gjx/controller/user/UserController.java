package com.gjx.controller.user;

import com.gjx.common.R;
import com.gjx.common.ResultCode;
import com.gjx.dto.request.UpdateUserRequest;
import com.gjx.entity.User;
import com.gjx.service.IUserService;
import com.gjx.util.AuthenticationUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 用户管理控制器
 */
@RestController
@RequestMapping("/api/users")
@Tag(name = "用户管理", description = "用户相关接口")
public class UserController {

    @Autowired
    private IUserService userService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    /**
     * 获取当前用户信息
     * @param request HTTP请求
     * @return 用户信息
     */
    @Operation(summary = "获取当前用户信息", description = "获取当前登录用户的详细信息")
    @GetMapping("/current")
    public R<User> getCurrentUser(HttpServletRequest request) {
        Long userId = AuthenticationUtil.getUserIdFromRequest(request);
        User user = userService.getById(userId);
        user.setPassword(null);
        return R.ok(user);
    }

    /**
     * 更新用户信息
     * <p>
     * 仅允许更新邮箱、手机号、头像等非敏感字段，
     * 防止用户通过请求体篡改 role、password 实现越权。
     *
     * @param requestDto 用户更新请求（仅含 email, phone, avatar）
     * @param request    HTTP请求
     * @return 更新结果
     */
    @Operation(summary = "更新用户信息", description = "更新当前用户的基本信息（邮箱、手机号、头像）")
    @PutMapping
    public R<?> updateUser(@RequestBody UpdateUserRequest requestDto, HttpServletRequest request) {
        Long userId = AuthenticationUtil.getUserIdFromRequest(request);
        User user = userService.getById(userId);
        if (user == null) {
            return R.error(ResultCode.NOT_FOUND, "用户不存在");
        }
        // 仅更新允许的字段，防止越权修改 role/password 等敏感字段
        if (requestDto.getEmail() != null) {
            user.setEmail(requestDto.getEmail());
        }
        if (requestDto.getPhone() != null) {
            user.setPhone(requestDto.getPhone());
        }
        if (requestDto.getAvatar() != null) {
            user.setAvatar(requestDto.getAvatar());
        }
        userService.updateById(user);
        return R.ok("更新成功");
    }

    /**
     * 修改密码
     * @param passwordData 密码数据
     * @param request HTTP请求
     * @return 修改结果
     */
    @Operation(summary = "修改密码", description = "修改当前用户的密码")
    @PostMapping("/password")
    public R<?> changePassword(@RequestBody Map<String, String> passwordData, HttpServletRequest request) {
        Long userId = AuthenticationUtil.getUserIdFromRequest(request);
        User user = userService.getById(userId);
        
        if (user == null) {
            return R.error(ResultCode.NOT_FOUND, "用户不存在");
        }
        
        String oldPassword = passwordData.get("oldPassword");
        String newPassword = passwordData.get("newPassword");
        
        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            return R.error(ResultCode.FORBIDDEN, "原密码错误");
        }
        
        user.setPassword(passwordEncoder.encode(newPassword));
        userService.updateById(user);
        
        return R.ok("密码修改成功");
    }
}
