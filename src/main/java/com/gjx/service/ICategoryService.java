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
}