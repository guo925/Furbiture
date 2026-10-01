package com.gjx.service.Impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.gjx.common.BusinessException;
import com.gjx.dto.response.CartItemVO;
import com.gjx.entity.Cart;
import com.gjx.entity.Product;
import com.gjx.mapper.CartMapper;
import com.gjx.mapper.ProductMapper;
import com.gjx.service.ICartService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 购物车服务实现类
 */
@Service
@RequiredArgsConstructor
public class CartServiceImpl extends ServiceImpl<CartMapper, Cart> implements ICartService {

    private final ProductMapper productMapper;

    @Override
    public List<CartItemVO> listByUserId(Long userId) {
        // 归属条件下沉进 WHERE：只查当前用户自己的购物车行
        LambdaQueryWrapper<Cart> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Cart::getUserId, userId);
        queryWrapper.orderByDesc(Cart::getCreateTime);
        List<Cart> carts = list(queryWrapper);

        // 空购物车直接返回，省掉一次无意义的商品查询
        if (carts.isEmpty()) {
            return Collections.emptyList();
        }

        // 一次批量查商品（而非在循环里逐条查），这是消灭 N+1 的关键：
        // 20 件购物车从「1(购物车) + 20(商品)」次查询降为「1 + 1」次
        Set<Long> productIds = carts.stream()
                .map(Cart::getProductId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, Product> productMap = productIds.isEmpty()
                ? Collections.emptyMap()
                : productMapper.selectBatchIds(productIds).stream()
                        .collect(Collectors.toMap(Product::getId, Function.identity()));

        // 商品可能已被删除（批量查不到）——此时 product 为 null，
        // 前端按「商品已下架」兜底展示，而不是让整个购物车列表报错
        return carts.stream()
                .map(cart -> CartItemVO.of(cart, productMap.get(cart.getProductId())))
                .collect(Collectors.toList());
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
