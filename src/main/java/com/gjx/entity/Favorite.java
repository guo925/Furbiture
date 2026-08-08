package com.gjx.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 商品收藏实体
 */
@Data
@TableName("favorite")
public class Favorite {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Long productId;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /** 非数据库字段：商品名称 */
    @TableField(exist = false)
    private String productName;

    /** 非数据库字段：商品主图 */
    @TableField(exist = false)
    private String productImage;

    /** 非数据库字段：商品价格 */
    @TableField(exist = false)
    private java.math.BigDecimal productPrice;
}
