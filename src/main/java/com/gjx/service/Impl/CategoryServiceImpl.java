package com.gjx.service.Impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.gjx.entity.Category;
import com.gjx.mapper.CategoryMapper;
import com.gjx.service.ICategoryService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 分类服务实现类
 */
@Service
public class CategoryServiceImpl extends ServiceImpl<CategoryMapper, Category> implements ICategoryService {

    @Override
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