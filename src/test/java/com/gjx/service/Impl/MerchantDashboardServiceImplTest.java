package com.gjx.service.Impl;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.AbstractWrapper;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.gjx.entity.Product;
import com.gjx.mapper.OrderItemMapper;
import com.gjx.mapper.OrderMapper;
import com.gjx.service.ICategoryService;
import com.gjx.service.IProductService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link MerchantDashboardServiceImpl} 新增的低库存 / 已下架统计测试。
 *
 * <p><b>为什么单独测这个：</b>这两项原先在前端是硬编码文案（"库存巡检" 的值是字符串
 * {@code '巡检'}、"资料维护" 恒为 {@code '1项'}）。改成真实统计后，两个都容易**静默写错**：
 * <ul>
 *   <li>阈值用 {@code <} 而不是 {@code <=} —— 库存正好等于阈值的商品不再告警，没人会发现；</li>
 *   <li>忘了限定 {@code status = 1} —— 已下架商品卖不出去，库存再低也不该算待办，
 *       算了会让商家看到一个永远处理不掉的待办。</li>
 * </ul>
 * 两种错误都不会报错、不会让测试变红（如果没有本测试），只会让数字一直是错的。
 */
@ExtendWith(MockitoExtension.class)
class MerchantDashboardServiceImplTest {

    /** 低库存阈值，必须与 MerchantDashboardServiceImpl.LOW_STOCK_THRESHOLD 一致 */
    private static final int LOW_STOCK_THRESHOLD = 10;

    /** 商品「在售」状态值 */
    private static final int ON_SHELF_STATUS = 1;

    @Mock
    private IProductService productService;

    @Mock
    private ICategoryService categoryService;

    @Mock
    private OrderMapper orderMapper;

    @Mock
    private OrderItemMapper orderItemMapper;

    private MerchantDashboardServiceImpl merchantDashboardService;

    @BeforeEach
    void setUp() {
        // LambdaQueryWrapper 求值 SQL 片段时要查 MyBatis-Plus 的 TableInfo 缓存，
        // 而该缓存由 MyBatis 运行时在 Mapper 初始化时填充。本测试不启动 Spring 上下文，
        // 因此必须手动把 Product 的表信息注册进去——否则 getSqlSegment() 会抛
        // "can not find lambda cache for this entity"。
        // （OrderServiceImplTest 用的是字符串列名的 QueryWrapper，不触发 lambda 解析，故无此问题）
        initTableInfoCache(Product.class);

        merchantDashboardService = new MerchantDashboardServiceImpl(
                productService, categoryService, orderMapper, orderItemMapper);
        // 商家名下无商品：订单相关的统计（pending / today）会走空集合短路，
        // 本测试只关心商品维度的两项统计，不必为订单侧堆桩
        when(productService.list(ArgumentMatchers.<Wrapper<Product>>any())).thenReturn(List.of());
        when(productService.count(any())).thenReturn(0L);
        when(orderItemMapper.selectMerchantSales(any())).thenReturn(null);
    }

    @Test
    @DisplayName("低库存统计：只算在售商品，且阈值是 <= 而非 <")
    void lowStockCountCoversOnlyOnShelfProductsIncludingThreshold() {
        merchantDashboardService.getDashboard(3L);

        Wrapper<Product> lowStock = capturedProductCounts().get(1);

        assertThat(lowStock.getSqlSegment())
                .contains("merchant_id")
                // 少了这个条件，下架商品的低库存也会计入，商家会看到处理不掉的待办
                .contains("status =")
                // 边界必须是 <=：库存正好等于阈值的商品同样需要补货
                .contains("stock <=");
        assertThat(boundValues(lowStock))
                .containsValue(3L)
                .containsValue(ON_SHELF_STATUS)
                .containsValue(LOW_STOCK_THRESHOLD);
    }

    @Test
    @DisplayName("已下架统计：用不等于在售来收敛，而不是写死 status = 0")
    void offShelfCountUsesNotEqualOnShelf() {
        merchantDashboardService.getDashboard(3L);

        Wrapper<Product> offShelf = capturedProductCounts().get(2);

        assertThat(offShelf.getSqlSegment())
                .contains("merchant_id")
                .contains("status <>");
        assertThat(boundValues(offShelf))
                .containsValue(3L)
                .containsValue(ON_SHELF_STATUS);
    }

    @Test
    @DisplayName("仪表板返回低库存与已下架两项，且商品统计只查三次（没有多余的 count）")
    void dashboardExposesBothNewCounters() {
        Map<String, Object> stats = merchantDashboardService.getDashboard(3L);

        assertThat(stats).containsKeys("lowStockCount", "offShelfCount");
        // productCount / lowStockCount / offShelfCount 各一次；再多说明有重复查询
        verify(productService, times(3)).count(any());
    }

    // ---------- 辅助 ----------

    /**
     * 手动把实体的 TableInfo 注册进 MyBatis-Plus 的缓存，使 {@code LambdaQueryWrapper}
     * 能在无 Spring 上下文的测试里解析 {@code SFunction} 并生成 SQL 片段。
     */
    private static void initTableInfoCache(Class<?> entityClass) {
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(new MybatisConfiguration(), "");
        assistant.setCurrentNamespace(entityClass.getName());
        TableInfoHelper.initTableInfo(assistant, entityClass);
    }

    /** 按调用顺序取回三次 productService.count(...) 的 Wrapper */
    @SuppressWarnings("unchecked")
    private List<Wrapper<Product>> capturedProductCounts() {
        ArgumentCaptor<Wrapper<Product>> captor = ArgumentCaptor.forClass(Wrapper.class);
        verify(productService, times(3)).count(captor.capture());
        return captor.getAllValues();
    }

    /**
     * 取绑定参数。必须先取一次 SQL 片段——MyBatis-Plus 的条件是延迟渲染的，
     * 参数要到 {@code getSqlSegment()} 求值时才登记，直接读会拿到空 Map 让断言真空通过。
     * （与 {@link OrderServiceImplTest} 中的同名方法同因，那边有更详细的说明。）
     */
    private static Map<String, Object> boundValues(Wrapper<Product> wrapper) {
        wrapper.getSqlSegment();
        return ((AbstractWrapper<Product, String, ?>) wrapper).getParamNameValuePairs();
    }
}
