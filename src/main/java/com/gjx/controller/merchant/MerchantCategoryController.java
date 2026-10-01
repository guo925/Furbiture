package com.gjx.controller.merchant;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.gjx.common.R;
import com.gjx.entity.Category;
import com.gjx.service.ICategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 商家端分类查询控制器（只读）
 * <p>
 * <b>为什么分类不收归商家管理</b>：category 表是<b>全平台共享数据</b>，表内没有 merchant_id，
 * 任何一个商家都不"拥有"任何一条分类。分类的增删改一律归管理员端
 * {@code /api/admin/categories}（SecurityConfig 中限定 ADMIN 权限）。
 * <p>
 * 本控制器此前带有 POST / PUT / DELETE 三个写接口，而 {@code /api/merchant/**}
 * 仅要求 MERCHANT 权限 → <b>任意商家都能增删改全平台分类</b>：可删除别人正使用的分类、
 * 可把分类挂到任意位置制造环，且每次写入都会连带清空分类树缓存，
 * 污染前台所有页面的分类展示。故移除全部写接口，只保留只读查询。
 */
@RestController
@RequestMapping("/api/merchant")
@Tag(name = "商家分类管理", description = "商家端分类查询接口（只读）")
@RequiredArgsConstructor
public class MerchantCategoryController {

    private final ICategoryService categoryService;

    /**
     * 分页获取商品分类（只读）
     * @param page 页码
     * @param size 每页大小
     * @param name 分类名称（模糊搜索，可选）
     * @return 分类分页数据
     */
    @Operation(summary = "获取分类列表", description = "分页获取商品分类（只读）")
    @GetMapping("/categories")
    public R<Page<Category>> getCategories(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String name) {
        Page<Category> categoryPage;
        if (name != null && !name.isEmpty()) {
            categoryPage = categoryService.lambdaQuery().like(Category::getName, name)
                    .page(new Page<>(page, size));
        } else {
            categoryPage = categoryService.page(new Page<>(page, size));
        }
        for (Category category : categoryPage.getRecords()) {
            // 先判 null 再拆箱比较：parentId 为 null 时直接 > 0 会 NPE
            if (category.getParentId() != null && category.getParentId() > 0) {
                Category parent = categoryService.getById(category.getParentId());
                if (parent != null) category.setParentName(parent.getName());
            }
        }
        return R.ok(categoryPage);
    }
}
