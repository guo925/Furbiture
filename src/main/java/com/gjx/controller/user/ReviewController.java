package com.gjx.controller.user;

import com.gjx.common.R;
import com.gjx.entity.Review;
import com.gjx.service.IReviewService;
import com.gjx.util.AuthenticationUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 评价控制器
 */
@RestController
@RequestMapping("/api")
@Tag(name = "商品评价", description = "商品评价相关接口")
public class ReviewController {

    @Autowired
    private IReviewService reviewService;

    @Operation(summary = "获取商品评价列表")
    @GetMapping("/products/{productId}/reviews")
    public R<?> listByProduct(@PathVariable Long productId,
                               @RequestParam(defaultValue = "1") Integer page,
                               @RequestParam(defaultValue = "10") Integer size) {
        return R.ok(reviewService.listByProductId(productId, page, size));
    }

    @Operation(summary = "获取商品评价统计")
    @GetMapping("/products/{productId}/review-stats")
    public R<?> getReviewStats(@PathVariable Long productId) {
        Double avgRating = reviewService.getAvgRating(productId);
        Integer count = reviewService.getReviewCount(productId);
        Map<String, Object> stats = Map.of("avgRating", avgRating, "count", count);
        return R.ok(stats);
    }

    @Operation(summary = "创建评价")
    @PostMapping("/reviews")
    public R<?> createReview(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        Long userId = AuthenticationUtil.getUserIdFromRequest(request);
        Long productId = Long.valueOf(body.get("productId").toString());
        Long orderId = body.get("orderId") != null ? Long.valueOf(body.get("orderId").toString()) : null;
        Integer rating = Integer.valueOf(body.get("rating").toString());
        String content = (String) body.get("content");
        String images = (String) body.get("images");

        reviewService.createReview(userId, productId, orderId, rating, content, images);
        return R.ok("评价成功");
    }
}
