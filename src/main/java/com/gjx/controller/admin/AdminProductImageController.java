package com.gjx.controller.admin;

import com.gjx.common.R;
import com.gjx.entity.ProductImage;
import com.gjx.service.IProductImageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 管理员商品图片管理控制器
 */
@RestController
@RequestMapping("/api/admin/product-images")
@Tag(name = "管理员商品图片管理", description = "管理员商品图片管理相关接口")
public class AdminProductImageController {

    @Autowired
    private IProductImageService productImageService;

    /**
     * 根据商品ID获取图片列表
     * @param productId 商品ID
     * @return 图片列表
     */
    @Operation(summary = "根据商品ID获取图片列表", description = "根据商品ID获取商品的图片列表")
    @GetMapping("/product/{productId}")
    public R<List<ProductImage>> listByProductId(@PathVariable Long productId) {
        List<ProductImage> images = productImageService.listByProductId(productId);
        return R.ok(images);
    }

    /**
     * 添加商品图片
     * @param productImage 商品图片信息
     * @return 添加结果
     */
    @Operation(summary = "添加商品图片", description = "为商品添加图片")
    @PostMapping
    public R<?> add(@RequestBody ProductImage productImage) {
        productImageService.save(productImage);
        return R.ok("添加成功");
    }

    /**
     * 更新商品图片
     * @param id 图片ID
     * @param productImage 商品图片信息
     * @return 更新结果
     */
    @Operation(summary = "更新商品图片", description = "更新商品图片信息")
    @PutMapping("/{id}")
    public R<?> update(@PathVariable Long id, @RequestBody ProductImage productImage) {
        productImage.setId(id);
        productImageService.updateById(productImage);
        return R.ok("更新成功");
    }

    /**
     * 删除商品图片
     * @param id 图片ID
     * @return 删除结果
     */
    @Operation(summary = "删除商品图片", description = "删除商品图片")
    @DeleteMapping("/{id}")
    public R<?> delete(@PathVariable Long id) {
        productImageService.removeById(id);
        return R.ok("删除成功");
    }

    /**
     * 批量删除商品图片
     * @param ids 图片ID列表
     * @return 删除结果
     */
    @Operation(summary = "批量删除商品图片", description = "批量删除商品图片")
    @DeleteMapping
    public R<?> batchDelete(@RequestParam List<Long> ids) {
        productImageService.removeByIds(ids);
        return R.ok("删除成功");
    }
}