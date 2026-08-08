package com.gjx.controller.admin;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.gjx.common.R;
import com.gjx.entity.User;
import com.gjx.enums.UserRoleEnum;
import com.gjx.service.IUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 管理员用户管理控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/admin/users")
@Tag(name = "管理员用户管理", description = "管理员用户管理相关接口")
public class AdminUserController {

    @Autowired
    private IUserService userService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Operation(summary = "获取用户列表", description = "获取所有用户列表，支持分页和用户名搜索")
    @GetMapping
    public R<Page<User>> list(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String username) {
        Page<User> userPage = userService.adminListUsers(page, size, username);
        userPage.getRecords().forEach(user -> user.setPassword(null));
        return R.ok(userPage);
    }

    @Operation(summary = "获取用户详情", description = "根据用户ID获取用户详细信息")
    @GetMapping("/{id}")
    public R<User> detail(@PathVariable Long id) {
        User user = userService.getById(id);
        if (user == null) {
            return R.error("用户不存在");
        }
        user.setPassword(null);
        return R.ok(user);
    }

    @Operation(summary = "创建用户", description = "创建新用户")
    @PostMapping
    public R<?> create(@RequestBody User user) {
        if (userService.findByUsername(user.getUsername()) != null) {
            return R.error("用户名已存在");
        }
        if (user.getRole() == null || user.getRole().isEmpty()) {
            user.setRole(UserRoleEnum.USER.getCode());
        }
        if (user.getPassword() != null && !user.getPassword().isEmpty()) {
            user.setPassword(passwordEncoder.encode(user.getPassword()));
        }
        userService.save(user);
        return R.ok("创建成功");
    }

    @Operation(summary = "更新用户", description = "更新用户信息")
    @PutMapping("/{id}")
    public R<?> update(@PathVariable Long id, @RequestBody User user) {
        User existingUser = userService.getById(id);
        if (existingUser == null) {
            return R.error("用户不存在");
        }

        if (!existingUser.getUsername().equals(user.getUsername())) {
            User checkUser = userService.findByUsername(user.getUsername());
            if (checkUser != null) {
                return R.error("用户名已存在");
            }
        }

        user.setId(id);

        if (user.getPassword() == null || user.getPassword().isEmpty()) {
            user.setPassword(existingUser.getPassword());
        } else {
            user.setPassword(passwordEncoder.encode(user.getPassword()));
        }

        userService.updateById(user);
        return R.ok("更新成功");
    }

    @Operation(summary = "删除用户", description = "删除用户")
    @DeleteMapping("/{id}")
    public R<?> delete(@PathVariable Long id) {
        userService.removeById(id);
        return R.ok("删除成功");
    }

    @Operation(summary = "重置用户密码", description = "管理员重置用户密码")
    @PostMapping("/{id}/reset-password")
    public R<?> resetPassword(@PathVariable Long id, @RequestBody Map<String, String> passwordData) {
        User user = userService.getById(id);
        if (user == null) {
            return R.error("用户不存在");
        }

        String password = passwordData.get("password");
        if (password == null || password.isEmpty()) {
            return R.error("密码不能为空");
        }

        user.setPassword(passwordEncoder.encode(password));
        userService.updateById(user);
        return R.ok("密码重置成功");
    }
}
