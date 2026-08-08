package com.gjx.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.gjx.entity.OrderItem;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface OrderItemMapper extends BaseMapper<OrderItem> {

    /**
     * 查询热销商品（按销量降序）
     * <p>
     * 使用 SQL GROUP BY 聚合替代全量加载到内存再排序的方式。
     */
    @Select("SELECT oi.product_id AS id, oi.product_name AS name, oi.price, " +
            "SUM(oi.quantity) AS salesCount " +
            "FROM order_item oi " +
            "GROUP BY oi.product_id, oi.product_name, oi.price " +
            "ORDER BY salesCount DESC " +
            "LIMIT #{limit}")
    List<Map<String, Object>> getHotProducts(@Param("limit") int limit);
}
