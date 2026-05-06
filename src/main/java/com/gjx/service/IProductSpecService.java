package com.gjx.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.gjx.entity.ProductSpec;

import java.util.List;

/**
 * 商品规格服务接口
 */
public interface IProductSpecService extends IService<ProductSpec> {
    /**
     * 根据商品ID获取规格列表
     * @param productId 商品ID
     * @return 规格列表
     */
    List<ProductSpec> listByProductId(Long productId);
    
    /**
     * 根据商品ID获取规格列表（控制器中使用的方法名）
     * @param productId 商品ID
     * @return 规格列表
     */
    List<ProductSpec> getSpecsByProductId(Long productId);
}