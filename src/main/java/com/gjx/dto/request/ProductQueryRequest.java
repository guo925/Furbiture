package com.gjx.dto.request;

import lombok.Data;

/**
 * 商品查询请求 DTO
 */
@Data
public class ProductQueryRequest {

    private String keyword;

    private Long categoryId;

    private Integer status;

    private String sort; // sales_desc, price_asc, price_desc, default

    private Double minPrice;

    private Double maxPrice;
}
