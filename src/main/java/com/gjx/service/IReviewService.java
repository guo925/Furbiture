package com.gjx.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.gjx.entity.Review;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

public interface IReviewService extends IService<Review> {

    /** 分页查询商品评价 */
    Page<Review> listByProductId(Long productId, Integer page, Integer size);

    /** 查询商品平均评分 */
    Double getAvgRating(Long productId);

    /** 查询商品评价数量 */
    Integer getReviewCount(Long productId);

    /** 创建评价 */
    void createReview(Long userId, Long productId, Long orderId, Integer rating, String content, String images);
}
