package com.gjx.dto.request;

import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 商品分类 新增/更新 请求 DTO
 * <p>
 * 原先直接以 {@code Category} 实体接收请求体，客户端可注入 {@code id} / {@code level} / {@code children}
 * 等字段。改为 DTO 后只暴露业务字段；{@code level} 一律由服务端按父级推导。
 * <p>
 * 注意：{@code name} 未加 {@code @NotBlank}，因为商家端「启用/禁用」是只提交 {@code status}
 * 的局部更新（见 MerchantCategories.vue 的 handleStatusChange），若强制名称非空会把该操作误拦。
 */
@Data
public class CategoryRequest {

    @Size(max = 50, message = "分类名称不能超过50个字符")
    private String name;

    /** 父分类ID，0 或空表示顶级分类 */
    private Long parentId;

    /** 排序序号 */
    private Integer sortOrder;

    @Size(max = 255, message = "分类图标地址不能超过255个字符")
    private String icon;

    /** 状态：0-禁用 1-启用 */
    private Integer status;
}
