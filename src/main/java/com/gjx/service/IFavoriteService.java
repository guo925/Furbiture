package com.gjx.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.gjx.entity.Favorite;

import java.util.List;

public interface IFavoriteService extends IService<Favorite> {

    /** 切换收藏状态（收藏/取消收藏） */
    boolean toggleFavorite(Long userId, Long productId);

    /** 是否已收藏 */
    boolean isFavorited(Long userId, Long productId);

    /** 获取用户收藏列表 */
    List<Favorite> listByUserId(Long userId);
}
