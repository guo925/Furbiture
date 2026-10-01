package com.gjx.controller.user;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.gjx.common.R;
import com.gjx.common.ResultCode;
import com.gjx.entity.Product;
import com.gjx.entity.ProductImage;
import com.gjx.entity.ProductSpec;
import com.gjx.service.IProductService;
import com.gjx.service.IProductImageService;
import com.gjx.service.IProductSpecService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 商品控制器
 */
@RestController
@RequestMapping("/api/products")
@Tag(name = "商品管理", description = "商品相关接口")
@RequiredArgsConstructor
public class ProductController {

    /** 关键词长度上限，与 ProductServiceImpl 缓存 key 的截断长度保持一致。 */
    private static final int KEYWORD_MAX_LENGTH = 32;

    private final IProductService productService;
    
    private final IProductImageService productImageService;
    
    private final IProductSpecService productSpecService;

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
        // 在入口拒绝超长关键词：既避免污染缓存 key，也避免超长 LIKE 拖慢查询
        if (isKeywordTooLong(keyword)) {
            return R.error(ResultCode.PARAM_ERROR, "关键词长度不能超过 " + KEYWORD_MAX_LENGTH + " 个字符");
        }
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
            return R.error(ResultCode.NOT_FOUND, "商品不存在");
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
        if (isKeywordTooLong(keyword)) {
            return R.error(ResultCode.PARAM_ERROR, "关键词长度不能超过 " + KEYWORD_MAX_LENGTH + " 个字符");
        }
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
        if (isKeywordTooLong(keyword)) {
            return R.error(ResultCode.PARAM_ERROR, "关键词长度不能超过 " + KEYWORD_MAX_LENGTH + " 个字符");
        }
        Page<Product> products = productService.searchProducts(keyword, 1, 5);
        List<String> names = products.getRecords().stream()
                .map(Product::getName)
                .limit(5)
                .collect(java.util.stream.Collectors.toList());
        return R.ok(names);
    }

    /**
     * 判断关键词是否超过长度上限。
     * <p>
     * 超长关键词会被拒绝而不是静默截断：截断会让用户以为搜到了别的东西，
     * 直接在入口返回参数错误更符合“fail fast”的接口契约。
     */
    private static boolean isKeywordTooLong(String keyword) {
        return keyword != null && keyword.trim().length() > KEYWORD_MAX_LENGTH;
    }
}
