package com.gjx.controller.admin;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.gjx.common.R;
import com.gjx.entity.Product;
import com.gjx.service.IProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 管理员商品管理控制器
 */
@RestController
@RequestMapping("/api/admin/products")
@Tag(name = "管理员商品管理", description = "管理员商品管理相关接口")
public class AdminProductController {

    @Autowired
    private IProductService productService;

    /**
     * 获取商品列表
     * @param page 页码
     * @param size 每页大小
     * @param name 商品名称
     * @param status 商品状态
     * @return 商品列表
     */
    @Operation(summary = "获取商品列表", description = "获取所有商品列表，支持分页、名称搜索和状态筛选")
    @GetMapping
    public R<Page<Product>> list(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) Integer status) {
        Page<Product> productPage = productService.adminListProducts(page, size, name, status);
        return R.ok(productPage);
    }

    /**
     * 获取商品详情
     * @param id 商品ID
     * @return 商品详情
     */
    @Operation(summary = "获取商品详情", description = "根据商品ID获取商品详情")
    @GetMapping("/{id}")
    public R<Product> detail(@PathVariable Long id) {
        Product product = productService.getById(id);
        if (product == null) {
            return R.error("商品不存在");
        }
        return R.ok(product);
    }

    /**
     * 创建商品
     * @param product 商品信息
     * @return 创建结果
     */
    @Operation(summary = "创建商品", description = "创建新商品")
    @PostMapping
    public R<?> create(@RequestBody Product product) {
        productService.save(product);
        return R.ok("创建成功");
    }

    /**
     * 更新商品
     * @param id 商品ID
     * @param product 商品信息
     * @return 更新结果
     */
    @Operation(summary = "更新商品", description = "更新商品信息")
    @PutMapping("/{id}")
    public R<?> update(@PathVariable Long id, @RequestBody Product product) {
        product.setId(id);
        productService.updateById(product);
        return R.ok("更新成功");
    }

    /**
     * 删除商品
     * @param id 商品ID
     * @return 删除结果
     */
    @Operation(summary = "删除商品", description = "删除商品")
    @DeleteMapping("/{id}")
    public R<?> delete(@PathVariable Long id) {
        productService.removeById(id);
        return R.ok("删除成功");
    }

    /**
     * 上架商品
     * @param id 商品ID
     * @return 操作结果
     */
    @Operation(summary = "上架商品", description = "上架商品")
    @PutMapping("/{id}/publish")
    public R<?> publish(@PathVariable Long id) {
        Product product = new Product();
        product.setId(id);
        product.setStatus(1);
        productService.updateById(product);
        return R.ok("上架成功");
    }

    /**
     * 下架商品
     * @param id 商品ID
     * @return 操作结果
     */
    @Operation(summary = "下架商品", description = "下架商品")
    @PutMapping("/{id}/unpublish")
    public R<?> unpublish(@PathVariable Long id) {
        Product product = new Product();
        product.setId(id);
        product.setStatus(0);
        productService.updateById(product);
        return R.ok("下架成功");
    }
}