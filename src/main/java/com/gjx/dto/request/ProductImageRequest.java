package com.gjx.dto.request;

import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 商品图片 新增/更新 请求 DTO
 * <p>
 * 字段不设为强制非空，以兼容「只更新排序」这类局部更新场景。
 */
@Data
public class ProductImageRequest {

    private Long productId;

    @Size(max = 255, message = "图片地址不能超过255个字符")
    private String imageUrl;

    private Integer sortOrder;
}
