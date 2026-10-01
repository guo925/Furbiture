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

    /**
     * 商品所属商家ID。
     *
     * <p><b>仅管理员端使用。</b>商家端会忽略请求体里的这个字段，一律以登录身份覆写
     * （见 {@code MerchantProductController.addProduct}），因此商家无法借此把商品
     * 挂到别人名下。
     *
     * <p>管理员端由 {@code AdminProductController} 显式校验：必须是**存在且角色为 MERCHANT**
     * 的用户，否则拒绝。之所以不沿用"直接从请求体批量赋值"，是因为无校验的批量赋值
     * 可以写出任意 merchant_id（含不存在的商家）——那正是此前产生"无主商品"的根因。
     */
    private Long merchantId;
}
