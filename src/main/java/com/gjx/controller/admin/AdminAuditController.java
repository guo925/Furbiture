package com.gjx.controller.admin;

import com.gjx.common.R;
import com.gjx.dto.request.AuditRejectRequest;
import com.gjx.service.IAdminAuditService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 管理员商家审核控制器
 * <p>
 * 只负责请求接收与参数校验，全部数据库访问与「审核 + 角色提升」的事务边界
 * 都收口到 {@link IAdminAuditService}（分层红线 §七-6：Controller 不得直接依赖
 * Mapper / JdbcTemplate，事务边界在 Service 层）。
 */
@RestController
@RequestMapping("/api/admin/audit")
@Tag(name = "商家审核", description = "管理员审核商家入驻申请")
@RequiredArgsConstructor
public class AdminAuditController {

    private final IAdminAuditService adminAuditService;

    @Operation(summary = "获取审核列表")
    @GetMapping("/applications")
    public R<?> listApplications(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) Integer status) {
        return R.ok(adminAuditService.listApplications(page, size, status));
    }

    @Operation(summary = "通过审核")
    @PutMapping("/applications/{id}/approve")
    public R<?> approve(@PathVariable Long id) {
        adminAuditService.approve(id);
        return R.ok("审核通过");
    }

    @Operation(summary = "拒绝审核")
    @PutMapping("/applications/{id}/reject")
    public R<?> reject(@PathVariable Long id, @Valid @RequestBody AuditRejectRequest rejectRequest) {
        adminAuditService.reject(id, rejectRequest.getReason());
        return R.ok("已拒绝");
    }
}
