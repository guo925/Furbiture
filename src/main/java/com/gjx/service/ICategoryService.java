package com.gjx.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.gjx.entity.Category;

import java.util.List;

/**
 * 分类服务接口
 */
public interface ICategoryService extends IService<Category> {
    /**
     * 获取分类树
     * @return 分类树列表
     */
    List<Category> getCategoryTree();

    /**
     * 清除分类树缓存（增删改分类后自动调用）
     */
    void evictCategoryTreeCache();
}