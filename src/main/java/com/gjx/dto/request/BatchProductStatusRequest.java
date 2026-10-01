package com.gjx.dto.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * 商家批量更新商品状态请求 DTO
 * <p>
 * 原先用 {@code Map<String,Object>} 接参并自行解析 ID（需兼容 Integer/Long 两种反序列化结果），
 * 改为强类型 DTO 后由 Jackson 统一转换，省去手写解析。
 */
@Data
public class BatchProductStatusRequest {

    @NotEmpty(message = "请选择要操作的商品")
    private List<Long> ids;

    @NotNull(message = "状态不能为空")
    private Integer status;
}
