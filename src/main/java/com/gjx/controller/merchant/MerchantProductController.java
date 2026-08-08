package com.gjx.controller.merchant;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.gjx.common.R;
import com.gjx.common.ResultCode;
import com.gjx.entity.Product;
import com.gjx.enums.UserRoleEnum;
import com.gjx.service.IProductService;
import com.gjx.service.IUserService;
import com.gjx.util.AuthenticationUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

/**
 * 商家商品管理控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/merchant")
@Tag(name = "商家商品管理", description = "商家商品CRUD接口")
public class MerchantProductController {

    @Autowired
    private IProductService productService;

    @Autowired
    private IUserService userService;

    @Operation(summary = "获取商品列表", description = "获取当前商家的商品分页列表")
    @GetMapping("/products")
    public R<Page<Product>> getProducts(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer status,
            HttpServletRequest request) {
        Long merchantId = getMerchantId(request);

        LambdaQueryWrapper<Product> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Product::getMerchantId, merchantId);
        if (keyword != null && !keyword.isEmpty()) {
            queryWrapper.and(w -> w.like(Product::getName, keyword).or().like(Product::getBrand, keyword));
        }
        if (status != null) {
            queryWrapper.eq(Product::getStatus, status);
        }
        queryWrapper.orderByDesc(Product::getCreateTime);

        return R.ok(productService.page(new Page<>(page, size), queryWrapper));
    }

    @Operation(summary = "添加商品", description = "商家添加新商品")
    @PostMapping("/products")
    public R<?> addProduct(@RequestBody Product product, HttpServletRequest request) {
        Long merchantId = getMerchantId(request);
        product.setMerchantId(merchantId);
        product.setSales(0);
        product.setStatus(1);
        productService.save(product);
        log.info("[商家添加商品] merchantId={}, productName={}", merchantId, product.getName());
        return R.ok("添加成功");
    }

    @Operation(summary = "更新商品", description = "商家更新自己的商品")
    @PutMapping("/products/{id}")
    public R<?> updateProduct(@PathVariable Long id, @RequestBody Product product, HttpServletRequest request) {
        Long merchantId = getMerchantId(request);
        Product existing = productService.getById(id);
        if (existing == null || !existing.getMerchantId().equals(merchantId)) {
            return R.error(ResultCode.FORBIDDEN, "无权操作此商品");
        }
        product.setId(id);
        product.setMerchantId(merchantId);
        product.setSales(existing.getSales());
        productService.updateById(product);
        log.info("[商家更新商品] productId={}, merchantId={}", id, merchantId);
        return R.ok("更新成功");
    }

    @Operation(summary = "删除商品", description = "商家删除自己的商品")
    @DeleteMapping("/products/{id}")
    public R<?> deleteProduct(@PathVariable Long id, HttpServletRequest request) {
        Long merchantId = getMerchantId(request);
        Product existing = productService.getById(id);
        if (existing == null || !existing.getMerchantId().equals(merchantId)) {
            return R.error(ResultCode.FORBIDDEN, "无权操作此商品");
        }
        productService.removeById(id);
        log.info("[商家删除商品] productId={}, merchantId={}", id, merchantId);
        return R.ok("删除成功");
    }

    @Operation(summary = "批量更新商品状态", description = "批量上下架商品")
    @PutMapping("/products/batch/status")
    public R<?> batchUpdateStatus(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        Long merchantId = getMerchantId(request);
        @SuppressWarnings("unchecked")
        List<Integer> ids = (List<Integer>) body.get("ids");
        Integer status = (Integer) body.get("status");
        if (ids == null || ids.isEmpty() || status == null) return R.error("参数错误");
        for (Integer id : ids) {
            Product p = productService.getById(id.longValue());
            if (p != null && p.getMerchantId().equals(merchantId)) {
                p.setStatus(status);
                productService.updateById(p);
            }
        }
        return R.ok("批量更新成功");
    }

    @Operation(summary = "批量删除商品")
    @DeleteMapping("/products/batch")
    public R<?> batchDelete(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        Long merchantId = getMerchantId(request);
        @SuppressWarnings("unchecked")
        List<Integer> ids = (List<Integer>) body.get("ids");
        if (ids == null || ids.isEmpty()) return R.error("参数错误");
        for (Integer id : ids) {
            Product p = productService.getById(id.longValue());
            if (p != null && p.getMerchantId().equals(merchantId)) {
                productService.removeById(id.longValue());
            }
        }
        return R.ok("批量删除成功");
    }

    private Long getMerchantId(HttpServletRequest request) {
        return userService.findByUsername(AuthenticationUtil.getUsernameFromRequest(request)).getId();
    }
}
