package com.gjx.controller.merchant;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.gjx.common.R;
import com.gjx.common.ResultCode;
import com.gjx.entity.Category;
import com.gjx.entity.Product;
import com.gjx.mapper.ProductMapper;
import com.gjx.service.ICategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 商家分类管理控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/merchant")
@Tag(name = "商家分类管理", description = "商家商品分类CRUD接口")
public class MerchantCategoryController {

    @Autowired
    private ICategoryService categoryService;

    @Autowired
    private ProductMapper productMapper;

    @Operation(summary = "获取分类列表", description = "分页获取商品分类")
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
            if (category.getParentId() != null && category.getParentId() > 0) {
                Category parent = categoryService.getById(category.getParentId());
                if (parent != null) category.setParentName(parent.getName());
            }
        }
        return R.ok(categoryPage);
    }

    @Operation(summary = "添加分类", description = "添加商品分类")
    @PostMapping("/categories")
    public R<?> addCategory(@RequestBody Category category) {
        if (category.getParentId() == null || category.getParentId() == 0) {
            category.setLevel(1);
            category.setParentId(0L);
        } else {
            Category parent = categoryService.getById(category.getParentId());
            if (parent == null) return R.error(ResultCode.NOT_FOUND, "父分类不存在");
            category.setLevel(parent.getLevel() + 1);
        }
        if (category.getSortOrder() == null) category.setSortOrder(0);
        // 使用 service 层保存以触发 @CacheEvict 清除分类树缓存
        categoryService.save(category);
        log.info("[商家添加分类] name={}", category.getName());
        return R.ok("添加成功");
    }

    @Operation(summary = "更新分类", description = "更新商品分类")
    @PutMapping("/categories/{id}")
    public R<?> updateCategory(@PathVariable Long id, @RequestBody Category category) {
        Category existing = categoryService.getById(id);
        if (existing == null) return R.error(ResultCode.NOT_FOUND, "分类不存在");
        category.setId(id);
        if (category.getParentId() != null && !category.getParentId().equals(existing.getParentId())) {
            if (category.getParentId() == 0) {
                category.setLevel(1);
            } else {
                Category parent = categoryService.getById(category.getParentId());
                if (parent == null) return R.error(ResultCode.NOT_FOUND, "父分类不存在");
                category.setLevel(parent.getLevel() + 1);
            }
        }
        // 使用 service 层更新以触发 @CacheEvict 清除分类树缓存
        categoryService.updateById(category);
        return R.ok("更新成功");
    }

    @Operation(summary = "删除分类", description = "删除商品分类（需检查子分类和关联商品）")
    @DeleteMapping("/categories/{id}")
    public R<?> deleteCategory(@PathVariable Long id) {
        Category category = categoryService.getById(id);
        if (category == null) return R.error(ResultCode.NOT_FOUND, "分类不存在");

        List<Category> children = categoryService.lambdaQuery()
                .eq(Category::getParentId, id).list();
        if (!children.isEmpty()) return R.error(ResultCode.ERROR, "请先删除子分类");

        long productCount = productMapper.selectCount(
                new LambdaQueryWrapper<Product>().eq(Product::getCategoryId, id));
        if (productCount > 0) return R.error(ResultCode.ERROR, "该分类下存在商品，无法删除");

        // 使用 service 层删除以触发 @CacheEvict 清除分类树和商品列表缓存
        categoryService.removeById(id);
        return R.ok("删除成功");
    }
}
