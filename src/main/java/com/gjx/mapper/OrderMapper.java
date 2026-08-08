package com.gjx.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.gjx.entity.Order;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.math.BigDecimal;

@Mapper
public interface OrderMapper extends BaseMapper<Order> {

    /**
     * 查询指定状态订单的总销售额（SQL SUM 聚合，避免全量加载）
     */
    @Select("SELECT COALESCE(SUM(total_amount), 0) FROM `order` WHERE status = #{status}")
    BigDecimal selectTotalSalesByStatus(@Param("status") int status);

    /**
     * 查询指定日期和状态订单的总销售额
     */
    @Select("SELECT COALESCE(SUM(total_amount), 0) FROM `order` " +
            "WHERE pay_time >= #{startDate} AND pay_time < #{endDate} AND status = #{status}")
    BigDecimal selectTotalSalesByDateAndStatus(@Param("startDate") String startDate,
                                                @Param("endDate") String endDate,
                                                @Param("status") int status);

    /**
     * 查询指定日期的订单数量
     */
    @Select("SELECT COUNT(*) FROM `order` " +
            "WHERE create_time >= #{startDate} AND create_time < #{endDate}")
    int countOrdersByDate(@Param("startDate") String startDate,
                          @Param("endDate") String endDate);

    /**
     * 查询今日订单数量
     */
    @Select("SELECT COUNT(*) FROM `order` WHERE DATE(create_time) = CURDATE()")
    int countTodayOrders();
}
