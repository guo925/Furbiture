package com.gjx.service;

import java.util.Map;

/**
 * 管理员商家审核服务接口
 * <p>
 * 原先这些逻辑（直接用 {@code JdbcTemplate} 读写 {@code merchant_audit}）写在
 * {@code AdminAuditController} 里，既违反了「Controller 不得直接做数据库访问」的分层红线，
 * 又让「改审核状态 + 提升用户角色」这两步写操作失去了事务保护。收口到本 Service。
 */
public interface IAdminAuditService {

    /**
     * 分页查询商家入驻申请
     *
     * @param page   页码（从 1 开始，非法值兜底为 1）
     * @param size   每页条数（非法值兜底、并限制上限）
     * @param status 审核状态过滤，可为 null（不过滤）
     * @return 含 {@code records}（申请列表）与 {@code total}（总数）的结果
     */
    Map<String, Object> listApplications(Integer page, Integer size, Integer status);

    /**
     * 通过审核：把申请置为「已通过」，并把申请人角色提升为商家。
     * <p>两步写操作必须在同一事务内，避免「审核已通过但角色未提升」的中间态。
     *
     * @param id 申请ID
     */
    void approve(Long id);

    /**
     * 拒绝审核：把申请置为「已拒绝」并记录拒绝原因。
     *
     * @param id     申请ID
     * @param reason 拒绝原因，为空时使用默认文案
     */
    void reject(Long id, String reason);
}
