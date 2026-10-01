package com.gjx.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.gjx.entity.Product;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface ProductMapper extends BaseMapper<Product> {

    // 扣减库存（乐观锁，实际开发可加版本号）
    @Update("UPDATE product SET stock = stock - #{quantity}, sales = sales + #{quantity} WHERE id = #{id} AND stock >= #{quantity}")
    int decreaseStock(@Param("id") Long id, @Param("quantity") Integer quantity);

    // 增加库存（仅补库存，不回冲销量，如管理员补货）
    @Update("UPDATE product SET stock = stock + #{quantity} WHERE id = #{id}")
    int increaseStock(@Param("id") Long id, @Param("quantity") Integer quantity);

    // 恢复库存并回冲销量（取消/退款时使用）
    // 条件下沉进 WHERE：sales < quantity 时影响行数为 0，由调用方判断并降级处理，
    // 避免 sales 被减成负数（否则热销榜/按销量排序会失真）
    @Update("UPDATE product SET stock = stock + #{quantity}, sales = sales - #{quantity} "
            + "WHERE id = #{id} AND sales >= #{quantity}")
    int restoreStockAndSales(@Param("id") Long id, @Param("quantity") Integer quantity);

    // 查询并锁定商品行（悲观锁，用于下单事务）
    @Select("SELECT * FROM product WHERE id = #{id} FOR UPDATE")
    Product selectForUpdate(@Param("id") Long id);
}