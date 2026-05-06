package com.gjx.service.Impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.gjx.entity.Category;
import com.gjx.entity.Product;
import com.gjx.mapper.ProductMapper;
import com.gjx.service.ICategoryService;
import com.gjx.service.IProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ProductServiceImpl extends ServiceImpl<ProductMapper, Product> implements IProductService {

    @Autowired
    private ICategoryService categoryService;

    @Override
    public boolean decreaseStock(Long productId, Integer quantity) {
        return baseMapper.decreaseStock(productId, quantity) > 0;
    }

    @Override
    public boolean increaseStock(Long productId, Integer quantity) {
        return baseMapper.increaseStock(productId, quantity) > 0;
    }

    @Override
    public Page<Product> listProducts(Long categoryId, String keyword, String sortBy, Integer page, Integer size) {
        LambdaQueryWrapper<Product> queryWrapper = new LambdaQueryWrapper<>();

        queryWrapper.eq(Product::getStatus, 1);

        if (categoryId != null) {
            queryWrapper.eq(Product::getCategoryId, categoryId);
        }

        if (keyword != null && !keyword.isEmpty()) {
            queryWrapper.like(Product::getName, keyword)
                    .or().like(Product::getBrand, keyword)
                    .or().like(Product::getDescription, keyword);
        }

        // 排序逻辑
        if ("price_asc".equals(sortBy)) {
            queryWrapper.orderByAsc(Product::getPrice);
        } else if ("price_desc".equals(sortBy)) {
            queryWrapper.orderByDesc(Product::getPrice);
        } else {
            queryWrapper.orderByDesc(Product::getCreateTime);
        }

        Page<Product> result = page(new Page<>(page, size), queryWrapper);

        fillCategoryNames(result.getRecords());

        return result;
    }

    @Override
    public Page<Product> searchProducts(String keyword, Integer page, Integer size) {
        LambdaQueryWrapper<Product> queryWrapper = new LambdaQueryWrapper<>();

        queryWrapper.eq(Product::getStatus, 1);

        if (keyword != null && !keyword.isEmpty()) {
            queryWrapper.like(Product::getName, keyword)
                    .or().like(Product::getBrand, keyword)
                    .or().like(Product::getDescription, keyword);
        }

        queryWrapper.orderByDesc(Product::getSales)
                .orderByDesc(Product::getCreateTime);

        Page<Product> result = page(new Page<>(page, size), queryWrapper);

        fillCategoryNames(result.getRecords());

        return result;
    }

    @Override
    public Page<Product> adminListProducts(Integer page, Integer size, String name, Integer status) {
        LambdaQueryWrapper<Product> queryWrapper = new LambdaQueryWrapper<>();

        if (name != null && !name.isEmpty()) {
            queryWrapper.like(Product::getName, name);
        }

        if (status != null) {
            queryWrapper.eq(Product::getStatus, status);
        }

        queryWrapper.orderByDesc(Product::getCreateTime);

        Page<Product> result = page(new Page<>(page, size), queryWrapper);

        fillCategoryNames(result.getRecords());

        return result;
    }

    private void fillCategoryNames(java.util.List<Product> products) {
        if (products == null || products.isEmpty()) {
            return;
        }

        java.util.Map<Long, String> categoryCache = new java.util.HashMap<>();

        for (Product product : products) {
            if (product.getCategoryId() != null) {
                String categoryName = categoryCache.get(product.getCategoryId());
                if (categoryName == null) {
                    Category category = categoryService.getById(product.getCategoryId());
                    if (category != null) {
                        categoryName = category.getName();
                        categoryCache.put(product.getCategoryId(), categoryName);
                    }
                }
                product.setCategoryName(categoryName);
            }
        }
    }
}