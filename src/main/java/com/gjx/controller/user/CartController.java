package com.gjx.controller.user;

import com.gjx.common.BusinessException;
import com.gjx.common.R;
import com.gjx.entity.Cart;
import com.gjx.service.ICartService;
import com.gjx.util.AuthenticationUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 购物车控制器
 */
@RestController
@RequestMapping("/api/carts")
@Tag(name = "购物车管理", description = "购物车相关接口")
public class CartController {

    @Autowired
    private ICartService cartService;

    /**
     * 获取购物车列表
     * @param request HTTP请求
     * @return 购物车列表
     */
    @Operation(summary = "获取购物车列表", description = "获取当前用户的购物车商品列表")
    @GetMapping
    public R<List<Cart>> list(HttpServletRequest request) {
        Long userId = AuthenticationUtil.getUserIdFromRequest(request);
        List<Cart> carts = cartService.listByUserId(userId);
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
    public R<?> add(@RequestBody AddToCartRequest requestDTO, HttpServletRequest request) {
        Long userId = AuthenticationUtil.getUserIdFromRequest(request);
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
    public R<?> update(@PathVariable Long id, @RequestBody UpdateCartRequest requestDTO, HttpServletRequest request) {
        try {
            Long userId = AuthenticationUtil.getUserIdFromRequest(request);
            if (userId == null) {
                return R.error("用户未登录");
            }
            cartService.updateCartItem(userId, id, requestDTO.getQuantity());
            return R.ok("更新成功");
        } catch (BusinessException e) {
            return R.error(e.getMessage());
        } catch (Exception e) {
            return R.error("更新失败");
        }
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
        try {
            Long userId = AuthenticationUtil.getUserIdFromRequest(request);
            if (userId == null) {
                return R.error("用户未登录");
            }
            cartService.deleteCartItem(userId, id);
            return R.ok("删除成功");
        } catch (BusinessException e) {
            return R.error(e.getMessage());
        } catch (Exception e) {
            return R.error("删除失败");
        }
    }

    /**
     * 清空购物车
     * @param request HTTP请求
     * @return 清空结果
     */
    @Operation(summary = "清空购物车", description = "清空当前用户的购物车")
    @DeleteMapping
    public R<?> clear(HttpServletRequest request) {
        Long userId = AuthenticationUtil.getUserIdFromRequest(request);
        cartService.clearCart(userId);
        return R.ok("购物车已清空");
    }

    /**
     * 添加到购物车请求
     */
    public static class AddToCartRequest {
        private Long productId;
        private Integer quantity;

        public Long getProductId() { return productId; }
        public void setProductId(Long productId) { this.productId = productId; }
        public Integer getQuantity() { return quantity; }
        public void setQuantity(Integer quantity) { this.quantity = quantity; }
    }

    /**
     * 更新购物车请求
     */
    public static class UpdateCartRequest {
        private Integer quantity;

        public Integer getQuantity() { return quantity; }
        public void setQuantity(Integer quantity) { this.quantity = quantity; }
    }
}