package com.gjx.controller.user;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.gjx.common.R;
import com.gjx.entity.Product;
import com.gjx.entity.ProductImage;
import com.gjx.entity.ProductSpec;
import com.gjx.service.IProductService;
import com.gjx.service.IProductImageService;
import com.gjx.service.IProductSpecService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 商品控制器
 */
@RestController
@RequestMapping("/api/products")
@Tag(name = "商品管理", description = "商品相关接口")
public class ProductController {

    @Autowired
    private IProductService productService;
    
    @Autowired
    private IProductImageService productImageService;
    
    @Autowired
    private IProductSpecService productSpecService;

    /**
     * 获取商品列表
     * @param categoryId 分类ID（可选）
     * @param keyword 关键词（可选）
     * @param page 页码
     * @param size 每页大小
     * @return 商品列表
     */
    @Operation(summary = "获取商品列表", description = "获取商品列表，支持分类筛选和关键词搜索")
    @GetMapping
    public R<Page<Product>> list(
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String sortBy,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {
        Page<Product> productPage = productService.listProducts(categoryId, keyword, sortBy, page, size);
        return R.ok(productPage);
    }

    /**
     * 获取商品详情
     * @param id 商品ID
     * @return 商品详情
     */
    @Operation(summary = "获取商品详情", description = "根据商品ID获取商品详细信息")
    @GetMapping("/{id}")
    public R<?> detail(@PathVariable Long id) {
        // 获取商品基本信息
        Product product = productService.getById(id);
        if (product == null) {
            return R.error("商品不存在");
        }
        
        // 获取商品图片
        List<ProductImage> images = productImageService.listByProductId(id);
        
        // 获取商品规格
        List<ProductSpec> specs = productSpecService.listByProductId(id);
        
        // 构建响应数据
        java.util.Map<String, Object> result = new java.util.HashMap<>();
        result.put("product", product);
        result.put("images", images);
        result.put("specs", specs);
        
        return R.ok(result);
    }

    /**
     * 搜索商品
     * @param keyword 搜索关键词
     * @param page 页码
     * @param size 每页大小
     * @return 搜索结果
     */
    @Operation(summary = "搜索商品", description = "根据关键词搜索商品")
    @GetMapping("/search")
    public R<Page<Product>> search(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {
        Page<Product> productPage = productService.searchProducts(keyword, page, size);
        return R.ok(productPage);
    }

    /**
     * 搜索建议（自动补全）
     * @param keyword 搜索关键词
     * @return 最多5条匹配建议
     */
    @Operation(summary = "搜索建议", description = "根据关键词返回自动补全建议")
    @GetMapping("/suggestions")
    public R<List<String>> suggestions(@RequestParam String keyword) {
        Page<Product> products = productService.searchProducts(keyword, 1, 5);
        List<String> names = products.getRecords().stream()
                .map(Product::getName)
                .limit(5)
                .collect(java.util.stream.Collectors.toList());
        return R.ok(names);
    }
}