package com.gjx.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.gjx.entity.Review;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface ReviewMapper extends BaseMapper<Review> {

    /** 查询商品平均评分 */
    @Select("SELECT COALESCE(AVG(rating), 0) FROM review WHERE product_id = #{productId}")
    Double selectAvgRating(@Param("productId") Long productId);

    /** 查询商品评价数量 */
    @Select("SELECT COUNT(*) FROM review WHERE product_id = #{productId}")
    Integer selectReviewCount(@Param("productId") Long productId);
}
