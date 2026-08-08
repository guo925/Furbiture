package com.gjx.controller.admin;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.gjx.common.R;
import com.gjx.common.ResultCode;
import com.gjx.entity.User;
import com.gjx.mapper.UserMapper;
import com.gjx.enums.UserRoleEnum;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 管理员商家审核控制器
 */
@RestController
@RequestMapping("/api/admin/audit")
@Tag(name = "商家审核", description = "管理员审核商家入驻申请")
public class AdminAuditController {

    @Autowired private JdbcTemplate jdbcTemplate;
    @Autowired private UserMapper userMapper;
    @Autowired private PasswordEncoder passwordEncoder;

    @Operation(summary = "获取审核列表")
    @GetMapping("/applications")
    public R<?> listApplications(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) Integer status) {
        StringBuilder sql = new StringBuilder("SELECT * FROM merchant_audit WHERE 1=1");
        if (status != null) sql.append(" AND status = ").append(status);
        sql.append(" ORDER BY create_time DESC LIMIT ").append((page - 1) * size).append(", ").append(size);
        String countSql = "SELECT COUNT(*) FROM merchant_audit" + (status != null ? " WHERE status = " + status : "");
        var list = jdbcTemplate.queryForList(sql.toString());
        long total = jdbcTemplate.queryForObject(countSql, Long.class);
        return R.ok(Map.of("records", list, "total", total));
    }

    @Operation(summary = "通过审核")
    @PutMapping("/applications/{id}/approve")
    public R<?> approve(@PathVariable Long id) {
        var audit = jdbcTemplate.queryForMap("SELECT * FROM merchant_audit WHERE id = ?", id);
        if (audit == null) return R.error(ResultCode.NOT_FOUND, "申请不存在");

        // 更新审核状态
        jdbcTemplate.update("UPDATE merchant_audit SET status = 1, audit_time = ? WHERE id = ?",
                LocalDateTime.now(), id);

        // 创建商家用户（如果不存在）
        Long userId = (Long) audit.get("user_id");
        User user = userMapper.selectById(userId);
        if (user != null && !UserRoleEnum.MERCHANT.getCode().equals(user.getRole())) {
            user.setRole(UserRoleEnum.MERCHANT.getCode());
            userMapper.updateById(user);
        }

        return R.ok("审核通过");
    }

    @Operation(summary = "拒绝审核")
    @PutMapping("/applications/{id}/reject")
    public R<?> reject(@PathVariable Long id, @RequestBody Map<String, String> body) {
        String reason = body.getOrDefault("reason", "不符合入驻条件");
        jdbcTemplate.update("UPDATE merchant_audit SET status = 2, reject_reason = ?, audit_time = ? WHERE id = ?",
                reason, LocalDateTime.now(), id);
        return R.ok("已拒绝");
    }
}
