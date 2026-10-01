package com.gjx.controller.admin;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.gjx.common.R;
import com.gjx.common.ResultCode;
import com.gjx.dto.request.CategoryRequest;
import com.gjx.entity.Category;
import com.gjx.service.ICategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 管理员分类管理控制器
 * <p>
 * 分类（category 表）是全平台共享数据，表里没有 merchant_id，不归属任何商家，
 * 因此分类的增删改<b>只</b>开放给管理员（本控制器）。商家端
 * {@code /api/merchant/categories} 仅保留只读查询，供商家发布商品时选择分类。
 * <p>
 * 层次结构的两条不变式（父子环、level 一致性）由 {@link ICategoryService} 的实现统一保证，
 * 本控制器只负责请求接收与参数校验。
 */
@Slf4j
@RestController
@RequestMapping("/api/admin/categories")
@Tag(name = "管理员分类管理", description = "管理员分类管理相关接口")
@RequiredArgsConstructor
public class AdminCategoryController {

    private final ICategoryService categoryService;

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
            return R.error(ResultCode.NOT_FOUND, "分类不存在");
        }
        return R.ok(category);
    }

    /**
     * 创建分类
     * @param request 分类信息（DTO）
     * @return 创建结果
     */
    @Operation(summary = "创建分类", description = "创建新分类")
    @PostMapping
    public R<?> create(@Valid @RequestBody CategoryRequest request) {
        Category category = toEntity(request);
        // parentId 为空按顶级分类处理（与建表脚本 parent_id NOT NULL DEFAULT 0 一致），
        // 否则会向 NOT NULL 列写入 null 而报错。level 由 Service 按父级推导。
        if (category.getParentId() == null) {
            category.setParentId(0L);
        }
        if (category.getSortOrder() == null) {
            category.setSortOrder(0);
        }
        categoryService.save(category);
        log.info("[管理员创建分类] id={}, name={}", category.getId(), category.getName());
        return R.ok("创建成功");
    }

    /**
     * 更新分类
     * <p>
     * parentId 变动时，Service 层会做环检测（拒绝挂到自身或子孙下）并递归重算子树 level；
     * 命中环时抛 BusinessException(ResultCode.PARAM_ERROR, "不能将分类挂到自身或其子分类下")，
     * 由 GlobalExceptionHandler 统一转成 {@code R.error(400, ...)}。
     *
     * @param id 分类ID
     * @param request 分类信息（DTO）
     * @return 更新结果
     */
    @Operation(summary = "更新分类", description = "更新分类信息；parentId 变动时会做环检测并重算子树层级")
    @PutMapping("/{id}")
    public R<?> update(@PathVariable Long id, @Valid @RequestBody CategoryRequest request) {
        Category category = toEntity(request);
        category.setId(id);
        // 按影响行数判断成败：id 不存在时 updateById 返回 false，不能当作更新成功
        if (!categoryService.updateById(category)) {
            return R.error(ResultCode.NOT_FOUND, "分类不存在");
        }
        categoryService.evictCategoryTreeCache();
        log.info("[管理员更新分类] id={}", id);
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

    /**
     * DTO → 实体映射，只映射允许客户端提交的字段（level 由服务端推导，不接受客户端注入）
     */
    private Category toEntity(CategoryRequest request) {
        Category category = new Category();
        category.setName(request.getName());
        category.setParentId(request.getParentId());
        category.setSortOrder(request.getSortOrder());
        category.setIcon(request.getIcon());
        category.setStatus(request.getStatus());
        return category;
    }
}
