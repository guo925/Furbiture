package com.gjx.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 分类实体类
 */
@Data
@TableName("category")
public class Category {
    /**
     * 分类ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /**
     * 分类名称
     */
    private String name;
    
    /**
     * 父分类ID
     */
    private Long parentId;
    
    /**
     * 分类级别
     */
    private Integer level;
    
    /**
     * 排序序号
     */
    private Integer sortOrder;
    
    /**
     * 分类图标/图片路径
     */
    private String icon;
    
    /**
     * 状态（0-禁用，1-启用）
     */
    private Integer status;
    
    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     * 逻辑删除标志：0-未删除，1-已删除。
     *
     * <p><b>为什么必须显式写 {@code @TableLogic}：</b>
     * {@code application.yml} 配了 {@code logic-delete-field: deleted}，但实体若没有同名字段，
     * 全局配置会<b>静默失效</b>，{@code removeById} 会退化成物理 {@code DELETE}（无报错、无日志）。
     * 显式标注后，删除分类只是置 {@code deleted = 1}，分类行保留。
     *
     * <p>{@code @TableField(select = false)} 同 {@code User.deleted}：让该列不进入查询结果，
     * 同时不影响逻辑删除所需的 {@code AND deleted = 0} 过滤条件。
     *
     * <p><b>注意：此处不处理子分类级联。</b>
     * 软删除一个仍有子分类的父分类后，子分类行不会被一并置为已删除，
     * 它们会因自身的 {@code deleted = 0} 仍被查出，此时其 {@code parentId} 已指向一个查不到的父级
     * → 分类树构建时这些子分类会「无处挂载」而丢失。这与原物理删除的表现相同
     * （物理删除同样不做级联），属既有行为，未在本次修复范围内改变。
     */
    @TableLogic
    @TableField(select = false)
    private Integer deleted;

    /**
     * 非数据库字段，用于树形结构展示
     */
    @TableField(exist = false)
    private List<Category> children;
    
    /**
     * 非数据库字段，父分类名称
     */
    @TableField(exist = false)
    private String parentName;
}
