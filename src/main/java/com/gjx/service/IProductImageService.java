package com.gjx.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.gjx.entity.ProductImage;

import java.util.List;

/**
 * 商品图片服务接口
 */
public interface IProductImageService extends IService<ProductImage> {
    /**
     * 根据商品ID获取图片列表
     * @param productId 商品ID
     * @return 图片列表
     */
    List<ProductImage> listByProductId(Long productId);
    
    /**
     * 根据商品ID获取图片列表（控制器中使用的方法名）
     * @param productId 商品ID
     * @return 图片列表
     */
    List<ProductImage> getImagesByProductId(Long productId);
}