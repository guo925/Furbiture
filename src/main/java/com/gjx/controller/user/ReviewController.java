package com.gjx.controller.user;

import com.gjx.common.R;
import com.gjx.dto.request.CreateReviewRequest;
import com.gjx.entity.Review;
import com.gjx.service.IReviewService;
import com.gjx.util.AuthenticationUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 评价控制器
 */
@RestController
@RequestMapping("/api")
@Tag(name = "商品评价", description = "商品评价相关接口")
@RequiredArgsConstructor
public class ReviewController {

    private final AuthenticationUtil authUtil;

    private final IReviewService reviewService;

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
    public R<?> createReview(@Valid @RequestBody CreateReviewRequest reviewRequest, HttpServletRequest request) {
        // 旧写法用 Map 接参并对 productId / rating 调 .toString()，缺字段直接 NPE；
        // 改为 DTO + @Valid 后由统一异常处理器返回明确的参数错误
        Long userId = authUtil.getUserIdFromRequest(request);
        reviewService.createReview(userId, reviewRequest.getProductId(), reviewRequest.getOrderId(),
                reviewRequest.getRating(), reviewRequest.getContent(), reviewRequest.getImages());
        return R.ok("评价成功");
    }
}
