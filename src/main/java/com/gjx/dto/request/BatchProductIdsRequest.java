package com.gjx.dto.request;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

/**
 * 批量商品操作（按 ID）请求 DTO
 */
@Data
public class BatchProductIdsRequest {

    @NotEmpty(message = "请选择要操作的商品")
    private List<Long> ids;
}
