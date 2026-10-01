package com.gjx.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.gjx.entity.Order;

import java.util.Map;
import java.util.Optional;

/**
 * 商家订单服务接口。
 *
 * <p><b>为什么需要这一层（分层理由）：</b>此前「筛出本商家商品 → 反查订单号 → 组装订单/明细」
 * 的查询编排直接写在 {@link com.gjx.controller.merchant.MerchantOrderController} 里，
 * 该 Controller 因此注入了 {@code OrderMapper}、{@code OrderItemMapper}，
 * 违反分层红线「Controller 只做请求接收、参数校验与转发，数据库访问必经 Service」。
 * 这里的归属判定（订单必须包含本商家商品，否则视为不存在）、分页与排序规则都属于业务逻辑，
 * 收口到 Service 后：Controller 只保留「取当前商家 ID → 调本接口 → 包装 {@code R}」三件事，
 * 数据访问被约束在 controller → service → mapper 的单向依赖内，也便于脱离 Web 层做单元测试。
 */
public interface IMerchantOrderService {

    /**
     * 分页查询「包含本商家商品」的订单列表。
     *
     * <p>归属口径：本商家的商品（{@code product.merchant_id}）→ 命中这些商品的订单项
     * （{@code order_item.product_id}）→ 这些订单项所属的订单。商家商品为空或其订单为空时，
     * 直接返回一个空 {@link Page}（不产生额外查询）。
     *
     * @param merchantId 商家 ID
     * @param page       页码（从 1 开始）
     * @param size       每页条数
     * @param status     订单状态过滤，可为 null（不过滤）
     * @return 订单分页结果，按创建时间倒序
     */
    Page<Order> pageMerchantOrders(Long merchantId, Integer page, Integer size, Integer status);

    /**
     * 查询订单详情，仅返回属于本商家的订单项。
     *
     * <p>归属校验：订单不含本商家商品时，与「订单不存在」返回同一结果，
     * 避免商家借订单号探测、查看他人订单。
     *
     * @param merchantId 商家 ID
     * @param orderNo    订单号
     * @return 命中时返回含 {@code order} 与 {@code orderItems} 的结果；
     *         订单不存在、或不含本商家商品时返回 {@link Optional#empty()}
     */
    Optional<Map<String, Object>> getOrderDetail(Long merchantId, String orderNo);
}
