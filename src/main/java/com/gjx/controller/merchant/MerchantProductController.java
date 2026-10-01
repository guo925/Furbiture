package com.gjx.controller.merchant;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.gjx.common.R;
import com.gjx.common.ResultCode;
import com.gjx.dto.request.BatchProductIdsRequest;
import com.gjx.dto.request.BatchProductStatusRequest;
import com.gjx.dto.request.CreateProductRequest;
import com.gjx.entity.Product;
import com.gjx.enums.UserRoleEnum;
import com.gjx.service.IProductService;
import com.gjx.service.IUserService;
import com.gjx.util.AuthenticationUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Objects;

/**
 * 商家商品管理控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/merchant")
@Tag(name = "商家商品管理", description = "商家商品CRUD接口")
@RequiredArgsConstructor
public class MerchantProductController {

    private final AuthenticationUtil authUtil;

    private final IProductService productService;

    private final IUserService userService;

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
    public R<?> addProduct(@Valid @RequestBody CreateProductRequest productRequest, HttpServletRequest request) {
        Long merchantId = getMerchantId(request);
        Product product = toEntity(productRequest);
        // 归属与初始状态一律由服务端覆写，忽略请求体里的 merchantId / sales / status
        product.setMerchantId(merchantId);
        product.setSales(0);
        product.setStatus(1);
        productService.save(product);
        log.info("[商家添加商品] merchantId={}, productName={}", merchantId, product.getName());
        return R.ok("添加成功");
    }

    @Operation(summary = "更新商品", description = "商家更新自己的商品")
    @PutMapping("/products/{id}")
    public R<?> updateProduct(@PathVariable Long id, @Valid @RequestBody CreateProductRequest productRequest,
                              HttpServletRequest request) {
        Long merchantId = getMerchantId(request);
        // 归属判定用 Objects.equals 而非 existing.getMerchantId().equals(merchantId)：
        // merchant_id 存在为 NULL 的历史数据（管理员端商品创建曾不写归属，现已改为必填，
        // 但库里可能残留这类"无主商品"）。对 NULL 归属调用 .equals 会抛 NPE →
        // 被全局兜底成 500，把"无权操作"错报成"服务器内部错误"，也留下 403/500 的存在性探测差异。
        // Objects.equals 下 NULL 归属与任何商家都不相等 → 403。
        // （merchantId 来自已认证用户，必不为 NULL，所以不存在"双方都 NULL 判为相等"的漏洞）
        Product existing = productService.getById(id);
        if (existing == null || !Objects.equals(existing.getMerchantId(), merchantId)) {
            return R.error(ResultCode.FORBIDDEN, "无权操作此商品");
        }
        Product product = toEntity(productRequest);
        product.setId(id);
        product.setMerchantId(merchantId);
        product.setSales(existing.getSales());
        productService.updateById(product);
        log.info("[商家更新商品] productId={}, merchantId={}", id, merchantId);
        return R.ok("更新成功");
    }

    /**
     * DTO → 实体映射，只映射允许客户端提交的业务字段
     */
    private Product toEntity(CreateProductRequest request) {
        Product product = new Product();
        product.setName(request.getName());
        product.setCategoryId(request.getCategoryId());
        product.setBrand(request.getBrand());
        product.setMainImage(request.getMainImage());
        product.setPrice(request.getPrice());
        product.setStock(request.getStock());
        product.setDescription(request.getDescription());
        product.setStatus(request.getStatus());
        return product;
    }


    @Operation(summary = "删除商品", description = "商家删除自己的商品")
    @DeleteMapping("/products/{id}")
    public R<?> deleteProduct(@PathVariable Long id, HttpServletRequest request) {
        Long merchantId = getMerchantId(request);
        // 同 updateProduct：NULL 归属（管理员创建的商品）不能走 .equals，否则 500
        Product existing = productService.getById(id);
        if (existing == null || !Objects.equals(existing.getMerchantId(), merchantId)) {
            return R.error(ResultCode.FORBIDDEN, "无权操作此商品");
        }
        productService.removeById(id);
        log.info("[商家删除商品] productId={}, merchantId={}", id, merchantId);
        return R.ok("删除成功");
    }

    @Operation(summary = "批量更新商品状态", description = "批量上下架商品")
    @PutMapping("/products/batch/status")
    public R<?> batchUpdateStatus(@Valid @RequestBody BatchProductStatusRequest body, HttpServletRequest request) {
        Long merchantId = getMerchantId(request);
        List<Long> ids = body.getIds();
        Integer status = body.getStatus();

        // 一次性查出当前商家名下的商品，既避免逐个查询（N+1），
        // 也用数量比对严格拦截越权 ID —— 只要有一个 ID 不属于本商家就整体拒绝，不做静默跳过
        List<Product> ownedProducts = productService.list(new LambdaQueryWrapper<Product>()
                .in(Product::getId, ids)
                .eq(Product::getMerchantId, merchantId));
        if (ownedProducts.size() != ids.size()) {
            return R.error(ResultCode.FORBIDDEN, "存在无权操作的商品");
        }

        ownedProducts.forEach(p -> p.setStatus(status));
        productService.updateBatchById(ownedProducts);
        log.info("[商家批量更新商品状态] merchantId={}, count={}, status={}", merchantId, ids.size(), status);
        return R.ok("批量更新成功");
    }

    @Operation(summary = "批量删除商品")
    @DeleteMapping("/products/batch")
    public R<?> batchDelete(@Valid @RequestBody BatchProductIdsRequest body, HttpServletRequest request) {
        Long merchantId = getMerchantId(request);
        List<Long> ids = body.getIds();

        long ownedCount = productService.count(new LambdaQueryWrapper<Product>()
                .in(Product::getId, ids)
                .eq(Product::getMerchantId, merchantId));
        if (ownedCount != ids.size()) {
            return R.error(ResultCode.FORBIDDEN, "存在无权操作的商品");
        }

        productService.removeByIds(ids);
        log.info("[商家批量删除商品] merchantId={}, count={}", merchantId, ids.size());
        return R.ok("批量删除成功");
    }

    private Long getMerchantId(HttpServletRequest request) {
        return userService.findByUsername(authUtil.getUsernameFromRequest(request)).getId();
    }
}
