package com.gjx.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.gjx.entity.Cart;

import java.util.List;

/**
 * 购物车服务接口
 */
public interface ICartService extends IService<Cart> {
    /**
     * 根据用户ID获取购物车列表
     * @param userId 用户ID
     * @return 购物车列表
     */
    List<Cart> listByUserId(Long userId);
    
    /**
     * 添加商品到购物车
     * @param userId 用户ID
     * @param productId 商品ID
     * @param quantity 数量
     */
    void addToCart(Long userId, Long productId, Integer quantity);
    
    /**
     * 更新购物车商品数量
     * @param userId 用户ID
     * @param cartItemId 购物车商品ID
     * @param quantity 数量
     */
    void updateCartItem(Long userId, Long cartItemId, Integer quantity);
    
    /**
     * 删除购物车商品
     * @param userId 用户ID
     * @param cartItemId 购物车商品ID
     */
    void deleteCartItem(Long userId, Long cartItemId);
    
    /**
     * 清空购物车
     * @param userId 用户ID
     */
    void clearCart(Long userId);
}