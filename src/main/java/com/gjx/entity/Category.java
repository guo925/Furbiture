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
