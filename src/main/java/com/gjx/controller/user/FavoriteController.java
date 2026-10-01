package com.gjx.controller.user;

import com.gjx.common.R;
import com.gjx.entity.Favorite;
import com.gjx.service.IFavoriteService;
import com.gjx.util.AuthenticationUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 收藏控制器
 */
@RestController
@RequestMapping("/api/favorites")
@Tag(name = "商品收藏", description = "商品收藏相关接口")
@RequiredArgsConstructor
public class FavoriteController {

    private final AuthenticationUtil authUtil;

    private final IFavoriteService favoriteService;

    @Operation(summary = "切换收藏状态")
    @PostMapping("/{productId}")
    public R<?> toggleFavorite(@PathVariable Long productId, HttpServletRequest request) {
        Long userId = authUtil.getUserIdFromRequest(request);
        boolean favorited = favoriteService.toggleFavorite(userId, productId);
        return R.ok(favorited ? "已收藏" : "已取消收藏");
    }

    @Operation(summary = "检查是否已收藏")
    @GetMapping("/check/{productId}")
    public R<?> checkFavorite(@PathVariable Long productId, HttpServletRequest request) {
        Long userId = authUtil.getUserIdFromRequest(request);
        boolean favorited = favoriteService.isFavorited(userId, productId);
        return R.ok(favorited);
    }

    @Operation(summary = "获取收藏列表")
    @GetMapping
    public R<List<Favorite>> listFavorites(HttpServletRequest request) {
        Long userId = authUtil.getUserIdFromRequest(request);
        return R.ok(favoriteService.listByUserId(userId));
    }
}
