package com.gjx.service.Impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.gjx.common.BusinessException;
import com.gjx.common.ResultCode;
import com.gjx.entity.Order;
import com.gjx.entity.OrderItem;
import com.gjx.entity.Review;
import com.gjx.enums.OrderStatusEnum;
import com.gjx.mapper.OrderItemMapper;
import com.gjx.mapper.OrderMapper;
import com.gjx.mapper.ReviewMapper;
import com.gjx.service.IReviewService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReviewServiceImpl extends ServiceImpl<ReviewMapper, Review> implements IReviewService {

    /** 评分下限 */
    private static final int MIN_RATING = 1;
    /** 评分上限 */
    private static final int MAX_RATING = 5;

    private final OrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;

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
    @Transactional(rollbackFor = Exception.class)
    public void createReview(Long userId, Long productId, Long orderId, Integer rating, String content, String images) {
        // 1. 评分区间校验
        if (rating == null || rating < MIN_RATING || rating > MAX_RATING) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "评分必须在1-5之间");
        }

        // 2. 评价必须关联订单 —— 无订单即无法证明"买过"
        if (orderId == null) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "评价必须关联订单");
        }

        // 3. 订单必须属于当前用户且状态为「已完成」
        //    归属(userId)与状态(COMPLETED)一并下沉进 WHERE：不存在 / 不属于本人 / 未完成
        //    统一返回同一消息，避免用错误差异探测他人订单；无 userId 则直接拒绝
        if (userId == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "未获取到登录用户");
        }
        Order order = orderMapper.selectOne(new LambdaQueryWrapper<Order>()
                .eq(Order::getId, orderId)
                .eq(Order::getUserId, userId)
                .eq(Order::getStatus, OrderStatusEnum.COMPLETED.getCode()));
        if (order == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "订单不存在或未完成，无法评价");
        }

        // 4. 该订单必须真的包含这个商品（防止用 A 商品的订单评价 B 商品）
        Long itemCount = orderItemMapper.selectCount(new LambdaQueryWrapper<OrderItem>()
                .eq(OrderItem::getOrderId, orderId)
                .eq(OrderItem::getProductId, productId));
        if (itemCount == null || itemCount == 0) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "该订单不包含此商品");
        }

        // 5. 防重复评价：同一 (userId, orderId, productId) 只能评价一次。
        //    并发窗口由数据库唯一键 uk_user_order_product 兜底（migration_v5）。
        long existing = count(new LambdaQueryWrapper<Review>()
                .eq(Review::getUserId, userId)
                .eq(Review::getOrderId, orderId)
                .eq(Review::getProductId, productId));
        if (existing > 0) {
            throw new BusinessException(ResultCode.BUSINESS_ERROR, "该订单商品已评价过");
        }

        Review review = new Review();
        review.setUserId(userId);
        review.setProductId(productId);
        review.setOrderId(orderId);
        review.setRating(rating);
        review.setContent(content);
        review.setImages(images);
        save(review);
        log.info("[创建评价] userId={}, orderId={}, productId={}, rating={}", userId, orderId, productId, rating);
    }
}
