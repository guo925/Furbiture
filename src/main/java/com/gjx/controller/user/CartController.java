package com.gjx.controller.user;

import com.gjx.common.R;
import com.gjx.dto.response.CartItemVO;
import com.gjx.service.ICartService;
import com.gjx.util.AuthenticationUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 购物车控制器
 */
@RestController
@RequestMapping("/api/carts")
@Tag(name = "购物车管理", description = "购物车相关接口")
@RequiredArgsConstructor
public class CartController {

    private final AuthenticationUtil authUtil;

    private final ICartService cartService;

    /**
     * 获取购物车列表
     * @param request HTTP请求
     * @return 购物车条目列表（含商品快照）
     */
    @Operation(summary = "获取购物车列表", description = "获取当前用户的购物车商品列表（含商品快照）")
    @GetMapping
    public R<List<CartItemVO>> list(HttpServletRequest request) {
        Long userId = authUtil.getUserIdFromRequest(request);
        List<CartItemVO> carts = cartService.listByUserId(userId);
        return R.ok(carts);
    }

    /**
     * 添加商品到购物车
     * @param requestDTO 包含productId和quantity
     * @param request HTTP请求
     * @return 添加结果
     */
    @Operation(summary = "添加商品到购物车", description = "将商品添加到购物车")
    @PostMapping
    public R<?> add(@Valid @RequestBody AddToCartRequest requestDTO, HttpServletRequest request) {
        Long userId = authUtil.getUserIdFromRequest(request);
        cartService.addToCart(userId, requestDTO.getProductId(), requestDTO.getQuantity());
        return R.ok("添加成功");
    }

    /**
     * 更新购物车商品数量
     * @param id 购物车ID
     * @param requestDTO 包含quantity
     * @param request HTTP请求
     * @return 更新结果
     */
    @Operation(summary = "更新购物车商品数量", description = "更新购物车中商品的数量")
    @PutMapping("/{id}")
    public R<?> update(@PathVariable Long id, @Valid @RequestBody UpdateCartRequest requestDTO, HttpServletRequest request) {
        // 异常不再在 Controller 内捕获：统一交给 GlobalExceptionHandler，
        // 既保证返回一致，也避免异常被吞掉后没有任何日志
        Long userId = authUtil.getUserIdFromRequest(request);
        cartService.updateCartItem(userId, id, requestDTO.getQuantity());
        return R.ok("更新成功");
    }

    /**
     * 删除购物车商品
     * @param id 购物车ID
     * @param request HTTP请求
     * @return 删除结果
     */
    @Operation(summary = "删除购物车商品", description = "从购物车中删除商品")
    @DeleteMapping("/{id}")
    public R<?> delete(@PathVariable Long id, HttpServletRequest request) {
        Long userId = authUtil.getUserIdFromRequest(request);
        cartService.deleteCartItem(userId, id);
        return R.ok("删除成功");
    }

    /**
     * 清空购物车
     * @param request HTTP请求
     * @return 清空结果
     */
    @Operation(summary = "清空购物车", description = "清空当前用户的购物车")
    @DeleteMapping
    public R<?> clear(HttpServletRequest request) {
        Long userId = authUtil.getUserIdFromRequest(request);
        cartService.clearCart(userId);
        return R.ok("购物车已清空");
    }

    /**
     * 添加到购物车请求
     */
    @Data
    public static class AddToCartRequest {
        @NotNull(message = "商品ID不能为空")
        private Long productId;

        @NotNull(message = "数量不能为空")
        @Min(value = 1, message = "数量至少为1")
        private Integer quantity;
    }

    /**
     * 更新购物车请求
     */
    @Data
    public static class UpdateCartRequest {
        @NotNull(message = "数量不能为空")
        @Min(value = 1, message = "数量至少为1")
        private Integer quantity;
    }
}
