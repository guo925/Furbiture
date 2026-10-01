package com.gjx.service.Impl;

import com.gjx.common.BusinessException;
import com.gjx.common.ResultCode;
import com.gjx.entity.User;
import com.gjx.enums.NotificationTypeEnum;
import com.gjx.enums.UserRoleEnum;
import com.gjx.mapper.UserMapper;
import com.gjx.service.IAdminAuditService;
import com.gjx.service.INotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 管理员商家审核服务实现。
 *
 * <p><b>为什么这里继续用 {@code JdbcTemplate} 而不是新建实体：</b>
 * {@code merchant_audit} 表未接入 ORM（见 {@code docs/AI-CONTEXT.md} §六-10），
 * 且本模块的读用法是「动态条件的 {@code SELECT *} 分页 + 直接以 {@code Map} 返回」，
 * 并不适合套用 MyBatis-Plus 的实体 CRUD；新增实体 + Mapper 只会带来
 * 「实体字段与表结构漂移」的新风险（可用 {@code scripts/check-entity-schema.mjs} 校验），
 * 收益却为零。分层红线针对的是「事务边界与数据访问必须在 Service 层」，
 * 在 Service 里使用 {@code JdbcTemplate} 完全合规（ArchUnit 规则也只约束 {@code ..controller..} 包）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminAuditServiceImpl implements IAdminAuditService {

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

    /**
     * 默认拒绝原因
     */
    private static final String DEFAULT_REJECT_REASON = "不符合入驻条件";

    private final JdbcTemplate jdbcTemplate;
    private final UserMapper userMapper;
    /** 审核结果需要回告申请人，否则用户提交后无从得知结果，只能反复进页面查看 */
    private final INotificationService notificationService;

    @Override
    public Map<String, Object> listApplications(Integer page, Integer size, Integer status) {
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
        return Map.of("records", list, "total", total == null ? 0L : total);
    }

    /**
     * 通过审核。
     *
     * <p><b>为什么必须在一个事务里：</b>本方法包含两次写操作——① 更新 {@code merchant_audit.status}
     * 为「已通过」，② 把申请人 {@code user.role} 提升为 MERCHANT。若两次写各自提交，一旦第②步失败
     * （数据库抖动、并发等），就会留下「审核记录显示已通过、但用户仍不是商家」的不一致状态，
     * 而管理员重试时第①步又已无实际意义。加上 {@code rollbackFor = Exception.class} 后，
     * 任一步抛异常都会整体回滚，保证「审核通过」与「角色提升」要么都发生、要么都不发生。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void approve(Long id) {
        List<Map<String, Object>> audits =
                jdbcTemplate.queryForList("SELECT * FROM merchant_audit WHERE id = ?", id);
        if (audits.isEmpty()) {
            throw new BusinessException(ResultCode.NOT_FOUND, "申请不存在");
        }
        Map<String, Object> audit = audits.get(0);

        // ① 更新审核状态
        jdbcTemplate.update("UPDATE merchant_audit SET status = ?, audit_time = ? WHERE id = ?",
                AUDIT_STATUS_APPROVED, LocalDateTime.now(), id);

        // ② 提升用户角色为商家
        // JDBC 驱动对 BIGINT 可能返回 Integer/Long，统一按 Number 取值，避免类型转换异常
        Object userIdValue = audit.get("user_id");
        if (userIdValue instanceof Number userId) {
            long applicantId = userId.longValue();
            User user = userMapper.selectById(applicantId);
            if (user == null) {
                // 申请人已被删除：不能提升角色，也不该给一个不存在的用户写通知
                log.warn("[商家审核通过] 申请人不存在，跳过角色提升与通知 auditId={}, userId={}", id, applicantId);
            } else {
                if (!UserRoleEnum.MERCHANT.getCode().equals(user.getRole())) {
                    user.setRole(UserRoleEnum.MERCHANT.getCode());
                    userMapper.updateById(user);
                }
                // ③ 回告申请人。与①②同处一个事务：任一步失败则整体回滚，
                //    不会出现"通知说已通过、角色却没升上去"
                notificationService.push(applicantId, NotificationTypeEnum.MERCHANT_AUDIT_APPROVED,
                        "您的商家入驻申请已通过，现在可以发布商品了", "/merchant");
            }
        }
        log.info("[商家审核通过] auditId={}, userId={}", id, userIdValue);
    }

    /**
     * 拒绝审核。
     *
     * <p><b>为什么要加事务：</b>本方法现在有两次写——① 更新审核状态为已拒绝，
     * ② 写入一条回告申请人的通知。只有第二次而不加事务时，一旦通知写入失败，
     * 申请记录已是「已拒绝」但用户永远收不到原因；加 {@code rollbackFor = Exception.class}
     * 后两者要么都成功要么都不发生。（此前只有一条 UPDATE，单语句自原子，故无需事务。）
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void reject(Long id, String reason) {
        String finalReason = (reason != null) ? reason : DEFAULT_REJECT_REASON;
        int updated = jdbcTemplate.update(
                "UPDATE merchant_audit SET status = ?, reject_reason = ?, audit_time = ? WHERE id = ?",
                AUDIT_STATUS_REJECTED, finalReason, LocalDateTime.now(), id);
        if (updated == 0) {
            // 保持原有对外行为（不抛异常），但留日志——否则"点了拒绝却什么也没发生"将无从排查
            log.warn("[商家审核拒绝] 申请不存在或已被删除，未做任何写入 auditId={}", id);
            return;
        }
        log.info("[商家审核拒绝] auditId={}", id);

        // 把拒绝原因带给申请人：只说"未通过"而不给原因，用户无从知道该改什么
        jdbcTemplate.queryForList("SELECT user_id FROM merchant_audit WHERE id = ?", id).stream()
                .map(row -> row.get("user_id"))
                .filter(value -> value instanceof Number number && number.longValue() > 0)
                .map(value -> ((Number) value).longValue())
                .findFirst()
                .ifPresent(applicantId -> notificationService.push(applicantId,
                        NotificationTypeEnum.MERCHANT_AUDIT_REJECTED,
                        "您的商家入驻申请未通过，原因：" + finalReason, "/profile"));
    }
}
