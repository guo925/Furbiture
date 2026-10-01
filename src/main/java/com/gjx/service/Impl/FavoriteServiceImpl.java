package com.gjx.service.Impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.gjx.common.BusinessException;
import com.gjx.entity.Favorite;
import com.gjx.entity.Product;
import com.gjx.mapper.FavoriteMapper;
import com.gjx.service.IFavoriteService;
import com.gjx.service.IProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class FavoriteServiceImpl extends ServiceImpl<FavoriteMapper, Favorite> implements IFavoriteService {

    private final IProductService productService;

    @Override
    @Transactional
    public boolean toggleFavorite(Long userId, Long productId) {
        Product product = productService.getById(productId);
        if (product == null) {
            throw new BusinessException("商品不存在");
        }
        Favorite existing = getOne(new LambdaQueryWrapper<Favorite>()
                .eq(Favorite::getUserId, userId)
                .eq(Favorite::getProductId, productId));
        if (existing != null) {
            removeById(existing.getId());
            log.info("[取消收藏] userId={}, productId={}", userId, productId);
            return false; // 已取消
        }
        Favorite favorite = new Favorite();
        favorite.setUserId(userId);
        favorite.setProductId(productId);
        try {
            save(favorite);
        } catch (DuplicateKeyException e) {
            // 并发双击：两个请求都通过了上面的"不存在"判断，
            // 先到者插入成功，后到者撞 favorite 的 UNIQUE(user_id, product_id)。
            // 目标状态（已收藏）已达成，幂等返回，不向上抛 500。
            log.info("[重复收藏] 并发已存在，幂等返回 userId={}, productId={}", userId, productId);
            return true;
        }
        log.info("[添加收藏] userId={}, productId={}", userId, productId);
        return true; // 已收藏
    }

    @Override
    public boolean isFavorited(Long userId, Long productId) {
        return count(new LambdaQueryWrapper<Favorite>()
                .eq(Favorite::getUserId, userId)
                .eq(Favorite::getProductId, productId)) > 0;
    }

    @Override
    public List<Favorite> listByUserId(Long userId) {
        List<Favorite> favorites = list(new LambdaQueryWrapper<Favorite>()
                .eq(Favorite::getUserId, userId)
                .orderByDesc(Favorite::getCreateTime));
        if (favorites.isEmpty()) {
            return favorites;
        }
        // 一次性批量查商品，避免对每个收藏各查一次（N+1）
        List<Long> productIds = favorites.stream()
                .map(Favorite::getProductId)
                .distinct()
                .collect(Collectors.toList());
        Map<Long, Product> productMap = productService.listByIds(productIds).stream()
                .collect(Collectors.toMap(Product::getId, Function.identity(), (a, b) -> a));
        // 填充商品信息
        for (Favorite fav : favorites) {
            Product product = productMap.get(fav.getProductId());
            if (product != null) {
                fav.setProductName(product.getName());
                fav.setProductImage(product.getMainImage());
                fav.setProductPrice(product.getPrice());
            }
        }
        return favorites;
    }
}
