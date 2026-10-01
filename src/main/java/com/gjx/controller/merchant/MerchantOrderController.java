package com.gjx.controller.merchant;

import com.gjx.common.R;
import com.gjx.common.ResultCode;
import com.gjx.dto.request.OrderStatusRequest;
import com.gjx.enums.OrderStatusEnum;
import com.gjx.service.IMerchantOrderService;
import com.gjx.service.IOrderService;
import com.gjx.service.IUserService;
import com.gjx.util.AuthenticationUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 商家订单管理控制器。
 *
 * <p>分层：Controller 只负责请求接收、参数校验与转发；「按商家商品筛订单」「组装仅含本商家
 * 订单项的详情」等查询编排与归属判定已下沉到 {@link IMerchantOrderService}（见该类 javadoc）。
 * 此前本类直接注入 {@code OrderMapper} / {@code OrderItemMapper} 违反了
 * 「Controller 不得直接依赖 Mapper」的分层红线。
 */
@Slf4j
@RestController
@RequestMapping("/api/merchant")
@Tag(name = "商家订单管理", description = "商家订单查询和状态更新接口")
@RequiredArgsConstructor
public class MerchantOrderController {

    private final AuthenticationUtil authUtil;

    private final IUserService userService;

    private final IOrderService orderService;

    private final IMerchantOrderService merchantOrderService;

    @Operation(summary = "获取订单列表", description = "获取包含商家商品的订单分页列表")
    @GetMapping("/orders")
    public R<?> getOrders(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) Integer status,
            HttpServletRequest request) {
        Long merchantId = getMerchantId(request);
        return R.ok(merchantOrderService.pageMerchantOrders(merchantId, page, size, status));
    }

    @Operation(summary = "获取订单详情", description = "获取订单详细信息（仅返回商家自己的商品项）")
    @GetMapping("/orders/{orderNo}")
    public R<Map<String, Object>> getOrderDetail(@PathVariable String orderNo, HttpServletRequest request) {
        Long merchantId = getMerchantId(request);
        // 订单不存在或不属于本商家时，Service 返回 empty，统一按「订单不存在」提示，避免探测他人订单
        return merchantOrderService.getOrderDetail(merchantId, orderNo)
                .<R<Map<String, Object>>>map(R::ok)
                .orElseGet(() -> R.error(ResultCode.NOT_FOUND, "订单不存在"));
    }

    @Operation(summary = "更新订单状态", description = "商家更新订单状态（当前仅支持发货）")
    @PutMapping("/orders/{orderNo}/status")
    public R<?> updateOrderStatus(@PathVariable String orderNo, @Valid @RequestBody OrderStatusRequest statusRequest,
                                  HttpServletRequest request) {
        Long merchantId = getMerchantId(request);
        Integer newStatus = statusRequest.getStatus();

        // 状态白名单：商家仅可执行"发货"，其余流转由用户（确认收货）或管理员负责，
        // 避免商家自行把订单改成已完成/已退款等状态
        if (!OrderStatusEnum.DELIVERED.getCode().equals(newStatus)) {
            return R.error(ResultCode.FORBIDDEN, "商家仅可执行发货操作");
        }

        // 归属校验与状态流转在 Service 层统一处理（订单必须含本商家商品，且当前为已付款）
        orderService.deliverOrderByMerchant(orderNo, merchantId);
        log.info("[商家更新订单状态] orderNo={}, newStatus={}, merchantId={}", orderNo, newStatus, merchantId);
        return R.ok("更新成功");
    }

    private Long getMerchantId(HttpServletRequest request) {
        return userService.findByUsername(authUtil.getUsernameFromRequest(request)).getId();
    }
}
