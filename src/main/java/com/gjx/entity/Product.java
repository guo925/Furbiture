package com.gjx.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("product")
public class Product {
    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;

    private Long categoryId;

    @TableField(exist = false)
    private String categoryName;

    private String brand;

    private String mainImage;

    private BigDecimal price;

    private Integer stock;

    private Integer status;

    private Integer sales;

    private String description;

    private Long merchantId;

    @TableField(exist = false)
    private String merchantName;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    /**
     * 逻辑删除标志：0-未删除，1-已删除。
     *
     * <p><b>为什么必须显式写 {@code @TableLogic}：</b>
     * {@code application.yml} 配了 {@code logic-delete-field: deleted}，但实体若没有同名字段，
     * 全局配置会<b>静默失效</b>，{@code removeById} 会退化成物理 {@code DELETE}（无报错、无日志）。
     * 显式标注后，商家/管理员「删除商品」只是置 {@code deleted = 1}，商品行保留可追溯。
     *
     * <p>{@code @TableField(select = false)} 同 {@code User.deleted}：让该列不进入查询结果，
     * 同时不影响逻辑删除所需的 {@code AND deleted = 0} 过滤条件。
     *
     * <p><b>已知副作用（可接受）：</b>
     * 商品被软删除后，{@code cart} / {@code favorite} 里可能残留指向它的引用行。
     * 这些引用行不会被级联清理，但读取路径早已按「商品可能已被删除」兜底：
     * {@code CartServiceImpl.listByUserId} 批量查商品时查不到该行 →
     * {@code productMap.get(...)} 返回 null → {@code ProductBriefVO.from(null)} 返回 null →
     * 前端按「商品已下架」展示，整条购物车列表不会因此报错。
     * 由于逻辑删除的 {@code deleted = 0} 过滤条件，软删除商品在批量查询里同样查不到，
     * 因此其表现与原物理删除<b>一致</b>，不产生新的空指针或异常。
     */
    @TableLogic
    @TableField(select = false)
    private Integer deleted;
}