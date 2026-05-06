package com.gjx.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 商品规格实体类
 */
@Data
@TableName("product_spec")
public class ProductSpec {
    /**
     * 规格ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /**
     * 商品ID
     */
    private Long productId;
    
    /**
     * 规格名称
     */
    private String specName;
    
    /**
     * 规格值
     */
    private String specValue;
}
