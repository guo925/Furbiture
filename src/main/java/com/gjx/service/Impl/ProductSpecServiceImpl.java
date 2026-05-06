package com.gjx.service.Impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.gjx.entity.ProductSpec;
import com.gjx.mapper.ProductSpecMapper;
import com.gjx.service.IProductSpecService;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 商品规格服务实现类
 */
@Service
public class ProductSpecServiceImpl extends ServiceImpl<ProductSpecMapper, ProductSpec> implements IProductSpecService {

    @Override
    public List<ProductSpec> listByProductId(Long productId) {
        LambdaQueryWrapper<ProductSpec> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ProductSpec::getProductId, productId);
        return list(queryWrapper);
    }

    @Override
    public List<ProductSpec> getSpecsByProductId(Long productId) {
        return listByProductId(productId);
    }
}