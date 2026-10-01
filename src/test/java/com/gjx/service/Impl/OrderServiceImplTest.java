package com.gjx.service.Impl;

import com.baomidou.mybatisplus.core.conditions.AbstractWrapper;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.gjx.entity.Order;
import com.gjx.mapper.AddressMapper;
import com.gjx.mapper.CartMapper;
import com.gjx.mapper.OrderItemMapper;
import com.gjx.mapper.OrderMapper;
import com.gjx.mapper.ProductMapper;
import com.gjx.service.INotificationService;
import com.gjx.service.IProductService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigInteger;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link OrderServiceImpl} 用户端订单查询条件构造的测试。
 *
 * <p><b>为什么单独测这个：</b>本类的 {@code userOrderFilter} 注释里写了三条对外承诺——
 * ①「关键词走 {@code {0}} 占位符绑定，不拼进 SQL 文本」②「OR 块被括号包住，
 * 不会把 {@code user_id} 条件一起或掉」③「列表与状态计数共用同一套条件，角标数字不会与列表漂移」。
 * 承诺写在注释里不会被构建验证：有人把占位符改成字符串拼接、或删掉那层括号，
 * 代码照样编译、页面照样能点，但前者引入 SQL 注入、后者会返回**别人**的订单。
 * 这里把三条承诺各钉一个测试。
 *
 * <p>另外把「每页条数上限」和「COUNT 映射为 BigInteger」也钉住：两者都是静默失效的类型——
 * 前者失效是慢查询拖垮数据库，后者失效是角标恒为 0（「看起来实现了、实际没生效」）。
 */
@ExtendWith(MockitoExtension.class)
class OrderServiceImplTest {

    /** 用真实中文关键词：ASCII 关键词可能与 SQL 里的列名/别名撞车，让「未拼进 SQL」的断言失去意义 */
    private static final String KEYWORD = "沙发";

    private static final int MAX_PAGE_SIZE = 100;

    @Mock
    private CartMapper cartMapper;

    @Mock
    private ProductMapper productMapper;

    @Mock
    private OrderItemMapper orderItemMapper;

    @Mock
    private AddressMapper addressMapper;

    @Mock
    private IProductService productService;

    @Mock
    private INotificationService notificationService;

    @Mock
    private OrderMapper orderMapper;

    private OrderServiceImpl orderService;

    @BeforeEach
    void setUp() {
        orderService = new OrderServiceImpl(cartMapper, productMapper, orderItemMapper,
                addressMapper, productService, notificationService);
        // baseMapper 是 ServiceImpl 的 protected 字段，@InjectMocks 不会注入，只能显式塞进去
        ReflectionTestUtils.setField(orderService, "baseMapper", orderMapper);
    }

    // ---------- 承诺 ①：关键词参数化 ----------

    @Test
    @DisplayName("关键词走参数绑定 → 用户输入不出现在 SQL 文本里")
    void keywordIsBoundAsParameterNotInlined() {
        QueryWrapper<Order> wrapper = OrderServiceImpl.userOrderFilter(1L, KEYWORD, null, null);

        assertThat(wrapper.getSqlSegment()).doesNotContain(KEYWORD);
        // 值确实被绑定了：LIKE 两侧各补一个 %，EXISTS 子查询用原值
        assertThat(wrapper.getParamNameValuePairs())
                .containsValue("%" + KEYWORD + "%")
                .containsValue(KEYWORD);
    }

    @Test
    @DisplayName("商品名称经 EXISTS 子查询匹配 → 不引入 join，也不落到 order 表的不存在列上")
    void keywordMatchesProductNameViaExistsSubquery() {
        String sql = OrderServiceImpl.userOrderFilter(1L, KEYWORD, null, null).getSqlSegment();

        assertThat(sql).contains("order_item").contains("oi.order_id");
    }

    @Test
    @DisplayName("负向对照：关键词真被写进 SQL 文本时，上面那条断言确实会失败")
    void negativeControlInlinedKeywordIsDetectable() {
        // 这里刻意用字面量把关键词塞进 SQL（不涉及任何用户输入），只为证明
        // keywordIsBoundAsParameterNotInlined 的 doesNotContain 不是"永远为真"的空断言
        QueryWrapper<Order> inlined = new QueryWrapper<Order>()
                .eq("user_id", 1L)
                .apply("EXISTS (SELECT 1 FROM order_item oi WHERE oi.product_name LIKE '%沙发%')");

        assertThat(inlined.getSqlSegment()).contains(KEYWORD);
    }

    // ---------- 承诺 ②：OR 块必须自带括号 ----------

    @Test
    @DisplayName("关键词的 OR 块被括号包住 → user_id 条件不会被 or 吞掉")
    void keywordOrBlockIsParenthesised() {
        String sql = OrderServiceImpl.userOrderFilter(1L, KEYWORD, null, null).getSqlSegment();

        // 少了外层括号，条件会退化成 WHERE user_id = ? AND order_no LIKE ? OR EXISTS(...)，
        // 语义上等于「任意用户的匹配订单」——既返回错数据，又是越权
        assertThat(sql).containsPattern("AND\\s+\\(\\s*order_no\\s+LIKE.*OR\\s+EXISTS");
    }

    @Test
    @DisplayName("关键词为空白 → 等同于不过滤，不产生 LIKE '%%' 的全表扫")
    void blankKeywordAddsNoCondition() {
        assertThat(OrderServiceImpl.userOrderFilter(1L, "   ", null, null).getSqlSegment())
                .doesNotContain("order_no");
        assertThat(OrderServiceImpl.userOrderFilter(1L, null, null, null).getSqlSegment())
                .doesNotContain("order_no");
    }

    // ---------- 日期区间 ----------

    @Test
    @DisplayName("结束日期含当天 → 上界取次日零点（半开区间）")
    void endDateIsInclusiveViaHalfOpenInterval() {
        QueryWrapper<Order> wrapper =
                OrderServiceImpl.userOrderFilter(1L, null, "2026-10-01", "2026-10-31");

        // 上界是 11-01 零点而非 10-31 23:59:59：后者会漏掉当天最后不足一秒的订单
        assertThat(boundValues(wrapper).values())
                .contains(LocalDateTime.parse("2026-10-01T00:00:00"))
                .contains(LocalDateTime.parse("2026-11-01T00:00:00"));
    }

    // ---------- 承诺 ③：列表与计数共用条件 ----------

    @Test
    @DisplayName("列表与状态计数的过滤条件逐字一致 → 角标数字与列表内容不会漂移")
    void listAndCountShareIdenticalFilterConditions() {
        when(orderMapper.selectPage(any(), any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(orderMapper.selectMaps(any())).thenReturn(List.of());

        orderService.pageByUserId(1L, null, KEYWORD, "2026-10-01", "2026-10-31", 1, 10);
        orderService.countByStatus(1L, KEYWORD, "2026-10-01", "2026-10-31");

        ArgumentCaptor<Wrapper<Order>> captor = ArgumentCaptor.forClass(Wrapper.class);
        verify(orderMapper).selectPage(any(), captor.capture());
        verify(orderMapper).selectMaps(captor.capture());

        Wrapper<Order> listWrapper = captor.getAllValues().get(0);
        Wrapper<Order> countWrapper = captor.getAllValues().get(1);

        // 只比对 WHERE 部分：列表额外带 ORDER BY、计数额外带 GROUP BY，那是各自的附加项
        assertThat(whereClause(listWrapper)).isEqualTo(whereClause(countWrapper));
        // 顺带钉住绑定值也一致（条件文本相同但绑错值，同样会让两边口径不一致）
        assertThat(boundValues(listWrapper)).isEqualTo(boundValues(countWrapper));
    }

    @Test
    @DisplayName("状态计数按状态分组，且刻意不带 status 条件")
    void countByStatusGroupsWithoutStatusFilter() {
        when(orderMapper.selectMaps(any())).thenReturn(List.of());

        orderService.countByStatus(1L, null, null, null);

        verify(orderMapper).selectMaps(argThatWrapper(wrapper -> {
            String sql = wrapper.getSqlSegment();
            // 带了 status 条件就只会统计出一种状态，「按状态拆分」的意义就没了
            return sql.contains("GROUP BY") && !sql.contains("status =");
        }));
    }

    @Test
    @DisplayName("COUNT 被驱动映射为 BigInteger 时仍能取到数量 → 角标不会恒为 0")
    void countByStatusHandlesBigIntegerCount() {
        when(orderMapper.selectMaps(any())).thenReturn(List.of(
                Map.<String, Object>of("status", 0, "cnt", BigInteger.valueOf(3)),
                Map.<String, Object>of("status", 3, "cnt", 5L)));

        assertThat(orderService.countByStatus(1L, null, null, null))
                .containsEntry(0, 3L)
                .containsEntry(3, 5L);
    }

    // ---------- 分页参数钳制 ----------

    @Test
    @DisplayName("每页条数超过上限时被钳制 → 调用方传 1000 也只取 100 条")
    void pageSizeIsClampedToMaximum() {
        when(orderMapper.selectPage(any(), any())).thenAnswer(invocation -> invocation.getArgument(0));

        Page<Order> result = orderService.pageByUserId(1L, null, null, null, null, 1, 1000);

        assertThat(result.getSize()).isEqualTo(MAX_PAGE_SIZE);
    }

    @Test
    @DisplayName("页码非法时归一到第 1 页")
    void illegalPageNumberFallsBackToFirstPage() {
        when(orderMapper.selectPage(any(), any())).thenAnswer(invocation -> invocation.getArgument(0));

        assertThat(orderService.pageByUserId(1L, null, null, null, null, 0, 10).getCurrent()).isEqualTo(1);
        assertThat(orderService.pageByUserId(1L, null, null, null, null, -5, 10).getCurrent()).isEqualTo(1);
    }

    @Test
    @DisplayName("状态集合为「全部」时不产生 status 条件；有值时按 IN 收敛")
    void statusFilterIsOptionalAndInclusive() {
        when(orderMapper.selectPage(any(), any())).thenAnswer(invocation -> invocation.getArgument(0));

        orderService.pageByUserId(1L, List.of(), null, null, null, 1, 10);
        verify(orderMapper).selectPage(any(), argThatWrapper(wrapper ->
                !wrapper.getSqlSegment().contains("status")));

        // 「退款/售后」页签覆盖 4 已取消 + 5 已退款，必须一次收敛两种状态
        orderService.pageByUserId(1L, List.of(4, 5), null, null, null, 1, 10);
        verify(orderMapper).selectPage(any(), argThatWrapper(wrapper ->
                wrapper.getSqlSegment().contains("status IN")
                        && boundValues(wrapper).containsValue(4)
                        && boundValues(wrapper).containsValue(5)));
    }

    // ---------- 辅助 ----------

    /**
     * 取 WHERE 部分。{@code getSqlSegment()} 还会带上 GROUP BY / ORDER BY，
     * 而这两者是列表与计数各自的附加项，不属于"过滤条件"。
     */
    private static String whereClause(Wrapper<Order> wrapper) {
        String sql = wrapper.getSqlSegment();
        int cut = sql.indexOf(" GROUP BY");
        if (cut < 0) {
            cut = sql.indexOf(" ORDER BY");
        }
        return cut < 0 ? sql : sql.substring(0, cut);
    }

    /**
     * 取绑定参数。
     *
     * <p>两处都不能省：
     * <ul>
     *   <li>先取一次 SQL 片段 —— MyBatis-Plus 3.5 的查询条件是<b>延迟渲染</b>的，
     *       参数要到 {@code getSqlSegment()} 求值时才登记进 {@code paramNameValuePairs}。
     *       直接读会拿到<b>空 Map</b>，让 {@code containsValue} 这类断言"真空失败"，
     *       更危险的是让否定式断言（如 {@code doesNotContainValue}）真空<b>通过</b>。</li>
     *   <li>收窄成 {@code AbstractWrapper} —— {@code getParamNameValuePairs} 声明在它上面，
     *       而 Mapper 方法的形参类型是更宽的 {@code Wrapper}。</li>
     * </ul>
     */
    private static Map<String, Object> boundValues(Wrapper<Order> wrapper) {
        wrapper.getSqlSegment();
        return ((AbstractWrapper<Order, String, ?>) wrapper).getParamNameValuePairs();
    }

    /**
     * 包装 Mockito 的 {@code argThat}：MyBatis-Plus 的 Wrapper 泛型嵌套较深，
     * 直接写 lambda 会让类型推断退化到 Object，这里显式收窄一次。
     */
    private static Wrapper<Order> argThatWrapper(java.util.function.Predicate<Wrapper<Order>> predicate) {
        return org.mockito.ArgumentMatchers.argThat(predicate::test);
    }
}
