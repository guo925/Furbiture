package com.gjx.controller.merchant;

import com.gjx.common.R;
import com.gjx.common.ResultCode;
import com.gjx.entity.User;
import com.gjx.service.IUserService;
import com.gjx.util.AuthenticationUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
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
public class MerchantProfileController {

    @Autowired
    private IUserService userService;

    @Autowired
    private PasswordEncoder passwordEncoder;

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

    @Operation(summary = "更新商家信息", description = "更新商家的邮箱、手机号等信息")
    @PutMapping("/info")
    public R<?> updateMerchantInfo(@RequestBody Map<String, String> infoData, HttpServletRequest request) {
        User user = getCurrentUser(request);
        if (infoData.containsKey("email")) user.setEmail(infoData.get("email"));
        if (infoData.containsKey("phone")) user.setPhone(infoData.get("phone"));
        if (infoData.containsKey("username")) user.setUsername(infoData.get("username"));
        else if (infoData.containsKey("name")) user.setUsername(infoData.get("name"));
        userService.updateById(user);
        log.info("[商家更新资料] userId={}", user.getId());
        return R.ok("更新成功");
    }

    @Operation(summary = "修改密码", description = "修改当前商家的登录密码")
    @PostMapping("/info/password")
    public R<?> changePassword(@RequestBody Map<String, String> passwordData, HttpServletRequest request) {
        User user = getCurrentUser(request);
        String oldPassword = passwordData.get("oldPassword");
        String newPassword = passwordData.get("newPassword");
        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            return R.error(ResultCode.FORBIDDEN, "原密码错误");
        }
        user.setPassword(passwordEncoder.encode(newPassword));
        userService.updateById(user);
        log.info("[商家修改密码] userId={}", user.getId());
        return R.ok("密码修改成功");
    }

    private User getCurrentUser(HttpServletRequest request) {
        return userService.findByUsername(AuthenticationUtil.getUsernameFromRequest(request));
    }
}
