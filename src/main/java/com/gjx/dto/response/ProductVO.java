package com.gjx.dto.response;

import com.gjx.entity.Product;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 商品视图对象
 */
@Data
@Builder
public class ProductVO {

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

    public static ProductVO from(Product product) {
        return ProductVO.builder()
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
                .build();
    }
}
