package com.gjx.dto.response;

import com.gjx.entity.Cart;
import com.gjx.entity.Product;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 购物车条目 VO —— 一次返回「购物车行 + 商品快照」，消灭前端逐条补查商品的 N+1。
 * <p>
 * 字段名与前端既有拼装形状（{@code { ...cart, product: { ...product } }}）保持一致：
 * 顶层是购物车行本身的字段（id / productId / quantity），内嵌 {@code product} 是商品快照。
 * 因此前端只需删除「逐条补查商品」的补丁逻辑，其余渲染代码零改动。
 */
@Data
@Builder
public class CartItemVO {

    /** 购物车行 ID */
    private Long id;

    /** 商品 ID */
    private Long productId;

    /** 购买数量 */
    private Integer quantity;

    /** 商品快照；商品已被删除时为 null（前端按「商品已下架」兜底展示） */
    private ProductBriefVO product;

    /**
     * 购物车列表展示所需的商品最小字段集。
     * <p>
     * 字段是前端模板实际读取的那些（name / mainImage / price / brand / categoryName），
     * 不是整个 Product 实体 —— 避免把 description、成本等无关字段带到列表接口。
     */
    @Data
    @Builder
    public static class ProductBriefVO {

        private Long id;

        private String name;

        private String brand;

        private String categoryName;

        private String mainImage;

        private BigDecimal price;

        private Integer stock;

        private Integer status;

        /**
         * 从 Product 实体构建商品快照。
         *
         * @param product 商品实体；为 null 表示商品已被删除，返回 null 由调用方兜底
         */
        public static ProductBriefVO from(Product product) {
            if (product == null) {
                return null;
            }
            return ProductBriefVO.builder()
                    .id(product.getId())
                    .name(product.getName())
                    .brand(product.getBrand())
                    .categoryName(product.getCategoryName())
                    .mainImage(product.getMainImage())
                    .price(product.getPrice())
                    .stock(product.getStock())
                    .status(product.getStatus())
                    .build();
        }
    }

    /**
     * 由购物车行 + 商品实体组装条目 VO。
     *
     * @param cart    购物车行
     * @param product 关联商品；为 null 表示商品已被删除
     */
    public static CartItemVO of(Cart cart, Product product) {
        return CartItemVO.builder()
                .id(cart.getId())
                .productId(cart.getProductId())
                .quantity(cart.getQuantity())
                .product(ProductBriefVO.from(product))
                .build();
    }
}
