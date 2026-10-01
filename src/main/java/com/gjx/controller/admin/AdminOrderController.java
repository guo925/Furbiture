package com.gjx.controller.admin;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.gjx.common.R;
import com.gjx.common.ResultCode;
import com.gjx.dto.request.OrderStatusRequest;
import com.gjx.entity.Order;
import com.gjx.enums.OrderStatusEnum;
import com.gjx.service.IOrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 管理员订单管理控制器
 */
@RestController
@RequestMapping("/api/admin/orders")
@Tag(name = "管理员订单管理", description = "管理员订单管理相关接口")
@RequiredArgsConstructor
public class AdminOrderController {

    private final IOrderService orderService;

    /**
     * 获取订单列表
     * @param page 页码
     * @param size 每页大小
     * @param orderNo 订单号
     * @param status 订单状态
     * @param userId 用户ID
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 订单列表
     */
    @Operation(summary = "获取订单列表", description = "获取所有订单列表，支持分页、订单号搜索、状态筛选、用户ID筛选和日期范围筛选")
    @GetMapping
    public R<Page<Order>> list(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String orderNo,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        Page<Order> orderPage = orderService.adminListOrders(page, size, orderNo, status, userId, startDate, endDate);
        return R.ok(orderPage);
    }

    /**
     * 获取订单详情
     * @param id 订单ID
     * @return 订单详情
     */
    @Operation(summary = "获取订单详情", description = "根据订单ID获取订单详情")
    @GetMapping("/{id}")
    public R<Order> detail(@PathVariable Long id) {
        Order order = orderService.getById(id);
        if (order == null) {
            return R.error(ResultCode.NOT_FOUND, "订单不存在");
        }
        return R.ok(order);
    }

    /**
     * 更新订单状态
     * @param id 订单ID
     * @param statusRequest 目标状态
     * @return 更新结果
     */
    @Operation(summary = "更新订单状态", description = "管理员应急通道：将订单强制改为任一合法状态")
    @PutMapping("/{id}/status")
    public R<?> updateStatus(@PathVariable Long id, @Valid @RequestBody OrderStatusRequest statusRequest) {
        // 目标状态必须是枚举内的合法值，否则会写进 999 这类脏数据污染统计
        OrderStatusEnum targetStatus = OrderStatusEnum.fromCode(statusRequest.getStatus());
        if (targetStatus == null) {
            return R.error(ResultCode.PARAM_ERROR, "非法的订单状态");
        }

        // 状态迁移校验、条件更新（期望前置状态进 WHERE，防 TOCTOU）与配套副作用
        // （取消/未发货退款回补库存、写各状态对应的时间戳与退款字段）都在
        // Service.adminChangeStatus 内原子完成——这里只做参数校验与结果文案。
        // 错误情形由 BusinessException 映射为对应状态码。
        boolean changed = orderService.adminChangeStatus(id, targetStatus);
        return R.ok(changed ? "状态更新成功" : "状态未变化");
    }

    /**
     * 发货
     * @param orderNo 订单号
     * @return 操作结果
     */
    @Operation(summary = "发货", description = "订单发货")
    @PutMapping("/deliver")
    public R<?> deliver(@RequestParam String orderNo) {
        orderService.deliverOrder(orderNo);
        return R.ok("发货成功");
    }
}
