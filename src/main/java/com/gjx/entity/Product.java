package com.gjx.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("product")
public class Product {
    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;

    private Long categoryId;

    @TableField(exist = false)
    private String categoryName;

    private String brand;

    private String mainImage;

    private BigDecimal price;

    private Integer stock;

    private Integer status;

    private Integer sales;

    private String description;

    private Long merchantId;

    @TableField(exist = false)
    private String merchantName;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}