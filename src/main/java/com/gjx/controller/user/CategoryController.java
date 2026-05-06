package com.gjx.controller.user;

import com.gjx.common.R;
import com.gjx.entity.Category;
import com.gjx.service.ICategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 分类控制器
 */
@RestController
@RequestMapping("/api/categories")
@Tag(name = "分类管理", description = "分类相关接口")
public class CategoryController {

    @Autowired
    private ICategoryService categoryService;

    /**
     * 获取分类列表
     * @return 分类列表
     */
    @Operation(summary = "获取分类列表", description = "获取所有商品分类")
    @GetMapping
    public R<List<Category>> list() {
        List<Category> categories = categoryService.list();
        return R.ok(categories);
    }

    /**
     * 获取分类树形结构
     * @return 分类树形结构
     */
    @Operation(summary = "获取分类树形结构", description = "获取分类的树形结构")
    @GetMapping("/tree")
    public R<List<Category>> tree() {
        List<Category> categoryTree = categoryService.getCategoryTree();
        return R.ok(categoryTree);
    }
}