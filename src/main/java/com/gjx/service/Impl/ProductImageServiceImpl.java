package com.gjx.service.Impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.gjx.entity.ProductImage;
import com.gjx.mapper.ProductImageMapper;
import com.gjx.service.IProductImageService;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 商品图片服务实现类
 */
@Service
public class ProductImageServiceImpl extends ServiceImpl<ProductImageMapper, ProductImage> implements IProductImageService {

    @Override
    public List<ProductImage> listByProductId(Long productId) {
        LambdaQueryWrapper<ProductImage> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ProductImage::getProductId, productId);
        queryWrapper.orderByAsc(ProductImage::getSortOrder);
        return list(queryWrapper);
    }

    @Override
    public List<ProductImage> getImagesByProductId(Long productId) {
        return listByProductId(productId);
    }
}