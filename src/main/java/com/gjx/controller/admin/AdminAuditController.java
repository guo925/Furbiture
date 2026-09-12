package com.gjx.controller.admin;

import com.gjx.common.R;
import com.gjx.common.ResultCode;
import com.gjx.entity.User;
import com.gjx.enums.UserRoleEnum;
import com.gjx.mapper.UserMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 管理员商家审核控制器
 */
@RestController
@RequestMapping("/api/admin/audit")
@Tag(name = "商家审核", description = "管理员审核商家入驻申请")
public class AdminAuditController {

    /**
     * 分页参数上限，避免一次性拉取全表
     */
    private static final int MAX_PAGE_SIZE = 100;

    /**
     * 审核状态：1 已通过
     */
    private static final int AUDIT_STATUS_APPROVED = 1;

    /**
     * 审核状态：2 已拒绝
     */
    private static final int AUDIT_STATUS_REJECTED = 2;

    @Autowired private JdbcTemplate jdbcTemplate;
    @Autowired private UserMapper userMapper;

    @Operation(summary = "获取审核列表")
    @GetMapping("/applications")
    public R<?> listApplications(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) Integer status) {
        // 分页参数兜底，防止负数或超大值导致 SQL 语义异常
        int safePage = (page == null || page < 1) ? 1 : page;
        int safeSize = (size == null || size < 1) ? 10 : Math.min(size, MAX_PAGE_SIZE);

        // 全部条件使用占位符绑定，杜绝字符串拼接带来的 SQL 注入
        StringBuilder sql = new StringBuilder("SELECT * FROM merchant_audit WHERE 1=1");
        StringBuilder countSql = new StringBuilder("SELECT COUNT(*) FROM merchant_audit WHERE 1=1");
        List<Object> params = new ArrayList<>();
        if (status != null) {
            sql.append(" AND status = ?");
            countSql.append(" AND status = ?");
            params.add(status);
        }
        sql.append(" ORDER BY create_time DESC LIMIT ?, ?");

        List<Object> queryParams = new ArrayList<>(params);
        queryParams.add((safePage - 1) * safeSize);
        queryParams.add(safeSize);

        List<Map<String, Object>> list = jdbcTemplate.queryForList(sql.toString(), queryParams.toArray());
        Long total = jdbcTemplate.queryForObject(countSql.toString(), Long.class, params.toArray());
        return R.ok(Map.of("records", list, "total", total == null ? 0L : total));
    }

    @Operation(summary = "通过审核")
    @PutMapping("/applications/{id}/approve")
    @Transactional(rollbackFor = Exception.class)
    public R<?> approve(@PathVariable Long id) {
        List<Map<String, Object>> audits =
                jdbcTemplate.queryForList("SELECT * FROM merchant_audit WHERE id = ?", id);
        if (audits.isEmpty()) {
            return R.error(ResultCode.NOT_FOUND, "申请不存在");
        }
        Map<String, Object> audit = audits.get(0);

        // 更新审核状态
        jdbcTemplate.update("UPDATE merchant_audit SET status = ?, audit_time = ? WHERE id = ?",
                AUDIT_STATUS_APPROVED, LocalDateTime.now(), id);

        // 提升用户角色为商家。两次写操作同处一个事务，避免"审核已通过但角色未提升"的中间态。
        // JDBC 驱动对 BIGINT 可能返回 Integer/Long，统一按 Number 取值，避免类型转换异常
        Object userIdValue = audit.get("user_id");
        if (userIdValue instanceof Number userId) {
            User user = userMapper.selectById(userId.longValue());
            if (user != null && !UserRoleEnum.MERCHANT.getCode().equals(user.getRole())) {
                user.setRole(UserRoleEnum.MERCHANT.getCode());
                userMapper.updateById(user);
            }
        }

        return R.ok("审核通过");
    }

    @Operation(summary = "拒绝审核")
    @PutMapping("/applications/{id}/reject")
    public R<?> reject(@PathVariable Long id, @RequestBody Map<String, String> body) {
        String reason = body.getOrDefault("reason", "不符合入驻条件");
        jdbcTemplate.update("UPDATE merchant_audit SET status = ?, reject_reason = ?, audit_time = ? WHERE id = ?",
                AUDIT_STATUS_REJECTED, reason, LocalDateTime.now(), id);
        return R.ok("已拒绝");
    }
}
