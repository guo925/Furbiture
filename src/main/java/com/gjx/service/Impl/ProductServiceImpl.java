package com.gjx.service.Impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.gjx.entity.Category;
import com.gjx.entity.Product;
import com.gjx.mapper.ProductMapper;
import com.gjx.service.ICategoryService;
import com.gjx.service.IProductService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Slf4j
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
    @Cacheable(value = "productList", key = "'list_' + #categoryId + '_' + #keyword + '_' + #sortBy + '_' + #page + '_' + #size", unless = "#result == null || #result.records.isEmpty()")
    public Page<Product> listProducts(Long categoryId, String keyword, String sortBy, Integer page, Integer size) {
        LambdaQueryWrapper<Product> queryWrapper = new LambdaQueryWrapper<>();

        queryWrapper.eq(Product::getStatus, 1);

        if (categoryId != null) {
            queryWrapper.eq(Product::getCategoryId, categoryId);
        }

        if (keyword != null && !keyword.isEmpty()) {
            queryWrapper.and(wrapper -> wrapper
                    .like(Product::getName, keyword)
                    .or().like(Product::getBrand, keyword)
                    .or().like(Product::getDescription, keyword));
        }

        // 排序逻辑
        if ("price_asc".equals(sortBy)) {
            queryWrapper.orderByAsc(Product::getPrice);
        } else if ("price_desc".equals(sortBy)) {
            queryWrapper.orderByDesc(Product::getPrice);
        } else if ("sales_desc".equals(sortBy)) {
            queryWrapper.orderByDesc(Product::getSales)
                    .orderByDesc(Product::getCreateTime);
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
            queryWrapper.and(wrapper -> wrapper
                    .like(Product::getName, keyword)
                    .or().like(Product::getBrand, keyword)
                    .or().like(Product::getDescription, keyword));
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

    /**
     * 批量填充商品分类名称
     * <p>
     * 优化：先收集所有 categoryId，一次批量查询，再映射回各商品，
     * 避免每个商品单独查询一次数据库（N+1 问题）。
     */
    private void fillCategoryNames(java.util.List<Product> products) {
        if (products == null || products.isEmpty()) {
            return;
        }

        // 收集所有非空 categoryId
        java.util.Set<Long> categoryIds = products.stream()
                .map(Product::getCategoryId)
                .filter(id -> id != null)
                .collect(java.util.stream.Collectors.toSet());

        if (categoryIds.isEmpty()) {
            return;
        }

        // 一次批量查询所有分类
        java.util.Map<Long, String> nameMap = categoryService.listByIds(new java.util.ArrayList<>(categoryIds))
                .stream()
                .collect(java.util.stream.Collectors.toMap(Category::getId, Category::getName, (a, b) -> a));

        // 映射分类名到商品
        for (Product product : products) {
            if (product.getCategoryId() != null) {
                product.setCategoryName(nameMap.get(product.getCategoryId()));
            }
        }
    }
}
