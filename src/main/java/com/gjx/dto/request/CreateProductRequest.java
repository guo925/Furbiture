package com.gjx.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 创建/更新商品请求 DTO
 * <p>
 * 统一管理员和商家端的商品创建/更新参数，带参数校验。
 */
@Data
public class CreateProductRequest {

    @NotBlank(message = "商品名称不能为空")
    private String name;

    @NotNull(message = "分类不能为空")
    private Long categoryId;

    @NotBlank(message = "品牌不能为空")
    private String brand;

    private String mainImage;

    @NotNull(message = "价格不能为空")
    @PositiveOrZero(message = "价格不能为负数")
    private BigDecimal price;

    @NotNull(message = "库存不能为空")
    @PositiveOrZero(message = "库存不能为负数")
    private Integer stock;

    private String description;

    /** 状态：1-上架 0-下架，默认上架 */
    private Integer status;
}
