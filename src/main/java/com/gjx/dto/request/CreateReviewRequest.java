package com.gjx.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 创建商品评价请求 DTO
 * <p>
 * 原先用 {@code Map<String,Object>} 接参并直接 {@code body.get("productId").toString()}，
 * 缺字段时会抛 NPE；改为 DTO + {@code @Valid} 后由统一异常处理器给出明确的参数错误。
 */
@Data
public class CreateReviewRequest {

    @NotNull(message = "商品ID不能为空")
    private Long productId;

    /** 关联订单ID，可为空 */
    private Long orderId;

    @NotNull(message = "评分不能为空")
    @Min(value = 1, message = "评分最低为1分")
    @Max(value = 5, message = "评分最高为5分")
    private Integer rating;

    @Size(max = 500, message = "评价内容不能超过500个字符")
    private String content;

    /** 评价图片地址（多个以逗号分隔） */
    private String images;
}
