<template>
  <div class="dashboard">
    <!-- 统计卡片 -->
    <el-row :gutter="20" class="stats-row">
      <el-col :xs="24" :sm="12" :md="6">
        <div class="stat-card" style="--card-color: #667eea">
          <div class="stat-icon"><el-icon :size="28"><Document /></el-icon></div>
          <div class="stat-info">
            <div class="stat-value">{{ stats.totalOrderCount }}</div>
            <div class="stat-label">总订单数</div>
          </div>
        </div>
      </el-col>
      <el-col :xs="24" :sm="12" :md="6">
        <div class="stat-card" style="--card-color: #f093fb">
          <div class="stat-icon"><el-icon :size="28"><Clock /></el-icon></div>
          <div class="stat-info">
            <div class="stat-value">{{ stats.todayOrderCount }}</div>
            <div class="stat-label">今日订单</div>
          </div>
        </div>
      </el-col>
      <el-col :xs="24" :sm="12" :md="6">
        <div class="stat-card" style="--card-color: #4facfe">
          <div class="stat-icon"><el-icon :size="28"><Money /></el-icon></div>
          <div class="stat-info">
            <div class="stat-value">¥{{ formatNum(stats.totalSales) }}</div>
            <div class="stat-label">总销售额</div>
          </div>
        </div>
      </el-col>
      <el-col :xs="24" :sm="12" :md="6">
        <div class="stat-card" style="--card-color: #43e97b">
          <div class="stat-icon"><el-icon :size="28"><User /></el-icon></div>
          <div class="stat-info">
            <div class="stat-value">{{ stats.userCount }}</div>
            <div class="stat-label">用户总数</div>
          </div>
        </div>
      </el-col>
    </el-row>

    <el-row :gutter="20">
      <el-col :xs="24" :sm="12" :md="6">
        <div class="stat-card" style="--card-color: #fa709a">
          <div class="stat-icon"><el-icon :size="28"><Money /></el-icon></div>
          <div class="stat-info">
            <div class="stat-value">¥{{ formatNum(stats.todaySales) }}</div>
            <div class="stat-label">今日销售额</div>
          </div>
        </div>
      </el-col>
      <el-col :xs="24" :sm="12" :md="6">
        <div class="stat-card" style="--card-color: #fee140">
          <div class="stat-icon"><el-icon :size="28"><Goods /></el-icon></div>
          <div class="stat-info">
            <div class="stat-value">{{ stats.productCount }}</div>
            <div class="stat-label">商品总数</div>
          </div>
        </div>
      </el-col>
    </el-row>

    <!-- 图表区域 -->
    <el-row :gutter="20" style="margin-top: 20px">
      <el-col :xs="24" :md="16">
        <div class="chart-card">
          <div class="chart-header">
            <h3>销售趋势（近 7 天）</h3>
          </div>
          <div ref="salesChartRef" class="chart-body"></div>
        </div>
      </el-col>
      <el-col :xs="24" :md="8">
        <div class="chart-card">
          <div class="chart-header">
            <h3>热销商品 TOP 5</h3>
          </div>
          <div class="hot-list">
            <div
              v-for="(item, idx) in hotProducts"
              :key="idx"
              class="hot-item"
            >
              <span class="hot-rank" :class="'rank-' + (idx + 1)">{{ idx + 1 }}</span>
              <span class="hot-name">{{ item.name }}</span>
              <span class="hot-count">{{ item.salesCount }} 件</span>
            </div>
            <el-empty v-if="!hotProducts.length" description="暂无数据" :image-size="60" />
          </div>
        </div>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, onUnmounted, nextTick } from 'vue'
import echarts from '../../utils/echarts'
import { adminAPI } from '../../api/modules/admin'
import { Document, Clock, Money, User, Goods } from '@element-plus/icons-vue'

const salesChartRef = ref(null)
let chartInstance = null
let chartResizeObserver = null

const stats = reactive({
  totalOrderCount: 0, todayOrderCount: 0, totalSales: 0, todaySales: 0,
  userCount: 0, productCount: 0
})

const hotProducts = ref([])

const formatNum = (num) => (num || 0).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })

const loadData = async () => {
  try {
    const res = await adminAPI.dashboard.getStats()
    if (res.data?.data) Object.assign(stats, res.data.data)
    const hotRes = await adminAPI.dashboard.getHotProducts()
    hotProducts.value = hotRes.data?.data || []
  } catch (e) { console.error('加载仪表板失败:', e?.message) }
}

const loadChart = async () => {
  try {
    const res = await adminAPI.dashboard.getSalesTrend()
    // 后端返回最近 7 天数组：[{ date: 'yyyy-MM-dd', sales, orderCount }, ...]
    const rawData = res.data?.data || []
    const dates = rawData.map(i => i.date.slice(5)) // 截取 MM-DD
    const values = rawData.map(i => i.sales)
    renderChart(dates, values)
  } catch (e) { console.error('加载图表失败:', e?.message) }
}

const renderChart = (dates, values) => {
  if (!salesChartRef.value) return
  if (chartInstance) chartInstance.dispose()
  chartInstance = echarts.init(salesChartRef.value)
  chartInstance.setOption({
    tooltip: {
      trigger: 'axis',
      backgroundColor: '#fff',
      borderColor: '#e2e8f0',
      textStyle: { color: '#2d3748' },
      boxShadow: '0 4px 12px rgba(0,0,0,0.1)'
    },
    grid: { left: '3%', right: '4%', bottom: '3%', top: '10%', containLabel: true },
    xAxis: {
      type: 'category',
      data: dates,
      axisLine: { lineStyle: { color: '#e2e8f0' } },
      axisLabel: { color: '#a0aec0' }
    },
    yAxis: {
      type: 'value',
      name: '销售额 (元)',
      splitLine: { lineStyle: { color: '#f7fafc' } },
      axisLabel: { color: '#a0aec0' }
    },
    series: [{
      data: values,
      type: 'line',
      smooth: true,
      symbol: 'circle',
      symbolSize: 8,
      lineStyle: { color: '#667eea', width: 3 },
      itemStyle: { color: '#667eea' },
      areaStyle: {
        color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
          { offset: 0, color: 'rgba(102, 126, 234, 0.3)' },
          { offset: 1, color: 'rgba(102, 126, 234, 0.02)' }
        ])
      }
    }]
  })
  observeChartResize()
}

/**
 * 图表容器宽度会随「窗口跨断点」和「侧边栏折叠」变化，
 * 这两种情况都不会触发 window.resize，所以监听元素本身尺寸更可靠。
 * ECharts 的 canvas 一直贴合容器，resize 不会反向改变容器尺寸，不存在循环触发。
 */
const observeChartResize = () => {
  if (chartResizeObserver || !salesChartRef.value) return
  chartResizeObserver = new ResizeObserver(() => {
    if (chartInstance && !chartInstance.isDisposed()) chartInstance.resize()
  })
  chartResizeObserver.observe(salesChartRef.value)
}

onMounted(async () => {
  await loadData()
  await nextTick()
  await loadChart()
})

onUnmounted(() => {
  chartResizeObserver?.disconnect()
  chartResizeObserver = null
  if (chartInstance) chartInstance.dispose()
})
</script>

<style scoped>
.stats-row { margin-bottom: 0; }

.stat-card {
  background: #fff;
  border-radius: 12px;
  padding: 20px 24px;
  display: flex;
  align-items: center;
  gap: 16px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.06);
  border: 1px solid #edf2f7;
  transition: all 0.25s;
  margin-bottom: 20px;
  cursor: pointer;
  position: relative;
  overflow: hidden;
}

.stat-card::before {
  content: '';
  position: absolute;
  top: 0; left: 0;
  width: 4px; height: 100%;
  background: var(--card-color);
  border-radius: 4px 0 0 4px;
}

.stat-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.stat-icon {
  width: 52px; height: 52px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: color-mix(in srgb, var(--card-color) 12%, #fff);
  color: var(--card-color);
}

.stat-value {
  font-size: 24px;
  font-weight: 700;
  color: #1a202c;
  line-height: 1.2;
}

.stat-label {
  font-size: 13px;
  color: #a0aec0;
  margin-top: 2px;
}

/* 图表卡片 */
.chart-card {
  background: #fff;
  border-radius: 12px;
  padding: 20px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.06);
  border: 1px solid #edf2f7;
}

.chart-header {
  margin-bottom: 16px;
}

.chart-header h3 {
  font-size: 16px;
  font-weight: 600;
  color: #1a202c;
  margin: 0;
}

.chart-body {
  width: 100%;
  height: 320px;
}

/* 热销商品 */
.hot-list {
  padding: 8px 0;
}

.hot-item {
  display: flex;
  align-items: center;
  padding: 12px 0;
  border-bottom: 1px solid #f7fafc;
  gap: 12px;
}

.hot-item:last-child { border-bottom: none; }

.hot-rank {
  width: 24px; height: 24px;
  border-radius: 6px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  font-weight: 700;
  background: #edf2f7;
  color: #a0aec0;
}

.hot-rank.rank-1 { background: #fff3cd; color: #f59e0b; }
.hot-rank.rank-2 { background: #e2e8f0; color: #718096; }
.hot-rank.rank-3 { background: #fed7d7; color: #e53e3e; }

.hot-name {
  flex: 1;
  font-size: 14px;
  color: #2d3748;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.hot-count {
  font-size: 13px;
  color: #667eea;
  font-weight: 500;
}

/* ===== 响应式：断点取值见 composables/useBreakpoint.js ===== */

/* 移动端：一列布局下卡片同步收窄，避免"一屏只放得下一张卡"。
   图表高度不压缩：yAxis.name 靠 grid.top 的百分比（10%）预留绘制空间，
   高度一压，预留量随之缩水，轴名会被画到画布外裁掉。 */
@media (max-width: 640px) {
  .stat-card {
    padding: 16px;
  }

  .stat-value {
    font-size: 20px;
  }
}
</style>
