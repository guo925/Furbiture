package com.gjx.service.Impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.gjx.entity.Category;
import com.gjx.mapper.CategoryMapper;
import com.gjx.service.ICategoryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 分类服务实现类
 */
@Slf4j
@Service
public class CategoryServiceImpl extends ServiceImpl<CategoryMapper, Category> implements ICategoryService {

    @Override
    @Cacheable(value = "categoryTree", key = "'all'", unless = "#result == null || #result.isEmpty()")
    public List<Category> getCategoryTree() {
        // 获取所有分类
        List<Category> allCategories = list();
        
        // 处理空值情况
        if (allCategories == null) {
            return new ArrayList<>();
        }
        
        // 按父ID分组
        Map<Long, List<Category>> categoryMap = allCategories.stream()
                .collect(Collectors.groupingBy(Category::getParentId));
        
        // 构建分类树
        List<Category> rootCategories = new ArrayList<>();
        for (Category category : allCategories) {
            if (category.getParentId() == 0) { // 根分类
                rootCategories.add(category);
                buildCategoryTree(category, categoryMap);
            }
        }
        
        return rootCategories;
    }
    
    /**
     * 递归构建分类树
     */
    /**
     * 清除分类树缓存
     */
    @CacheEvict(value = "categoryTree", key = "'all'")
    public void evictCategoryTreeCache() {
        log.debug("清除分类树缓存");
    }

    // 分类变更会同时影响分类树与商品列表（商品列表内嵌 categoryName），
    // 因此写操作需要同时失效两个缓存，否则商品列表会残留旧分类名。

    /**
     * 覆盖保存方法，自动清除分类树与商品列表缓存
     */
    @Override
    @CacheEvict(value = {"categoryTree", "productList"}, allEntries = true)
    public boolean save(Category entity) {
        return super.save(entity);
    }

    /**
     * 覆盖更新方法，自动清除分类树与商品列表缓存
     */
    @Override
    @CacheEvict(value = {"categoryTree", "productList"}, allEntries = true)
    public boolean updateById(Category entity) {
        return super.updateById(entity);
    }

    /**
     * 覆盖删除方法，自动清除分类树与商品列表缓存
     */
    @Override
    @CacheEvict(value = {"categoryTree", "productList"}, allEntries = true)
    public boolean removeById(java.io.Serializable id) {
        return super.removeById(id);
    }

    private void buildCategoryTree(Category parent, Map<Long, List<Category>> categoryMap) {
        List<Category> children = categoryMap.get(parent.getId());
        if (children != null && !children.isEmpty()) {
            parent.setChildren(children);
            for (Category child : children) {
                buildCategoryTree(child, categoryMap);
            }
        }
    }
}