package com.gjx.service.Impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.gjx.common.BusinessException;
import com.gjx.entity.Favorite;
import com.gjx.entity.Product;
import com.gjx.mapper.FavoriteMapper;
import com.gjx.service.IFavoriteService;
import com.gjx.service.IProductService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
public class FavoriteServiceImpl extends ServiceImpl<FavoriteMapper, Favorite> implements IFavoriteService {

    @Autowired
    private IProductService productService;

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
        save(favorite);
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
        // 填充商品信息
        for (Favorite fav : favorites) {
            Product product = productService.getById(fav.getProductId());
            if (product != null) {
                fav.setProductName(product.getName());
                fav.setProductImage(product.getMainImage());
                fav.setProductPrice(product.getPrice());
            }
        }
        return favorites;
    }
}
