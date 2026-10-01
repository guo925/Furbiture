package com.gjx.controller.merchant;

import com.gjx.common.R;
import com.gjx.common.ResultCode;
import com.gjx.dto.request.ChangePasswordRequest;
import com.gjx.dto.request.MerchantInfoRequest;
import com.gjx.entity.User;
import com.gjx.service.IUserService;
import com.gjx.util.AuthenticationUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 商家店铺资料控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/merchant")
@Tag(name = "商家店铺资料", description = "商家信息和密码管理接口")
@RequiredArgsConstructor
public class MerchantProfileController {

    private final AuthenticationUtil authUtil;

    private final IUserService userService;

    private final PasswordEncoder passwordEncoder;

    @Operation(summary = "获取商家信息", description = "获取当前登录商家的基本信息")
    @GetMapping("/info")
    public R<Map<String, Object>> getMerchantInfo(HttpServletRequest request) {
        User user = getCurrentUser(request);
        Map<String, Object> info = new HashMap<>();
        info.put("id", user.getId());
        info.put("username", user.getUsername());
        info.put("phone", user.getPhone());
        info.put("email", user.getEmail());
        info.put("name", user.getUsername());
        return R.ok(info);
    }

    @Operation(summary = "更新商家信息", description = "更新商家的邮箱、手机号等信息（用户名不可修改）")
    @PutMapping("/info")
    public R<?> updateMerchantInfo(@Valid @RequestBody MerchantInfoRequest infoData, HttpServletRequest request) {
        User user = getCurrentUser(request);
        // 用户名禁止修改：username 是唯一登录标识，且 UserServiceImpl.findByUsername 用
        // getOne(...)（默认 throwEx=true）查库——一旦出现重名会直接抛 TooManyResultsException，
        // 登录、AuthenticationUtil 及所有依赖它的接口都会失效。若商家把自己改成 "admin"，
        // 管理员将永久无法登录。因此这里彻底不接收 username 变更（前端回填的同名值视为无操作）。
        String submittedUsername = infoData.getUsername() != null ? infoData.getUsername() : infoData.getName();
        if (submittedUsername != null && !submittedUsername.equals(user.getUsername())) {
            log.warn("[商家更新资料] 拒绝修改用户名 userId={}", user.getId());
            return R.error(ResultCode.PARAM_ERROR, "用户名不可修改");
        }
        if (infoData.getEmail() != null) user.setEmail(infoData.getEmail());
        if (infoData.getPhone() != null) user.setPhone(infoData.getPhone());
        userService.updateById(user);
        log.info("[商家更新资料] userId={}", user.getId());
        return R.ok("更新成功");
    }

    @Operation(summary = "修改密码", description = "修改当前商家的登录密码")
    @PostMapping("/info/password")
    public R<?> changePassword(@Valid @RequestBody ChangePasswordRequest passwordRequest, HttpServletRequest request) {
        User user = getCurrentUser(request);
        if (!passwordEncoder.matches(passwordRequest.getOldPassword(), user.getPassword())) {
            return R.error(ResultCode.FORBIDDEN, "原密码错误");
        }
        user.setPassword(passwordEncoder.encode(passwordRequest.getNewPassword()));
        userService.updateById(user);
        log.info("[商家修改密码] userId={}", user.getId());
        return R.ok("密码修改成功");
    }

    private User getCurrentUser(HttpServletRequest request) {
        return userService.findByUsername(authUtil.getUsernameFromRequest(request));
    }
}
