package com.gjx.service.Impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.gjx.common.BusinessException;
import com.gjx.entity.Review;
import com.gjx.mapper.ReviewMapper;
import com.gjx.service.IReviewService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class ReviewServiceImpl extends ServiceImpl<ReviewMapper, Review> implements IReviewService {

    @Override
    public Page<Review> listByProductId(Long productId, Integer page, Integer size) {
        return page(new Page<>(page, size),
                new LambdaQueryWrapper<Review>()
                        .eq(Review::getProductId, productId)
                        .orderByDesc(Review::getCreateTime));
    }

    @Override
    public Double getAvgRating(Long productId) {
        return baseMapper.selectAvgRating(productId);
    }

    @Override
    public Integer getReviewCount(Long productId) {
        return baseMapper.selectReviewCount(productId);
    }

    @Override
    public void createReview(Long userId, Long productId, Long orderId, Integer rating, String content, String images) {
        if (rating == null || rating < 1 || rating > 5) {
            throw new BusinessException("评分必须在1-5之间");
        }
        Review review = new Review();
        review.setUserId(userId);
        review.setProductId(productId);
        review.setOrderId(orderId);
        review.setRating(rating);
        review.setContent(content);
        review.setImages(images);
        save(review);
        log.info("[创建评价] userId={}, productId={}, rating={}", userId, productId, rating);
    }
}
