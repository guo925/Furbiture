package com.gjx.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户实体类
 */
@Data
@TableName("user")
public class User {
    /**
     * 用户ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /**
     * 用户名
     */
    private String username;
    
    /**
     * 密码（BCrypt 哈希）
     * <p>
     * {@code WRITE_ONLY} 表示只允许反序列化（写入）、永不序列化输出：
     * 从根上杜绝任何接口把密码哈希吐给前端，无需每个返回用户的地方再手工 {@code setPassword(null)}。
     */
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String password;
    
    /**
     * 手机号
     */
    private String phone;
    
    /**
     * 邮箱
     */
    private String email;
    
    /**
     * 头像
     */
    private String avatar;
    
    /**
     * 角色：USER, ADMIN
     */
    private String role;
    
    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    
    /**
     * 更新时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /**
     * 逻辑删除标志：0-未删除，1-已删除（默认取全局配置的 logic-not-delete-value / logic-delete-value）。
     *
     * <p><b>为什么必须显式写 {@code @TableLogic}（本项目最容易踩的坑）：</b>
     * {@code application.yml} 里已经配了 {@code logic-delete-field: deleted}，但 MyBatis-Plus
     * 只会在「实体类里存在同名字段」时才把该列当作逻辑删除列。全局配置<b>不会</b>替实体补字段——
     * 实体缺这个字段时配置<b>静默失效</b>：{@code removeById} 会退化成物理 {@code DELETE}，
     * 既不报错也没有任何日志。正因如此，此前 {@code AdminUserController.delete} 一直是真删除，
     * 删掉有订单的用户后历史订单就永久失去用户信息且不可恢复。显式标注后逻辑删除才真正生效。
     *
     * <p><b>为什么加 {@code @TableField(select = false)}：</b>
     * 该列是纯内部状态，不应出现在查询结果里（否则会随 Entity 直吐给前端污染 VO）。
     * {@code select = false} 只是把它从 SELECT 的列清单里去掉；逻辑删除所需的
     * {@code AND deleted = 0} 过滤条件由 {@code @TableLogic} 单独拼接，两者互不影响。
     *
     * <p><b>已知副作用（本项目接受，务必知悉）：</b>
     * {@code user.username} 上有 UNIQUE 约束，而软删除后该行仍留在表里，因此
     * <b>被删除的用户名无法再被注册</b>（同名注册会撞唯一键）。本项目接受该后果：
     * 软删除的首要目的是「保住历史订单的用户归属」，用户名复用并非业务需求；
     * 且把唯一键改成 {@code (username, deleted)} 也只能容纳「一条已删除 + 一条在用」，
     * 反复「注册→注销→再注册」仍会撞键，并非真正的修复
     * （真正的修复需要 generated column 或删除时给 username 改名，属后续独立工作）。
     */
    @TableLogic
    @TableField(select = false)
    private Integer deleted;
}
