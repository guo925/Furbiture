package com.gjx.controller.admin;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.gjx.common.R;
import com.gjx.common.ResultCode;
import com.gjx.dto.request.CreateProductRequest;
import com.gjx.entity.Product;
import com.gjx.entity.User;
import com.gjx.enums.UserRoleEnum;
import com.gjx.service.IProductService;
import com.gjx.service.IUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 管理员商品管理控制器
 */
@RestController
@RequestMapping("/api/admin/products")
@Tag(name = "管理员商品管理", description = "管理员商品管理相关接口")
@RequiredArgsConstructor
public class AdminProductController {

    private final IProductService productService;

    private final IUserService userService;

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
            return R.error(ResultCode.NOT_FOUND, "商品不存在");
        }
        return R.ok(product);
    }

    /**
     * 创建商品
     * @param request 商品信息（DTO，仅含允许客户端提交的字段）
     * @return 创建结果
     */
    @Operation(summary = "创建商品", description = "创建新商品，必须指定所属商家")
    @PostMapping
    public R<?> create(@Valid @RequestBody CreateProductRequest request) {
        // 归属必填：项目所有商家归属判定（商家端商品列表、订单归属、商家订单查询、
        // 仪表板统计、下单后给商家推通知）都以 product.merchant_id 为起点。
        // 建出 merchant_id 为 NULL 的商品，它就永远不会出现在任何商家端，
        // 订单也不会通知任何商家——是个查起来很费劲的"隐形商品"。
        R<?> invalid = rejectIfNotMerchant(request.getMerchantId(), true);
        if (invalid != null) {
            return invalid;
        }
        productService.save(toEntity(request));
        return R.ok("创建成功");
    }

    /**
     * 更新商品
     * @param id 商品ID
     * @param request 商品信息（DTO）
     * @return 更新结果
     */
    @Operation(summary = "更新商品", description = "更新商品信息；传 merchantId 可改归属")
    @PutMapping("/{id}")
    public R<?> update(@PathVariable Long id, @Valid @RequestBody CreateProductRequest request) {
        // 归属可不传（不传则保持原归属，MyBatis-Plus 的 updateById 会跳过 null 字段）；
        // 一旦传了就必须是合法商家——顺带也是修复历史无主商品的入口
        R<?> invalid = rejectIfNotMerchant(request.getMerchantId(), false);
        if (invalid != null) {
            return invalid;
        }
        Product product = toEntity(request);
        product.setId(id);
        if (!productService.updateById(product)) {
            return R.error(ResultCode.NOT_FOUND, "商品不存在");
        }
        return R.ok("更新成功");
    }

    /**
     * 校验 merchantId 是否为合法商家。
     *
     * @param merchantId 待校验的商家ID
     * @param required   为 {@code true} 时空值也算不合法（创建场景）
     * @return 校验通过返回 {@code null}；否则返回可直接返回给调用方的错误响应
     */
    private R<?> rejectIfNotMerchant(Long merchantId, boolean required) {
        if (merchantId == null) {
            return required ? R.error(ResultCode.PARAM_ERROR, "请指定商品所属商家") : null;
        }
        User merchant = userService.getById(merchantId);
        if (merchant == null) {
            // 不存在的商家同样拒绝：留着会变成另一批"看不见的商品"
            return R.error(ResultCode.PARAM_ERROR, "指定的商家不存在");
        }
        // 两类失败分开报，管理员才知道是"选错了人"还是"这个人不是商家"
        if (!UserRoleEnum.MERCHANT.getCode().equals(merchant.getRole())) {
            return R.error(ResultCode.PARAM_ERROR,
                    "用户「" + merchant.getUsername() + "」不是商家，不能作为商品归属");
        }
        return null;
    }

    /**
     * DTO → 实体映射
     * <p>
     * 只映射允许客户端提交的业务字段，杜绝通过请求体注入 {@code id} / {@code sales} 等字段（批量赋值漏洞）。
     *
     * <p>{@code merchantId} 是**显式**映射的，不是批量赋值：它的取值已经过
     * {@link #rejectIfNotMerchant} 校验（必须存在且角色为 MERCHANT），
     * 直接信任原始请求体才会写出指向不存在商家的归属。
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
        product.setMerchantId(request.getMerchantId());
        return product;
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
