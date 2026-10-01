package com.gjx.service.Impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.gjx.common.BusinessException;
import com.gjx.common.ResultCode;
import com.gjx.entity.*;
import com.gjx.enums.NotificationTypeEnum;
import com.gjx.enums.OrderStatusEnum;
import com.gjx.mapper.*;
import com.gjx.service.INotificationService;
import com.gjx.service.IOrderService;
import com.gjx.service.IProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderServiceImpl extends ServiceImpl<OrderMapper, Order> implements IOrderService {

    /** 订单列表未传 size 时的默认每页条数 */
    private static final int DEFAULT_PAGE_SIZE = 20;
    /** 订单列表每页条数上限，防止调用方传入超大 size 拖垮数据库 */
    private static final int MAX_PAGE_SIZE = 100;

    private final CartMapper cartMapper;
    private final ProductMapper productMapper;
    private final OrderItemMapper orderItemMapper;
    private final AddressMapper addressMapper;
    /**
     * 通过 Service 而非 Mapper 直接操作库存：库存变更是商品列表缓存的失效条件之一，
     * 走 Service 层可复用其 @CacheEvict，避免下单后前端仍看到旧库存
     */
    private final IProductService productService;
    /**
     * 状态流转后向买卖双方推送站内通知。
     * <p>调用点全部位于本类的 {@code @Transactional} 方法内，
     * WebSocket 实时推送由 NotificationServiceImpl 注册为 afterCommit 回调——
     * 事务回滚时通知既不入库也不推送，不会产生"幽灵提醒"。
     */
    private final INotificationService notificationService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Order createOrder(Long userId, Long addressId, List<Long> cartItemIds) {
        // 1. 查询选中的购物车商品
        List<Cart> cartItems = cartMapper.selectBatchIds(cartItemIds);
        // 归属校验：数量对不上（含不存在/已删除的 ID）或存在他人购物车项时一律拒绝，
        // 防止使用他人购物车 ID 下单并删除他人购物车数据
        if (cartItems.isEmpty() || cartItems.size() != cartItemIds.size()) {
            throw new BusinessException("购物车商品不存在");
        }
        if (cartItems.stream().anyMatch(item -> !Objects.equals(item.getUserId(), userId))) {
            throw new BusinessException("购物车商品不存在");
        }

        // 2. 校验地址
        Address address = addressMapper.selectById(addressId);
        if (address == null || !address.getUserId().equals(userId)) {
            throw new BusinessException("收货地址无效");
        }

        // 3. 原子"认领"购物车行（必须在扣库存之前）
        //    为什么用 DELETE 的返回值做认领：上面的 selectBatchIds 是快照读、不加锁，
        //    两个并发请求（或前端连点两次）可以同时读到同一批 cart 行并双双通过归属校验。
        //    DELETE 会对命中的 cart 行加行锁：并发下后到的事务会阻塞到前一个事务提交，
        //    提交后再执行 DELETE 时发现影响行数为 0 → 抛异常回滚，绝不会二次扣库存、二次建单。
        //    这正是原实现丢弃 delete 返回值的资损根因：能否"消费掉"这些购物车行，必须由影响行数裁决。
        int claimed = cartMapper.delete(new LambdaQueryWrapper<Cart>()
                .in(Cart::getId, cartItemIds)
                .eq(Cart::getUserId, userId));
        if (claimed != cartItemIds.size()) {
            throw new BusinessException("购物车商品已被处理，请勿重复提交");
        }

        // 4. 校验库存并锁定（使用悲观锁）
        // 按商品ID排序后加锁，保证多个并发事务以相同顺序获取行锁，避免交叉加锁导致的死锁
        List<Cart> sortedItems = cartItems.stream()
                .sorted(Comparator.comparing(Cart::getProductId))
                .toList();

        BigDecimal total = BigDecimal.ZERO;
        Map<Long, Product> productSnapshot = new HashMap<>();
        for (Cart item : sortedItems) {
            // 防御性校验：购物车入口已限制数量为正，但历史遗留数据可能违反该约束。
            // 负数量会让下面的 `stock >= quantity` 恒真，且扣减 SQL 变成反向加库存
            if (item.getQuantity() == null || item.getQuantity() <= 0) {
                throw new BusinessException("商品数量不合法");
            }
            Product product = productMapper.selectForUpdate(item.getProductId());
            if (product == null) {
                throw new BusinessException("商品不存在");
            }
            if (product.getStock() < item.getQuantity()) {
                throw new BusinessException("商品 " + product.getName() + " 库存不足");
            }
            productSnapshot.put(product.getId(), product);
            total = total.add(product.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())));
        }

        // 5. 扣减库存（条件更新，库存不足时影响行数为 0）
        for (Cart item : sortedItems) {
            if (!productService.decreaseStock(item.getProductId(), item.getQuantity())) {
                throw new BusinessException("扣减库存失败");
            }
        }

        // 6. 创建订单
        Order order = new Order();
        // 去横杠后的 UUID 恰好 32 位，无需再截取
        order.setOrderNo(UUID.randomUUID().toString().replace("-", ""));
        order.setUserId(userId);
        order.setAddressId(addressId);
        order.setTotalAmount(total);
        order.setStatus(OrderStatusEnum.PENDING_PAYMENT.getCode());
        save(order);
        log.info("[创建订单] orderNo={}, userId={}, amount={}", order.getOrderNo(), userId, total);

        // 7. 保存订单明细（复用步骤4中已加锁查询到的商品快照，避免重复查库）
        for (Cart item : sortedItems) {
            Product product = productSnapshot.get(item.getProductId());
            OrderItem orderItem = new OrderItem();
            orderItem.setOrderId(order.getId());
            orderItem.setProductId(product.getId());
            orderItem.setProductName(product.getName());
            orderItem.setProductImage(product.getMainImage());
            orderItem.setPrice(product.getPrice());
            orderItem.setQuantity(item.getQuantity());
            orderItemMapper.insert(orderItem);
        }

        return order;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void mockPay(String orderNo, Long userId) {
        // 归属校验：只能支付自己的订单
        Order order = getOwnedOrder(orderNo, userId);

        // 原子状态流转：仅当仍处于待付款时更新成功，避免并发重复支付
        LambdaUpdateWrapper<Order> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(Order::getId, order.getId())
                .eq(Order::getStatus, OrderStatusEnum.PENDING_PAYMENT.getCode())
                .set(Order::getStatus, OrderStatusEnum.PAID.getCode())
                .set(Order::getPayTime, LocalDateTime.now());
        if (baseMapper.update(null, updateWrapper) == 0) {
            throw new BusinessException("订单状态已变更，请刷新后重试");
        }
        log.info("[订单支付] orderNo={}, userId={}", orderNo, userId);
        notifyMerchants(order.getId(), NotificationTypeEnum.ORDER_PAID,
                "订单 " + order.getOrderNo() + " 已付款，请尽快发货", "/merchant/orders");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelOrder(String orderNo, Long userId) {
        // 归属校验：只能取消自己的订单
        Order order = getOwnedOrder(orderNo, userId);
        cancelPendingOrder(order, "只有待付款订单可以取消");
        log.info("[订单取消] orderNo={}, userId={}", orderNo, userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deliverOrder(String orderNo) {
        Order order = getByOrderNo(orderNo, null);
        if (order == null) throw new BusinessException("订单不存在");

        LambdaUpdateWrapper<Order> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(Order::getId, order.getId())
                .eq(Order::getStatus, OrderStatusEnum.PAID.getCode())
                .set(Order::getStatus, OrderStatusEnum.DELIVERED.getCode())
                .set(Order::getDeliveryTime, LocalDateTime.now());
        if (baseMapper.update(null, updateWrapper) == 0) {
            throw new BusinessException("只有已付款订单可以发货");
        }
        log.info("[订单发货] orderNo={}", orderNo);
        notificationService.push(order.getUserId(), NotificationTypeEnum.ORDER_DELIVERED,
                "订单 " + orderNo + " 已发货，请注意查收", "/order/" + order.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deliverOrderByMerchant(String orderNo, Long merchantId) {
        Order order = getByOrderNo(orderNo, null);
        if (order == null) throw new BusinessException("订单不存在");
        // 归属校验：订单必须包含该商家的商品，否则商家可操作平台上任意订单
        if (!orderContainsMerchantProduct(order.getId(), merchantId)) {
            throw new BusinessException("订单不存在");
        }

        LambdaUpdateWrapper<Order> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(Order::getId, order.getId())
                .eq(Order::getStatus, OrderStatusEnum.PAID.getCode())
                .set(Order::getStatus, OrderStatusEnum.DELIVERED.getCode())
                .set(Order::getDeliveryTime, LocalDateTime.now());
        if (baseMapper.update(null, updateWrapper) == 0) {
            throw new BusinessException("只有已付款订单可以发货");
        }
        log.info("[商家发货] orderNo={}, merchantId={}", orderNo, merchantId);
        notificationService.push(order.getUserId(), NotificationTypeEnum.ORDER_DELIVERED,
                "订单 " + orderNo + " 已发货，请注意查收", "/order/" + order.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void confirmReceive(String orderNo, Long userId) {
        // 归属校验：只能确认自己的订单
        Order order = getOwnedOrder(orderNo, userId);

        LambdaUpdateWrapper<Order> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(Order::getId, order.getId())
                .eq(Order::getStatus, OrderStatusEnum.DELIVERED.getCode())
                .set(Order::getStatus, OrderStatusEnum.COMPLETED.getCode())
                .set(Order::getFinishTime, LocalDateTime.now());
        if (baseMapper.update(null, updateWrapper) == 0) {
            throw new BusinessException("只有已发货订单可以确认收货");
        }
        log.info("[确认收货] orderNo={}, userId={}", orderNo, userId);
        notifyMerchants(order.getId(), NotificationTypeEnum.ORDER_COMPLETED,
                "订单 " + orderNo + " 买家已确认收货", "/merchant/orders");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void requestRefund(String orderNo, Long userId, String reason) {
        // 归属校验：只能对自己的订单申请退款
        Order order = getOwnedOrder(orderNo, userId);

        Integer currentStatus = order.getStatus();
        boolean unshipped = OrderStatusEnum.PAID.getCode().equals(currentStatus);
        boolean shipped = OrderStatusEnum.DELIVERED.getCode().equals(currentStatus);
        if (!unshipped && !shipped) {
            throw new BusinessException("只有已付款或已发货订单可以申请退款");
        }

        LambdaUpdateWrapper<Order> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(Order::getId, order.getId())
                // 以当前状态作为条件，保证并发下不会与支付/发货/取消互相覆盖
                .eq(Order::getStatus, currentStatus)
                .set(Order::getStatus, OrderStatusEnum.REFUNDED.getCode())
                .set(Order::getRefundAmount, order.getTotalAmount())
                .set(Order::getRefundReason, reason)
                .set(Order::getRefundTime, LocalDateTime.now());
        if (baseMapper.update(null, updateWrapper) == 0) {
            throw new BusinessException("订单状态已变更，请刷新后重试");
        }

        // 未发货的订单退款需归还库存；已发货订单须先完成退货入库，此处不回补，
        // 避免"货还在用户手上、库存却已恢复"导致超卖
        if (unshipped) {
            restoreStock(order.getId());
        }
        log.info("[订单退款] orderNo={}, userId={}, amount={}, reason={}",
                orderNo, userId, order.getTotalAmount(), reason);
        notifyMerchants(order.getId(), NotificationTypeEnum.ORDER_REFUNDED,
                "订单 " + orderNo + " 买家申请退款，请及时处理", "/merchant/orders");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelTimeoutOrder(Long orderId) {
        Order order = getById(orderId);
        if (order == null) return;
        // 条件更新保证同一订单只会被成功取消一次：已被用户取消或已支付的订单会更新失败并直接跳过，
        // 从而避免多实例定时任务并发执行时重复回补库存。
        // 只有真正取消成功（返回 true）才通知买家，避免多实例争抢时重复推送。
        boolean cancelled = cancelPendingOrder(order, null);
        if (cancelled) {
            notificationService.push(order.getUserId(), NotificationTypeEnum.ORDER_TIMEOUT_CANCELLED,
                    "订单 " + order.getOrderNo() + " 因超时未支付已自动取消", "/order/" + order.getId());
        }
    }

    /**
     * 原子取消待付款订单并恢复库存
     *
     * @param order           订单
     * @param failureMessage  状态不匹配时的提示；为 null 表示由调用方（定时任务）静默跳过
     * @return 本次调用是否真正完成了取消（false 表示订单已被并发操作改变了状态，未做任何写入）
     */
    private boolean cancelPendingOrder(Order order, String failureMessage) {
        LambdaUpdateWrapper<Order> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(Order::getId, order.getId())
                .eq(Order::getStatus, OrderStatusEnum.PENDING_PAYMENT.getCode())
                .set(Order::getStatus, OrderStatusEnum.CANCELLED.getCode())
                .set(Order::getCancelTime, LocalDateTime.now());
        if (baseMapper.update(null, updateWrapper) == 0) {
            if (failureMessage != null) {
                throw new BusinessException(failureMessage);
            }
            return false;
        }
        restoreStock(order.getId());
        return true;
    }

    /**
     * 恢复订单中所有商品的库存，并回冲销量
     * <p>
     * 只应在状态流转成功（影响行数为 1）后调用，否则会造成库存重复回补。<br>
     * 取消/退款时若不回冲 {@code product.sales}，销量会永久虚高，导致"按销量排序/热销榜"失真，
     * 因此这里改用 {@code restoreStockAndSales} 同时回补库存与回冲销量。<br>
     * 走 mapper 直连而非 productService，是为了使用"同时改库存与销量"的原子语句；
     * 商品列表缓存内嵌了库存与销量，故循环结束后统一失效一次（等价于原 productService.increaseStock 的 @CacheEvict）。
     */
    private void restoreStock(Long orderId) {
        List<OrderItem> items = orderItemMapper.selectList(
                new LambdaQueryWrapper<OrderItem>().eq(OrderItem::getOrderId, orderId));
        if (items.isEmpty()) {
            return;
        }
        for (OrderItem item : items) {
            int rows = productMapper.restoreStockAndSales(item.getProductId(), item.getQuantity());
            if (rows == 0) {
                // 销量不足以回冲（历史/手工数据异常）时退化为仅回补库存，优先保证库存不丢失，
                // 同时留一条告警日志便于排查销量口径异常
                log.warn("[库存回补] 商品销量不足，仅回补库存 orderId={}, productId={}, quantity={}",
                        orderId, item.getProductId(), item.getQuantity());
                productMapper.increaseStock(item.getProductId(), item.getQuantity());
            }
        }
        productService.evictProductListCache();
    }

    /**
     * 查询订单并校验归属
     * <p>
     * 用户不存在或不属于该用户时，统一抛出"订单不存在"，避免通过错误信息枚举他人订单号。
     */
    private Order getOwnedOrder(String orderNo, Long userId) {
        if (userId == null) {
            throw new BusinessException("未获取到登录用户");
        }
        Order order = getByOrderNo(orderNo, userId);
        if (order == null) {
            throw new BusinessException("订单不存在");
        }
        return order;
    }

    /**
     * 判断订单是否包含指定商家的商品
     */
    private boolean orderContainsMerchantProduct(Long orderId, Long merchantId) {
        return resolveMerchantUserIds(orderId).contains(merchantId);
    }

    /**
     * 查出订单中所有商品所属的商家用户ID（去重）。
     *
     * <p><b>为什么不读 {@code order.merchant_id}：</b>那一列未被 ORM 映射、无索引、代码从不写入，
     * 是建表早期留下的死列（见 {@code .claude/CLAUDE.md} 坑位清单第 3 条）。
     * 判断"订单属于哪个商家"的唯一可靠路径是 {@code order_item → product.merchant_id}，
     * 且 {@code product.merchant_id} 存的就是商家在 {@code user} 表里的主键
     * （商家端各 Controller 用 {@code findByUsername(...).getId()} 取得同一个值）。
     *
     * <p>一次订单可能跨多个商家（购物车混合下单），故返回列表而非单值；
     * 通知类操作按商家逐个推送，归属校验类操作则用上面的 {@code contains} 判断。
     */
    private List<Long> resolveMerchantUserIds(Long orderId) {
        List<OrderItem> items = orderItemMapper.selectList(
                new LambdaQueryWrapper<OrderItem>().eq(OrderItem::getOrderId, orderId));
        if (items.isEmpty()) {
            return List.of();
        }
        List<Long> productIds = items.stream().map(OrderItem::getProductId).distinct().toList();
        // 只 SELECT merchant_id 一列：这里不需要商品的其他字段，避免把 description 等大字段拉回来
        return productMapper.selectList(new LambdaQueryWrapper<Product>()
                        .select(Product::getMerchantId)
                        .in(Product::getId, productIds))
                .stream()
                .map(Product::getMerchantId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
    }

    /**
     * 向订单涉及的所有商家推送同一条通知。
     *
     * <p>逐个 {@code push} 而非批量插入：{@code push} 内部会为每个收件人单独建立
     * WebSocket 会话寻址，且通知量级等于订单涉及商家数（通常 1~3），无需为它做批量优化。
     */
    private void notifyMerchants(Long orderId, NotificationTypeEnum type, String content, String link) {
        for (Long merchantUserId : resolveMerchantUserIds(orderId)) {
            notificationService.push(merchantUserId, type, content, link);
        }
    }

    @Override
    public Page<Order> pageByUserId(Long userId, List<Integer> statuses, String keyword,
                                    String startDate, String endDate, Integer page, Integer size) {
        QueryWrapper<Order> queryWrapper = userOrderFilter(userId, keyword, startDate, endDate);
        if (statuses != null && !statuses.isEmpty()) {
            queryWrapper.in("status", statuses);
        }
        queryWrapper.orderByDesc("create_time");

        int pageNo = (page == null || page < 1) ? 1 : page;
        int pageSize = (size == null || size < 1) ? DEFAULT_PAGE_SIZE : Math.min(size, MAX_PAGE_SIZE);
        return page(new Page<>(pageNo, pageSize), queryWrapper);
    }

    @Override
    public Map<Integer, Long> countByStatus(Long userId, String keyword, String startDate, String endDate) {
        QueryWrapper<Order> queryWrapper = userOrderFilter(userId, keyword, startDate, endDate);
        // 只取「状态 + 聚合值」两列，并刻意不加 status 条件——本方法做的就是按状态分组
        queryWrapper.select("status", "COUNT(*) AS cnt").groupBy("status");

        Map<Integer, Long> counts = new HashMap<>();
        for (Map<String, Object> row : baseMapper.selectMaps(queryWrapper)) {
            // 走 Number 取值：COUNT(*) 在不同驱动下可能映射为 Long / BigInteger
            if (row.get("status") instanceof Number statusNo && row.get("cnt") instanceof Number countNo) {
                counts.put(statusNo.intValue(), countNo.longValue());
            }
        }
        return counts;
    }

    /**
     * 构造用户端订单查询的公共过滤条件：归属 + 关键词 + 下单日期，**不含状态**。
     *
     * <p>列表与状态计数必须共用本方法，而不是各写一份。角标数字与列表内容对不上
     * （统计只覆盖最近 N 笔）正是本次重构要修掉的缺陷——两份独立的条件迟早会漂移。
     *
     * <p>用 {@link QueryWrapper}（字符串列名）而非 {@code LambdaQueryWrapper}：
     * 状态计数需要 {@code select("status", "COUNT(*) AS cnt")} 与 {@code groupBy}，
     * 只有 QueryWrapper 提供基于字符串的 select；两者共用本方法就不能混用两种 Wrapper。
     * 列名写错会在 {@code OrderServiceImplTest} 的 SQL 断言里暴露，不依赖运行期才发现。
     *
     * @param userId    订单归属用户。进 WHERE 条件而非"查出来再判断"，避免条件变化时的竞态
     * @param keyword   商品名称 / 订单号关键词，空白表示不过滤
     * @param startDate 下单开始日期 yyyy-MM-dd，空白表示不限
     * @param endDate   下单结束日期 yyyy-MM-dd（含当天），空白表示不限
     * @return 已附加上述条件的 QueryWrapper
     */
    static QueryWrapper<Order> userOrderFilter(Long userId, String keyword, String startDate, String endDate) {
        QueryWrapper<Order> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("user_id", userId);

        if (keyword != null && !keyword.isBlank()) {
            String kw = keyword.trim();
            // 整块必须包在 and(...) 里：少了这层括号，or 会把 user_id 条件一并"或"掉，
            // 查询退化成"任意用户的匹配订单"——既返回错数据，又是越权
            queryWrapper.and(inner -> inner
                    .like("order_no", kw)
                    .or()
                    // 商品名称在 order_item 上，用 EXISTS 子查询：走 idx_order_item_order 索引。
                    // {0} 由 MyBatis-Plus 绑定成 #{} 占位符（见 getParamNameValuePairs），
                    // 不是把关键词拼进 SQL 文本，因此不受 SQL 注入影响
                    .exists("SELECT 1 FROM order_item oi WHERE oi.order_id = `order`.id "
                            + "AND oi.product_name LIKE CONCAT('%', {0}, '%')", kw));
        }
        if (startDate != null && !startDate.isBlank()) {
            queryWrapper.ge("create_time", LocalDate.parse(startDate.trim()).atStartOfDay());
        }
        if (endDate != null && !endDate.isBlank()) {
            // 半开区间 [start, end+1day)：写成 <= end 23:59:59 会漏掉当天最后不足一秒的订单
            queryWrapper.lt("create_time", LocalDate.parse(endDate.trim()).plusDays(1).atStartOfDay());
        }
        return queryWrapper;
    }

    @Override
    public Map<Long, List<OrderItem>> getOrderItemsByOrderIds(List<Long> orderIds) {
        if (orderIds == null || orderIds.isEmpty()) {
            return Collections.emptyMap();
        }
        // 一次 IN 查询取回整页明细，再按订单ID分组，避免逐单查询（N+1）
        List<OrderItem> items = orderItemMapper.selectList(
                new LambdaQueryWrapper<OrderItem>().in(OrderItem::getOrderId, orderIds));
        return items.stream().collect(Collectors.groupingBy(OrderItem::getOrderId));
    }

    @Override
    public Order getByOrderNo(String orderNo, Long userId) {
        LambdaQueryWrapper<Order> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Order::getOrderNo, orderNo);
        if (userId != null) {
            queryWrapper.eq(Order::getUserId, userId);
        }
        return getOne(queryWrapper);
    }

    @Override
    public Page<Order> adminListOrders(Integer page, Integer size, String orderNo, Integer status, Long userId, String startDate, String endDate) {
        LambdaQueryWrapper<Order> queryWrapper = new LambdaQueryWrapper<>();
        if (orderNo != null && !orderNo.isEmpty()) {
            queryWrapper.like(Order::getOrderNo, orderNo);
        }
        if (status != null) {
            queryWrapper.eq(Order::getStatus, status);
        }
        if (userId != null) {
            queryWrapper.eq(Order::getUserId, userId);
        }
        if (startDate != null && !startDate.isEmpty()) {
            LocalDateTime startDateTime = LocalDateTime.parse(startDate + "T00:00:00");
            queryWrapper.ge(Order::getCreateTime, startDateTime);
        }
        if (endDate != null && !endDate.isEmpty()) {
            LocalDateTime endDateTime = LocalDateTime.parse(endDate + "T23:59:59");
            queryWrapper.le(Order::getCreateTime, endDateTime);
        }
        queryWrapper.orderByDesc(Order::getCreateTime);
        return page(new Page<>(page, size), queryWrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean adminChangeStatus(Long orderId, OrderStatusEnum targetStatus) {
        Order order = getById(orderId);
        if (order == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "订单不存在");
        }
        OrderStatusEnum currentStatus = OrderStatusEnum.fromCode(order.getStatus());
        if (currentStatus == null) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "订单当前状态异常，无法流转");
        }
        if (currentStatus == targetStatus) {
            return false;                       // 幂等：重复提交不报错，也不产生任何写入
        }
        if (!currentStatus.canTransitionTo(targetStatus)) {
            throw new BusinessException(ResultCode.BUSINESS_ERROR,
                    "不允许从「" + currentStatus.getDescription() + "」变更为「" + targetStatus.getDescription() + "」");
        }

        boolean refunding = targetStatus == OrderStatusEnum.REFUNDED;
        LambdaUpdateWrapper<Order> updateWrapper = new LambdaUpdateWrapper<Order>()
                .eq(Order::getId, orderId)
                // 期望的前置状态进 WHERE：这样"检查"与"更新"之间即便被并发修改也写不坏数据（防 TOCTOU）
                .eq(Order::getStatus, currentStatus.getCode())
                .set(Order::getStatus, targetStatus.getCode());
        // 每种目标状态各有自己的时间戳字段，正常流转路径（mockPay / deliverOrder /
        // confirmReceive / cancelPendingOrder / requestRefund）都会写。强制流转同样必须写：
        // 前端订单卡片的"付款/发货/完成/取消时间"直接读这些字段，不写就是空白。
        LocalDateTime now = LocalDateTime.now();
        switch (targetStatus) {
            case PAID -> updateWrapper.set(Order::getPayTime, now);
            case DELIVERED -> updateWrapper.set(Order::getDeliveryTime, now);
            case COMPLETED -> updateWrapper.set(Order::getFinishTime, now);
            case CANCELLED -> updateWrapper.set(Order::getCancelTime, now);
            // 退款字段与 requestRefund 同口径：金额取整单金额，时间取当前。
            // 退款原因不编造、保持为空——这是管理员强制操作，没有买家填写的理由
            case REFUNDED -> updateWrapper
                    .set(Order::getRefundAmount, order.getTotalAmount())
                    .set(Order::getRefundTime, now);
            // 没有任何迁移以「待付款」为目标（canTransitionTo 已排除回流），此分支不可达
            case PENDING_PAYMENT -> { }
        }
        if (baseMapper.update(null, updateWrapper) == 0) {
            throw new BusinessException(ResultCode.BUSINESS_ERROR, "订单状态已被其他操作变更，请刷新后重试");
        }

        // 只有"货还没出库"的流转才回补库存，否则会出现"货在买家手上、库存却已恢复"导致超卖：
        //   → 已取消：来源必然是待付款（未付款但库存在下单时已扣）      ⇒ 回补
        //   → 已退款：来源是已付款（未发货）⇒ 回补；来源是已发货/已完成 ⇒ 不回补
        boolean releasingStock = targetStatus == OrderStatusEnum.CANCELLED
                || (refunding && currentStatus == OrderStatusEnum.PAID);
        if (releasingStock) {
            restoreStock(orderId);
        }

        log.info("[管理员变更订单状态] orderId={}, {} -> {}, 回补库存={}",
                orderId, currentStatus.getCode(), targetStatus.getCode(), releasingStock);
        return true;
    }

    @Override
    public int countTodayOrders() {
        return baseMapper.countTodayOrders();
    }

    @Override
    public BigDecimal getTodaySales() {
        String today = LocalDate.now().toString();
        String tomorrow = LocalDate.now().plusDays(1).toString();
        BigDecimal sales = baseMapper.selectTotalSalesByDateAndStatus(
                today, tomorrow, OrderStatusEnum.PAID.getCode());
        return sales != null ? sales : BigDecimal.ZERO;
    }

    @Override
    public long countTotalOrders() {
        return count();
    }

    @Override
    public BigDecimal getTotalSales() {
        BigDecimal sales = baseMapper.selectTotalSalesByStatus(OrderStatusEnum.PAID.getCode());
        return sales != null ? sales : BigDecimal.ZERO;
    }

    @Override
    public BigDecimal getSalesByDate(String date) {
        String nextDay = LocalDate.parse(date).plusDays(1).toString();
        BigDecimal sales = baseMapper.selectTotalSalesByDateAndStatus(
                date, nextDay, OrderStatusEnum.PAID.getCode());
        return sales != null ? sales : BigDecimal.ZERO;
    }

    @Override
    public int countOrdersByDate(String date) {
        String nextDay = LocalDate.parse(date).plusDays(1).toString();
        return baseMapper.countOrdersByDate(date, nextDay);
    }

    @Override
    public List<Map<String, Object>> getHotProducts(int limit) {
        // 使用 SQL GROUP BY 聚合替代全量加载到内存
        return orderItemMapper.getHotProducts(limit);
    }
}
