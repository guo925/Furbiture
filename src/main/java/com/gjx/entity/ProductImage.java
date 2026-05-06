package com.gjx.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 商品图片实体类
 */
@Data
@TableName("product_image")
public class ProductImage {
    /**
     * 图片ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /**
     * 商品ID
     */
    private Long productId;
    
    /**
     * 图片路径
     */
    private String imageUrl;
    
    /**
     * 排序序号
     */
    private Integer sortOrder;
}
