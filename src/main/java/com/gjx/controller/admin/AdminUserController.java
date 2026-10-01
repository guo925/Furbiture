package com.gjx.controller.admin;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.gjx.common.R;
import com.gjx.common.ResultCode;
import com.gjx.dto.request.AdminUserCreateRequest;
import com.gjx.dto.request.AdminUserUpdateRequest;
import com.gjx.dto.request.ResetPasswordRequest;
import com.gjx.entity.User;
import com.gjx.enums.UserRoleEnum;
import com.gjx.service.IUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

/**
 * 管理员用户管理控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/admin/users")
@Tag(name = "管理员用户管理", description = "管理员用户管理相关接口")
@RequiredArgsConstructor
public class AdminUserController {

    private final IUserService userService;

    private final PasswordEncoder passwordEncoder;

    @Operation(summary = "获取用户列表", description = "获取所有用户列表，支持分页、用户名搜索与角色过滤")
    @GetMapping
    public R<Page<User>> list(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String role) {
        // 密码字段由 User.password 上的 @JsonProperty(WRITE_ONLY) 保证不会被序列化输出
        Page<User> userPage = userService.adminListUsers(page, size, username, role);
        return R.ok(userPage);
    }

    @Operation(summary = "获取用户详情", description = "根据用户ID获取用户详细信息")
    @GetMapping("/{id}")
    public R<User> detail(@PathVariable Long id) {
        User user = userService.getById(id);
        if (user == null) {
            return R.error(ResultCode.NOT_FOUND, "用户不存在");
        }
        return R.ok(user);
    }

    @Operation(summary = "创建用户", description = "创建新用户")
    @PostMapping
    public R<?> create(@Valid @RequestBody AdminUserCreateRequest request) {
        if (userService.findByUsername(request.getUsername()) != null) {
            return R.error(ResultCode.PARAM_ERROR, "用户名已存在");
        }

        // 显式构造实体并逐字段赋值：既避免客户端注入 id / createTime，
        // 也确保 role 只能取服务端认可的取值
        User user = new User();
        user.setUsername(request.getUsername());
        user.setPhone(request.getPhone());
        user.setEmail(request.getEmail());
        user.setAvatar(request.getAvatar());
        user.setRole(request.getRole() == null || request.getRole().isEmpty()
                ? UserRoleEnum.USER.getCode() : request.getRole());
        if (request.getPassword() != null && !request.getPassword().isEmpty()) {
            user.setPassword(passwordEncoder.encode(request.getPassword()));
        }
        userService.save(user);
        return R.ok("创建成功");
    }

    @Operation(summary = "更新用户", description = "更新用户信息")
    @PutMapping("/{id}")
    public R<?> update(@PathVariable Long id, @Valid @RequestBody AdminUserUpdateRequest request) {
        User existingUser = userService.getById(id);
        if (existingUser == null) {
            return R.error(ResultCode.NOT_FOUND, "用户不存在");
        }

        if (request.getUsername() != null && !existingUser.getUsername().equals(request.getUsername())) {
            if (userService.findByUsername(request.getUsername()) != null) {
                return R.error(ResultCode.PARAM_ERROR, "用户名已存在");
            }
        }

        User user = new User();
        user.setId(id);
        user.setUsername(request.getUsername());
        user.setRole(request.getRole());
        user.setPhone(request.getPhone());
        user.setEmail(request.getEmail());
        user.setAvatar(request.getAvatar());

        if (request.getPassword() == null || request.getPassword().isEmpty()) {
            // 未提交新密码则沿用原哈希（null 字段不参与部分更新，必须显式回填）
            user.setPassword(existingUser.getPassword());
        } else {
            user.setPassword(passwordEncoder.encode(request.getPassword()));
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
    public R<?> resetPassword(@PathVariable Long id, @Valid @RequestBody ResetPasswordRequest request) {
        User user = userService.getById(id);
        if (user == null) {
            return R.error(ResultCode.NOT_FOUND, "用户不存在");
        }

        user.setPassword(passwordEncoder.encode(request.getPassword()));
        userService.updateById(user);
        return R.ok("密码重置成功");
    }
}
