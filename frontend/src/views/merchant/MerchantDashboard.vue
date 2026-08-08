<template>
  <div>
    <div class="seller-dashboard">
      <section class="overview-panel">
        <div class="shop-card">
          <div class="shop-top">
            <div class="shop-avatar">{{ shopInitial }}</div>
            <div>
              <span class="eyebrow">店铺概览</span>
              <h2>{{ shopName }}</h2>
              <p>今天重点处理待发货订单、库存巡检和商品曝光维护。</p>
            </div>
          </div>
          <div class="shop-actions">
            <el-button type="primary" color="#ff7a1a" @click="router.push('/merchant/products')">
              <el-icon><Plus /></el-icon>
              发布商品
            </el-button>
            <el-button @click="router.push('/merchant/orders')">
              <el-icon><Tickets /></el-icon>
              处理订单
            </el-button>
          </div>
        </div>

        <div class="notice-card">
          <div class="notice-title">
            <el-icon><Bell /></el-icon>
            <span>平台提醒</span>
          </div>
          <p>保持商品图片、库存和发货时效完整，可提升买家下单转化。</p>
          <router-link to="/merchant/profile">完善店铺资料</router-link>
        </div>
      </section>

      <section class="metric-grid">
        <router-link v-for="item in metrics" :key="item.label" :to="item.to" class="metric-card">
          <span class="metric-icon" :class="item.tone">
            <el-icon><component :is="item.icon" /></el-icon>
          </span>
          <span class="metric-label">{{ item.label }}</span>
          <strong>{{ item.value }}</strong>
          <small>{{ item.hint }}</small>
        </router-link>
      </section>

      <section class="dashboard-columns">
        <div class="work-panel todo-panel">
          <div class="panel-head">
            <div>
              <h3>交易待办</h3>
              <p>按买家履约优先级处理</p>
            </div>
            <router-link to="/merchant/orders">全部订单</router-link>
          </div>
          <div class="todo-list">
            <router-link v-for="todo in todos" :key="todo.label" :to="todo.to" class="todo-item">
              <span>
                <b>{{ todo.value }}</b>
                <small>{{ todo.label }}</small>
              </span>
              <el-icon><ArrowRight /></el-icon>
            </router-link>
          </div>
        </div>

        <div class="work-panel health-panel">
          <div class="panel-head">
            <div>
              <h3>店铺健康</h3>
              <p>基础项越完整，买家信任越高</p>
            </div>
          </div>
          <div class="health-score">
            <el-progress type="dashboard" :percentage="healthScore" :width="118" color="#ff7a1a" />
            <div>
              <strong>{{ healthScore }}分</strong>
              <span>经营状态良好</span>
            </div>
          </div>
          <div class="health-list">
            <span><i class="ok"></i> 商品可售状态正常</span>
            <span><i class="ok"></i> 店铺资料可维护</span>
            <span><i :class="orderFunnel.paid ? 'warn' : 'ok'"></i> 发货待办 {{ orderFunnel.paid }} 单</span>
          </div>
        </div>
      </section>

      <section class="dashboard-columns bottom-columns">
        <div class="work-panel shortcut-panel">
          <div class="panel-head">
            <div>
              <h3>常用工具</h3>
              <p>高频经营动作快速进入</p>
            </div>
          </div>
          <div class="shortcut-grid">
            <router-link v-for="shortcut in shortcuts" :key="shortcut.label" :to="shortcut.to">
              <el-icon><component :is="shortcut.icon" /></el-icon>
              <span>{{ shortcut.label }}</span>
            </router-link>
          </div>
        </div>

        <div class="work-panel trend-panel">
          <div class="panel-head">
            <div>
              <h3>交易概览</h3>
              <p>实时统计当前店铺交易结果</p>
            </div>
          </div>
          <div class="trend-bars">
            <div class="trend-row">
              <span>订单完成</span>
              <div><i :style="{ width: completeRate + '%' }"></i></div>
              <b>{{ orderFunnel.completed }}</b>
            </div>
            <div class="trend-row">
              <span>商品供给</span>
              <div><i :style="{ width: productRate + '%' }"></i></div>
              <b>{{ stats.productCount }}</b>
            </div>
            <div class="trend-row">
              <span>发货压力</span>
              <div><i class="danger" :style="{ width: pendingRate + '%' }"></i></div>
              <b>{{ orderFunnel.paid }}</b>
            </div>
          </div>
        </div>
      </section>

      <section class="chart-grid">
        <div class="work-panel chart-panel">
          <div class="panel-head">
            <div>
              <h3>营收趋势</h3>
              <p>近 7 天销售额走势</p>
            </div>
          </div>
          <div ref="revenueChartRef" class="chart-box"></div>
        </div>
        <div class="work-panel chart-panel">
          <div class="panel-head">
            <div>
              <h3>品类分布</h3>
              <p>在售商品分类占比</p>
            </div>
          </div>
          <div v-if="!categoryDistribution.length" class="chart-empty">
            <el-empty description="暂无数据" :image-size="60" />
          </div>
          <div v-else ref="categoryChartRef" class="chart-box"></div>
        </div>
      </section>

      <section class="work-panel hot-panel">
        <div class="panel-head">
          <div>
            <h3>热销商品</h3>
            <p>按销量排序 TOP 5</p>
          </div>
        </div>
        <div class="hot-list">
          <div v-for="(item, idx) in hotProducts.slice(0, 5)" :key="idx" class="hot-item">
            <span class="hot-rank" :class="'rank-' + (idx + 1)">{{ idx + 1 }}</span>
            <span class="hot-name">{{ item.name }}</span>
            <span class="hot-count">{{ item.salesCount }} 件</span>
          </div>
          <el-empty v-if="!hotProducts.length" description="暂无数据" :image-size="60" />
        </div>
      </section>
    </div>
  </div>
</template>

<script setup>
import { computed, nextTick, onMounted, onUnmounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import * as echarts from 'echarts'
import { ElMessage } from 'element-plus'
import {
  ArrowRight,
  Bell,
  Box,
  Goods,
  List,
  Money,
  Plus,
  Setting,
  Tickets,
  TrendCharts,
  User
} from '@element-plus/icons-vue'
import { merchantAPI } from '../../api'
import { useUserStore } from '../../stores/user'

const router = useRouter()
const userStore = useUserStore()

const stats = ref({
  productCount: 0,
  pendingOrderCount: 0,
  totalSales: 0,
  todayOrderCount: 0
})

// 图表数据（来自后端真实统计接口）
const revenueTrend = ref([])
const categoryDistribution = ref([])
const orderFunnel = ref({ pending: 0, paid: 0, delivered: 0, completed: 0 })
const hotProducts = ref([])

const revenueChartRef = ref(null)
const categoryChartRef = ref(null)
let revenueChart = null
let categoryChart = null

const shopName = computed(() => userStore.user?.username || '商家店铺')
const shopInitial = computed(() => shopName.value.slice(0, 1).toUpperCase())
const lowStockHint = computed(() => stats.value.productCount > 0 ? '巡检' : '0')

// 订单漏斗总量（含全部有效状态）
const funnelTotal = computed(() =>
  orderFunnel.value.pending + orderFunnel.value.paid + orderFunnel.value.delivered + orderFunnel.value.completed)

// 商品供给分：以 20 个在售商品为满分
const productScore = computed(() => Math.min(100, Math.round(stats.value.productCount * 5)))

// 店铺健康分：履约率（已发货+已完成占比）为主，商品供给为辅；无订单时仅按商品计
const healthScore = computed(() => {
  const total = funnelTotal.value
  if (!total) return Math.max(40, productScore.value)
  const fulfillment = (orderFunnel.value.delivered + orderFunnel.value.completed) / total * 100
  return Math.round(fulfillment * 0.7 + productScore.value * 0.3)
})

// 交易概览进度条：基于真实订单漏斗的占比
const completeRate = computed(() => {
  const total = funnelTotal.value
  return total ? Math.round(orderFunnel.value.completed / total * 100) : 0
})
// 商品供给率：归一化展示（20 个在售商品为满额）
const productRate = computed(() => Math.min(100, Math.round(stats.value.productCount / 20 * 100)))
const pendingRate = computed(() => {
  const total = funnelTotal.value
  return total ? Math.round(orderFunnel.value.paid / total * 100) : 0
})

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

const todos = computed(() => [
  { label: '待发货订单', value: orderFunnel.value.paid, to: '/merchant/orders?status=1' },
  { label: '库存巡检', value: lowStockHint.value, to: '/merchant/products' },
  { label: '商品发布', value: stats.value.productCount ? '继续上新' : '去发布', to: '/merchant/products' },
  { label: '资料维护', value: '1项', to: '/merchant/profile' }
])

const shortcuts = [
  { label: '发布新品', to: '/merchant/products', icon: Plus },
  { label: '库存维护', to: '/merchant/products', icon: Box },
  { label: '处理发货', to: '/merchant/orders?status=1', icon: Tickets },
  { label: '分类整理', to: '/merchant/categories', icon: List },
  { label: '店铺资料', to: '/merchant/profile', icon: User },
  { label: '运营设置', to: '/merchant/profile', icon: Setting }
]

const loadStats = async () => {
  try {
    const [statsRes, trendRes, categoryRes, funnelRes, hotRes] = await Promise.all([
      merchantAPI.dashboard.getStats(),
      merchantAPI.dashboard.getRevenueTrend(),
      merchantAPI.dashboard.getCategoryDistribution(),
      merchantAPI.dashboard.getOrderFunnel(),
      merchantAPI.dashboard.getTopProducts()
    ])
    stats.value = statsRes.data.data || stats.value
    revenueTrend.value = trendRes.data.data || []
    categoryDistribution.value = categoryRes.data.data || []
    orderFunnel.value = funnelRes.data.data || orderFunnel.value
    hotProducts.value = hotRes.data.data || []
    await nextTick()
    renderCharts()
  } catch (error) {
    ElMessage.error(error.response?.data?.msg || '获取统计数据失败')
  }
}

const renderCharts = () => {
  renderRevenueChart()
  renderCategoryChart()
}

const renderRevenueChart = () => {
  if (!revenueChartRef.value) return
  if (revenueChart) revenueChart.dispose()
  revenueChart = echarts.init(revenueChartRef.value)
  revenueChart.setOption({
    tooltip: {
      trigger: 'axis',
      backgroundColor: '#fff',
      borderColor: '#e8ecf3',
      textStyle: { color: '#17202b' }
    },
    grid: { left: '3%', right: '4%', bottom: '3%', top: '10%', containLabel: true },
    xAxis: {
      type: 'category',
      data: revenueTrend.value.map(i => i.date),
      axisLine: { lineStyle: { color: '#e8ecf3' } },
      axisLabel: { color: '#8a94a6' }
    },
    yAxis: {
      type: 'value',
      name: '销售额 (元)',
      splitLine: { lineStyle: { color: '#f7f9fc' } },
      axisLabel: { color: '#8a94a6' }
    },
    series: [{
      name: '销售额',
      type: 'line',
      smooth: true,
      symbol: 'circle',
      symbolSize: 7,
      data: revenueTrend.value.map(i => i.sales),
      lineStyle: { color: '#ff7a1a', width: 3 },
      itemStyle: { color: '#ff7a1a' },
      areaStyle: {
        color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
          { offset: 0, color: 'rgba(255, 122, 26, 0.25)' },
          { offset: 1, color: 'rgba(255, 122, 26, 0.02)' }
        ])
      }
    }]
  })
}

const renderCategoryChart = () => {
  if (!categoryChartRef.value) return
  if (categoryChart) categoryChart.dispose()
  categoryChart = echarts.init(categoryChartRef.value)
  categoryChart.setOption({
    tooltip: { trigger: 'item', formatter: '{b}: {c} ({d}%)' },
    legend: { bottom: 0, textStyle: { color: '#667085' } },
    color: ['#ff7a1a', '#f6ad55', '#2563eb', '#16a34a', '#7c3aed', '#ef4444', '#0ea5e9', '#f59e0b'],
    series: [{
      type: 'pie',
      radius: ['40%', '68%'],
      center: ['50%', '44%'],
      avoidLabelOverlap: true,
      itemStyle: { borderRadius: 6, borderColor: '#fff', borderWidth: 2 },
      label: { formatter: '{b}\n{c}件' },
      labelLine: { length: 10, length2: 8 },
      data: categoryDistribution.value.map(i => ({ name: i.name, value: i.value }))
    }]
  })
}

const handleResize = () => {
  revenueChart?.resize()
  categoryChart?.resize()
}

onMounted(async () => {
  if (!userStore.isAuthenticated) {
    router.push('/login')
    return
  }
  window.addEventListener('resize', handleResize)
  await loadStats()
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  if (revenueChart) revenueChart.dispose()
  if (categoryChart) categoryChart.dispose()
})

const money = value => Number(value || 0).toFixed(2)
</script>

<style scoped>
.seller-dashboard {
  display: grid;
  gap: 18px;
}

.overview-panel,
.dashboard-columns {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 360px;
  gap: 18px;
}

.shop-card,
.notice-card,
.metric-card,
.work-panel {
  background: rgba(255, 255, 255, 0.96);
  border: 1px solid #e8ecf3;
  border-radius: 10px;
  box-shadow: 0 12px 28px rgba(17, 24, 39, 0.04);
}

.shop-card {
  min-height: 176px;
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 24px;
  padding: 26px;
  background:
    radial-gradient(circle at right top, rgba(255, 122, 26, 0.12), transparent 32%),
    linear-gradient(135deg, #ffffff 0%, #fff9f3 100%);
}

.shop-top {
  display: flex;
  align-items: flex-start;
  gap: 16px;
}

.shop-avatar {
  width: 58px;
  height: 58px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex: 0 0 auto;
  border-radius: 14px;
  background: linear-gradient(135deg, #ff7a1a, #ffae63);
  color: #fff;
  font-size: 24px;
  font-weight: 800;
}

.eyebrow {
  color: #ff7a1a;
  font-size: 13px;
  font-weight: 700;
}

.shop-card h2 {
  margin: 8px 0 10px;
  color: #17202b;
  font-size: 28px;
}

.shop-card p,
.notice-card p,
.panel-head p {
  margin: 0;
  color: #6b7280;
  line-height: 1.6;
}

.shop-actions {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

.notice-card {
  padding: 22px;
}

.notice-title {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 12px;
  color: #17202b;
  font-weight: 700;
}

.notice-card a,
.panel-head a {
  display: inline-flex;
  margin-top: 14px;
  color: #ff7a1a;
  text-decoration: none;
  font-weight: 600;
}

.metric-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 14px;
}

.metric-card {
  position: relative;
  display: grid;
  gap: 8px;
  min-height: 142px;
  padding: 18px;
  color: inherit;
  text-decoration: none;
  overflow: hidden;
  transition: transform 0.16s ease, box-shadow 0.16s ease;
}

.metric-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 16px 32px rgba(17, 24, 39, 0.08);
}

.metric-icon {
  width: 36px;
  height: 36px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: 9px;
  font-size: 18px;
}

.metric-icon.blue {
  color: #2563eb;
  background: #eff6ff;
}

.metric-icon.orange {
  color: #ea580c;
  background: #fff7ed;
}

.metric-icon.green {
  color: #16a34a;
  background: #f0fdf4;
}

.metric-icon.purple {
  color: #7c3aed;
  background: #f5f3ff;
}

.metric-label {
  color: #667085;
  font-size: 13px;
}

.metric-card strong {
  color: #111827;
  font-size: 28px;
  line-height: 1;
}

.metric-card small {
  color: #8a94a6;
}

.work-panel {
  padding: 20px;
}

.panel-head {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 16px;
  margin-bottom: 16px;
}

.panel-head h3 {
  margin: 0 0 4px;
  color: #17202b;
  font-size: 18px;
}

.todo-list {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.todo-item {
  min-height: 86px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  padding: 16px;
  border-radius: 8px;
  background: #f7f9fc;
  color: inherit;
  text-decoration: none;
  border: 1px solid transparent;
}

.todo-item:hover {
  border-color: #ffd7b3;
  background: #fffaf5;
}

.todo-item span {
  display: grid;
  gap: 6px;
}

.todo-item b {
  color: #ff7a1a;
  font-size: 24px;
  line-height: 1;
}

.todo-item small {
  color: #667085;
}

.health-score {
  display: flex;
  align-items: center;
  gap: 18px;
  padding: 4px 0 14px;
}

.health-score div {
  display: grid;
  gap: 6px;
}

.health-score strong {
  color: #17202b;
  font-size: 24px;
}

.health-score span {
  color: #6b7280;
}

.health-list {
  display: grid;
  gap: 10px;
  color: #4b5563;
}

.health-list span {
  display: flex;
  align-items: center;
  gap: 8px;
}

.health-list i {
  width: 8px;
  height: 8px;
  border-radius: 50%;
}

.health-list .ok {
  background: #22c55e;
}

.health-list .warn {
  background: #ff7a1a;
}

.bottom-columns {
  grid-template-columns: minmax(0, 1fr) minmax(360px, 0.62fr);
}

/* 图表区域 */
.chart-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.4fr) minmax(0, 1fr);
  gap: 18px;
}

.chart-panel .chart-box {
  width: 100%;
  height: 280px;
}

.chart-empty {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 280px;
}

/* 热销商品 */
.hot-panel {
  margin-top: 18px;
}

.hot-list {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 14px;
  padding: 6px 0;
}

.hot-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 14px;
  border-radius: 8px;
  background: #f7f9fc;
}

.hot-rank {
  width: 24px;
  height: 24px;
  border-radius: 6px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  font-weight: 700;
  background: #edf1f7;
  color: #8a94a6;
  flex: 0 0 auto;
}

.hot-rank.rank-1 { background: #fff3cd; color: #f59e0b; }
.hot-rank.rank-2 { background: #e2e8f0; color: #718096; }
.hot-rank.rank-3 { background: #fed7d7; color: #e53e3e; }

.hot-name {
  flex: 1;
  font-size: 13px;
  color: #344054;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.hot-count {
  font-size: 12px;
  color: #ff7a1a;
  font-weight: 600;
  flex: 0 0 auto;
}

.shortcut-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
}

.shortcut-grid a {
  min-height: 84px;
  display: grid;
  place-items: center;
  gap: 8px;
  padding: 12px;
  border-radius: 8px;
  background: #f7f9fc;
  color: #344054;
  text-decoration: none;
  font-weight: 600;
}

.shortcut-grid a:hover {
  color: #ff7a1a;
  background: #fff7ed;
}

.shortcut-grid .el-icon {
  font-size: 22px;
}

.trend-bars {
  display: grid;
  gap: 18px;
  padding-top: 6px;
}

.trend-row {
  display: grid;
  grid-template-columns: 72px minmax(0, 1fr) 44px;
  align-items: center;
  gap: 12px;
  color: #667085;
}

.trend-row div {
  height: 9px;
  border-radius: 999px;
  background: #edf1f7;
  overflow: hidden;
}

.trend-row i {
  display: block;
  height: 100%;
  border-radius: inherit;
  background: linear-gradient(90deg, #ff7a1a, #ffb15d);
}

.trend-row i.danger {
  background: linear-gradient(90deg, #ef4444, #fb923c);
}

.trend-row b {
  color: #17202b;
  text-align: right;
}

@media (max-width: 1160px) {
  .overview-panel,
  .dashboard-columns,
  .bottom-columns,
  .chart-grid {
    grid-template-columns: 1fr;
  }

  .metric-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .hot-list {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 720px) {
  .shop-card {
    flex-direction: column;
  }

  .shop-top {
    flex-direction: column;
  }

  .metric-grid,
  .todo-list,
  .shortcut-grid,
  .hot-list {
    grid-template-columns: 1fr;
  }
}
</style>
