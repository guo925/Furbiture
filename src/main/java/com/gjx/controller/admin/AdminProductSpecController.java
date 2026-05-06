package com.gjx.controller.admin;

import com.gjx.common.R;
import com.gjx.entity.ProductSpec;
import com.gjx.service.IProductSpecService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 管理员商品规格管理控制器
 */
@RestController
@RequestMapping("/admin/product-specs")
@Tag(name = "管理员商品规格管理", description = "管理员商品规格管理相关接口")
public class AdminProductSpecController {

    @Autowired
    private IProductSpecService productSpecService;

    /**
     * 根据商品ID获取规格列表
     * @param productId 商品ID
     * @return 规格列表
     */
    @Operation(summary = "根据商品ID获取规格列表", description = "根据商品ID获取商品的规格列表")
    @GetMapping("/product/{productId}")
    public R<List<ProductSpec>> listByProductId(@PathVariable Long productId) {
        List<ProductSpec> specs = productSpecService.listByProductId(productId);
        return R.ok(specs);
    }

    /**
     * 添加商品规格
     * @param productSpec 商品规格信息
     * @return 添加结果
     */
    @Operation(summary = "添加商品规格", description = "为商品添加规格")
    @PostMapping
    public R<?> add(@RequestBody ProductSpec productSpec) {
        productSpecService.save(productSpec);
        return R.ok("添加成功");
    }

    /**
     * 更新商品规格
     * @param id 规格ID
     * @param productSpec 商品规格信息
     * @return 更新结果
     */
    @Operation(summary = "更新商品规格", description = "更新商品规格信息")
    @PutMapping("/{id}")
    public R<?> update(@PathVariable Long id, @RequestBody ProductSpec productSpec) {
        productSpec.setId(id);
        productSpecService.updateById(productSpec);
        return R.ok("更新成功");
    }

    /**
     * 删除商品规格
     * @param id 规格ID
     * @return 删除结果
     */
    @Operation(summary = "删除商品规格", description = "删除商品规格")
    @DeleteMapping("/{id}")
    public R<?> delete(@PathVariable Long id) {
        productSpecService.removeById(id);
        return R.ok("删除成功");
    }

    /**
     * 批量删除商品规格
     * @param ids 规格ID列表
     * @return 删除结果
     */
    @Operation(summary = "批量删除商品规格", description = "批量删除商品规格")
    @DeleteMapping
    public R<?> batchDelete(@RequestParam List<Long> ids) {
        productSpecService.removeByIds(ids);
        return R.ok("删除成功");
    }
}