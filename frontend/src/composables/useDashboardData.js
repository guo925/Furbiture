import { computed, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Box, Goods, List, Money, Plus, Tickets, TrendCharts, User } from '@element-plus/icons-vue'
import { merchantAPI } from '../api'
import { useUserStore } from '../stores/user'
import { money } from '../utils/format'

/**
 * 商家仪表板数据与派生指标
 *
 * 从 MerchantDashboard.vue（原 865 行）抽离：数据加载、店铺信息、派生指标（健康分、
 * 各进度条比率、指标卡与待办清单）。视图层只负责把这里返回的响应式数据渲染成组件树。
 *
 * 数据口径一律来自后端真实统计接口，本文件不产出任何硬编码文案：
 * 待办清单的四项、健康分等级与健康项均由真实数据派生。
 */
export function useDashboardData() {
  const userStore = useUserStore()

  // —— 原始数据（来自后端真实统计接口）——
  const stats = ref({
    productCount: 0,
    pendingOrderCount: 0,
    totalSales: 0,
    todayOrderCount: 0,
    lowStockCount: 0,
    offShelfCount: 0
  })
  const revenueTrend = ref([])
  const categoryDistribution = ref([])
  const orderFunnel = ref({ pending: 0, paid: 0, delivered: 0, completed: 0 })
  const hotProducts = ref([])

  // —— 店铺信息 ——
  const shopName = computed(() => userStore.user?.username || '商家店铺')
  const shopInitial = computed(() => shopName.value.slice(0, 1).toUpperCase())

  /**
   * 资料待完善项数。
   *
   * 只算手机号与邮箱两个**商家自己可维护**的字段：username 不可修改（后端硬性拒绝，
   * 见 MerchantProfileController），把它算进来会得到一个商家永远无法消除的待办。
   * 数据取自 userStore 中的 UserVO，不额外发请求。
   */
  const profileMissingCount = computed(
    () => ['phone', 'email'].filter(field => !String(userStore.user?.[field] || '').trim()).length
  )

  // 订单漏斗总量（含全部有效状态）
  const funnelTotal = computed(
    () =>
      orderFunnel.value.pending +
      orderFunnel.value.paid +
      orderFunnel.value.delivered +
      orderFunnel.value.completed
  )

  // 商品供给分：以 20 个在售商品为满分
  const productScore = computed(() => Math.min(100, Math.round(stats.value.productCount * 5)))

  // 履约率：已发货 + 已完成 占漏斗总量之比
  const fulfillmentRate = computed(() => {
    const total = funnelTotal.value
    return total
      ? Math.round(((orderFunnel.value.delivered + orderFunnel.value.completed) / total) * 100)
      : 0
  })

  // 店铺健康分：履约率为主，商品供给为辅；无订单时仅按商品计。
  // ⚠️ 口径刻意不含"资料完整度"，因此面板副标题写的必须是履约与供给——别把文案写成
  // "基础项越完整分数越高"，那与公式对不上（本次修的就是这处矛盾）
  const healthScore = computed(() => {
    if (!funnelTotal.value) return Math.max(40, productScore.value)
    return Math.round(fulfillmentRate.value * 0.7 + productScore.value * 0.3)
  })

  /** 健康分等级文案：分数是会变的，文案必须跟着变，不能像以前那样恒为"良好" */
  const healthLevel = computed(() => {
    const score = healthScore.value
    if (score >= 85) return '经营状态优秀'
    if (score >= 70) return '经营状态良好'
    if (score >= 50) return '经营状态一般'
    return '经营状态待改善'
  })

  // 交易概览进度条：基于真实订单漏斗的占比
  const completeRate = computed(() => {
    const total = funnelTotal.value
    return total ? Math.round((orderFunnel.value.completed / total) * 100) : 0
  })
  // 商品供给率：归一化展示（20 个在售商品为满额）
  const productRate = computed(() =>
    Math.min(100, Math.round((stats.value.productCount / 20) * 100))
  )
  const pendingRate = computed(() => {
    const total = funnelTotal.value
    return total ? Math.round((orderFunnel.value.paid / total) * 100) : 0
  })

  /**
   * 健康分的构成项——把分数是怎么来的摊开给商家看。
   * 前两项正是 healthScore 公式的两个因子；第三项不计入分数，但同样需要商家处理。
   * 定义位置必须在 productRate 之后：它引用了 productRate。
   */
  const healthItems = computed(() => [
    {
      label: `履约率 ${fulfillmentRate.value}%`,
      tone: fulfillmentRate.value >= 50 ? 'ok' : 'warn'
    },
    {
      label: `商品供给 ${stats.value.productCount}/20 件`,
      tone: productRate.value >= 50 ? 'ok' : 'warn'
    },
    {
      label: `发货待办 ${orderFunnel.value.paid} 单`,
      tone: orderFunnel.value.paid ? 'warn' : 'ok'
    }
  ])

  const metrics = computed(() => [
    {
      label: '在售商品',
      value: stats.value.productCount,
      hint: '进入商品管理维护价格库存',
      to: '/merchant/products',
      icon: Goods,
      tone: 'blue'
    },
    {
      label: '待发货',
      value: stats.value.pendingOrderCount,
      hint: '买家已付款，建议优先处理',
      to: '/merchant/orders?status=1',
      icon: Box,
      tone: 'orange'
    },
    {
      label: '今日订单',
      value: stats.value.todayOrderCount,
      hint: '今日新增交易订单',
      to: '/merchant/orders',
      icon: TrendCharts,
      tone: 'green'
    },
    {
      label: '累计销售额',
      value: `¥${money(stats.value.totalSales)}`,
      hint: '已完成订单销售统计',
      to: '/merchant/orders',
      icon: Money,
      tone: 'purple'
    }
  ])

  /**
   * 交易待办：四项全部是真实统计数字。
   * 此前其中两项是假数据——「库存巡检」的值是字符串 `'巡检'`（一个词塞进了本该是数字的
   * 24px 大字位），「资料维护」的值恒为 `'1项'`。现已分别改为后端统计的低库存数量、
   * 已下架商品数量，资料一项由 profileMissingCount 实时计算。
   */
  const todos = computed(() => [
    { label: '待发货订单', value: orderFunnel.value.paid, to: '/merchant/orders?status=1' },
    { label: '库存告警', value: stats.value.lowStockCount, to: '/merchant/products' },
    { label: '已下架商品', value: stats.value.offShelfCount, to: '/merchant/products' },
    { label: '资料待完善', value: profileMissingCount.value, to: '/merchant/profile' }
  ])

  /**
   * 快捷入口。全部指向本项目真实存在的商家端页面。
   * 原有一项「运营设置」指向 `/merchant/profile`，既是「店铺资料」的重复目标，
   * 标签又承诺了一个并不存在的设置页 —— 已移除。
   */
  const shortcuts = [
    { label: '发布新品', to: '/merchant/products', icon: Plus },
    { label: '库存维护', to: '/merchant/products', icon: Box },
    { label: '处理发货', to: '/merchant/orders?status=1', icon: Tickets },
    { label: '分类整理', to: '/merchant/categories', icon: List },
    { label: '店铺资料', to: '/merchant/profile', icon: User }
  ]

  /**
   * 拉取全部统计数据。
   * 图表渲染由各图表子组件自行监听数据变化完成，此处不再直接操作 ECharts 实例。
   */
  const loadStats = async () => {
    try {
      const [statsRes, trendRes, categoryRes, funnelRes, hotRes] = await Promise.all([
        merchantAPI.dashboard.getStats(),
        merchantAPI.dashboard.getRevenueTrend(),
        merchantAPI.dashboard.getCategoryDistribution(),
        merchantAPI.dashboard.getOrderFunnel(),
        merchantAPI.dashboard.getTopProducts()
      ])
      // 合并而非整体替换：后端若少返回某个字段（如新增统计尚未上线），
      // 对应项会保留上面的 0 默认值，而不是变成 undefined 在待办里渲染成空白
      stats.value = { ...stats.value, ...(statsRes.data.data || {}) }
      revenueTrend.value = trendRes.data.data || []
      categoryDistribution.value = categoryRes.data.data || []
      orderFunnel.value = funnelRes.data.data || orderFunnel.value
      hotProducts.value = hotRes.data.data || []
    } catch (error) {
      ElMessage.error(error.response?.data?.msg || '获取统计数据失败')
    }
  }

  return {
    stats,
    revenueTrend,
    categoryDistribution,
    orderFunnel,
    hotProducts,
    shopName,
    shopInitial,
    healthScore,
    healthLevel,
    healthItems,
    profileMissingCount,
    completeRate,
    productRate,
    pendingRate,
    metrics,
    todos,
    shortcuts,
    loadStats
  }
}
