package com.gjx.service.Impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.gjx.common.BusinessException;
import com.gjx.entity.Cart;
import com.gjx.entity.Product;
import com.gjx.mapper.CartMapper;
import com.gjx.mapper.ProductMapper;
import com.gjx.service.ICartService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 购物车服务实现类
 */
@Service
public class CartServiceImpl extends ServiceImpl<CartMapper, Cart> implements ICartService {

    @Autowired
    private ProductMapper productMapper;

    @Override
    public List<Cart> listByUserId(Long userId) {
        LambdaQueryWrapper<Cart> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Cart::getUserId, userId);
        queryWrapper.orderByDesc(Cart::getCreateTime);
        return list(queryWrapper);
    }

    @Override
    public void addToCart(Long userId, Long productId, Integer quantity) {
        // 数量必须为正整数。若允许负数，下单时 `stock >= quantity` 恒为真，
        // 而扣减 SQL 是 `stock = stock - quantity`，负数量会变成反向增加库存（可被刷库存）
        if (quantity == null || quantity <= 0) {
            throw new BusinessException("商品数量必须大于 0");
        }

        // 验证商品是否存在
        Product product = productMapper.selectById(productId);
        if (product == null) {
            throw new BusinessException("商品不存在");
        }
        if (product.getStatus() != 1) {
            throw new BusinessException("商品已下架");
        }
        if (product.getStock() < quantity) {
            throw new BusinessException("商品库存不足");
        }

        // 检查购物车中是否已存在该商品
        LambdaQueryWrapper<Cart> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Cart::getUserId, userId);
        queryWrapper.eq(Cart::getProductId, productId);
        Cart existingCart = getOne(queryWrapper);

        if (existingCart != null) {
            // 已存在，更新数量
            existingCart.setQuantity(existingCart.getQuantity() + quantity);
            updateById(existingCart);
        } else {
            // 不存在，添加新商品
            Cart cart = new Cart();
            cart.setUserId(userId);
            cart.setProductId(productId);
            cart.setQuantity(quantity);
            save(cart);
        }
    }

    @Override
    public void updateCartItem(Long userId, Long cartItemId, Integer quantity) {
        // 同上：负数量会导致下单时反向增加库存
        if (quantity == null || quantity <= 0) {
            throw new BusinessException("商品数量必须大于 0");
        }

        // 验证购物车商品是否存在且属于当前用户
        Cart cart = getById(cartItemId);
        if (cart == null) {
            throw new BusinessException("购物车商品不存在");
        }
        if (!cart.getUserId().equals(userId)) {
            throw new BusinessException("购物车商品不存在");
        }

        // 验证商品库存
        Product product = productMapper.selectById(cart.getProductId());
        if (product == null) {
            throw new BusinessException("商品不存在");
        }
        if (product.getStock() < quantity) {
            throw new BusinessException("商品库存不足");
        }

        // 更新数量
        cart.setQuantity(quantity);
        updateById(cart);
    }

    @Override
    public void deleteCartItem(Long userId, Long cartItemId) {
        // 验证购物车商品是否存在且属于当前用户
        Cart cart = getById(cartItemId);
        if (cart == null) {
            throw new BusinessException("购物车商品不存在");
        }
        if (!cart.getUserId().equals(userId)) {
            throw new BusinessException("购物车商品不存在");
        }

        removeById(cartItemId);
    }

    @Override
    public void clearCart(Long userId) {
        LambdaQueryWrapper<Cart> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Cart::getUserId, userId);
        remove(queryWrapper);
    }
}