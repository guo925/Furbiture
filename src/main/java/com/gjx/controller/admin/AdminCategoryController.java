package com.gjx.controller.admin;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.gjx.common.R;
import com.gjx.entity.Category;
import com.gjx.service.ICategoryService;
import lombok.extern.slf4j.Slf4j;
import com.gjx.service.Impl.CategoryServiceImpl;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.List;

/**
 * 管理员分类管理控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/admin/categories")
@Tag(name = "管理员分类管理", description = "管理员分类管理相关接口")
public class AdminCategoryController {

    @Autowired
    private ICategoryService categoryService;

    /**
     * 获取分类列表（分页）
     * @param page 页码
     * @param size 每页大小
     * @param name 分类名称（搜索）
     * @return 分类列表（分页）
     */
    @Operation(summary = "获取分类列表", description = "获取所有分类列表（分页）")
    @GetMapping
    public R<Page<Category>> list(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String name) {
        Page<Category> categoryPage;
        
        if (name != null && !name.isEmpty()) {
            // 按名称搜索
            categoryPage = categoryService.lambdaQuery()
                    .like(Category::getName, name)
                    .page(new Page<>(page, size));
        } else {
            categoryPage = categoryService.page(new Page<>(page, size));
        }
        
        // 设置父分类名称
        for (Category category : categoryPage.getRecords()) {
            if (category.getParentId() != null && category.getParentId() > 0) {
                Category parent = categoryService.getById(category.getParentId());
                if (parent != null) {
                    category.setParentName(parent.getName());
                }
            }
        }
        
        return R.ok(categoryPage);
    }

    /**
     * 获取分类树形结构
     * @return 分类树形结构列表
     */
    @Operation(summary = "获取分类树", description = "获取分类树形结构")
    @GetMapping("/tree")
    public R<List<Category>> getTree() {
        List<Category> categoryTree = categoryService.getCategoryTree();
        return R.ok(categoryTree);
    }

    /**
     * 获取分类详情
     * @param id 分类ID
     * @return 分类详情
     */
    @Operation(summary = "获取分类详情", description = "根据分类ID获取分类详情")
    @GetMapping("/{id}")
    public R<Category> detail(@PathVariable Long id) {
        Category category = categoryService.getById(id);
        if (category == null) {
            return R.error("分类不存在");
        }
        return R.ok(category);
    }

    /**
     * 创建分类
     * @param category 分类信息
     * @return 创建结果
     */
    @Operation(summary = "创建分类", description = "创建新分类")
    @PostMapping
    public R<?> create(@RequestBody Category category) {
        categoryService.save(category);
        return R.ok("创建成功");
    }

    /**
     * 更新分类
     * @param id 分类ID
     * @param category 分类信息
     * @return 更新结果
     */
    @Operation(summary = "更新分类", description = "更新分类信息")
    @PutMapping("/{id}")
    public R<?> update(@PathVariable Long id, @RequestBody Category category) {
        category.setId(id);
        categoryService.updateById(category);
        categoryService.evictCategoryTreeCache();
        log.info("[管理员更新分类] id={}", category.getId());
        return R.ok("更新成功");
    }

    /**
     * 删除分类
     * @param id 分类ID
     * @return 删除结果
     */
    @Operation(summary = "删除分类", description = "删除分类")
    @DeleteMapping("/{id}")
    public R<?> delete(@PathVariable Long id) {
        categoryService.removeById(id);
        categoryService.evictCategoryTreeCache();
        log.info("[管理员删除分类] id={}", id);
        return R.ok("删除成功");
    }
}