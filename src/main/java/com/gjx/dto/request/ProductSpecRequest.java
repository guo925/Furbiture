package com.gjx.dto.request;

import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 商品规格 新增/更新 请求 DTO
 * <p>
 * 字段不设为强制非空，以兼容局部更新场景。
 */
@Data
public class ProductSpecRequest {

    private Long productId;

    @Size(max = 100, message = "规格名称不能超过100个字符")
    private String specName;

    @Size(max = 255, message = "规格值不能超过255个字符")
    private String specValue;
}
