package com.gjx.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.gjx.entity.Product;

/**
 * 商品服务接口
 */
public interface IProductService extends IService<Product> {
    /**
     * 扣减库存
     * @param productId 商品ID
     * @param quantity 扣减数量
     * @return 是否成功
     */
    boolean decreaseStock(Long productId, Integer quantity);
    
    /**
     * 增加库存
     * @param productId 商品ID
     * @param quantity 增加数量
     * @return 是否成功
     */
    boolean increaseStock(Long productId, Integer quantity);
    
    /**
     * 获取商品列表
     * @param categoryId 分类ID
     * @param keyword 关键词
     * @param sortBy 排序方式
     * @param page 页码
     * @param size 每页大小
     * @return 商品列表
     */
    Page<Product> listProducts(Long categoryId, String keyword, String sortBy, Integer page, Integer size);
    
    /**
     * 搜索商品
     * @param keyword 关键词
     * @param page 页码
     * @param size 每页大小
     * @return 搜索结果
     */
    Page<Product> searchProducts(String keyword, Integer page, Integer size);
    
    /**
     * 获取商品列表（管理员端）
     * @param page 页码
     * @param size 每页大小
     * @param name 商品名称
     * @param status 商品状态
     * @return 商品列表
     */
    Page<Product> adminListProducts(Integer page, Integer size, String name, Integer status);
}