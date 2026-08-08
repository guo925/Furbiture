package com.gjx.dto.response;

import com.gjx.entity.Product;
import com.gjx.entity.ProductImage;
import com.gjx.entity.ProductSpec;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 商品详情 VO — 供前端商品详情页使用
 * <p>
 * 聚合商品基本信息、图片列表、规格列表、评价统计等。
 */
@Data
@Builder
public class ProductDetailVO {

    private Long id;
    private String name;
    private Long categoryId;
    private String categoryName;
    private String brand;
    private String mainImage;
    private BigDecimal price;
    private Integer stock;
    private Integer status;
    private Integer sales;
    private String description;
    private Long merchantId;
    private String merchantName;
    private LocalDateTime createTime;

    /** 商品图片列表 */
    private List<String> images;

    /** 商品规格分组 */
    private List<SpecGroup> specs;

    /** 平均评分 */
    private Double avgRating;

    /** 评价数量 */
    private Integer reviewCount;

    /**
     * 规格分组（如 "颜色": ["灰色","米色"]）
     */
    @Data
    @Builder
    public static class SpecGroup {
        private String name;
        private List<String> values;
    }

    /**
     * 从 Product 实体 + 关联数据构建 VO
     */
    public static ProductDetailVO from(Product product,
                                        List<ProductImage> images,
                                        List<ProductSpec> specs,
                                        List<String> imageUrls) {
        // 将规格按名称分组
        java.util.Map<String, java.util.List<String>> specGroups = new java.util.LinkedHashMap<>();
        if (specs != null) {
            for (ProductSpec spec : specs) {
                specGroups.computeIfAbsent(spec.getSpecName(), k -> new java.util.ArrayList<>())
                        .add(spec.getSpecValue());
            }
        }

        List<SpecGroup> specGroupList = new java.util.ArrayList<>();
        specGroups.forEach((name, values) ->
                specGroupList.add(SpecGroup.builder().name(name).values(values).build()));

        return ProductDetailVO.builder()
                .id(product.getId())
                .name(product.getName())
                .categoryId(product.getCategoryId())
                .categoryName(product.getCategoryName())
                .brand(product.getBrand())
                .mainImage(product.getMainImage())
                .price(product.getPrice())
                .stock(product.getStock())
                .status(product.getStatus())
                .sales(product.getSales())
                .description(product.getDescription())
                .merchantId(product.getMerchantId())
                .merchantName(product.getMerchantName())
                .createTime(product.getCreateTime())
                .images(imageUrls != null ? imageUrls : new java.util.ArrayList<>())
                .specs(specGroupList)
                .avgRating(null)   // 评价系统实现后补充
                .reviewCount(null)  // 评价系统实现后补充
                .build();
    }
}
