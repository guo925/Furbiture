<template>
  <div class="dashboard">
    <h1>管理控制台</h1>
    
    <div class="stats-grid">
      <el-card class="stat-card" @click="goToOrders">
        <div class="stat-content">
          <div class="stat-number">{{ stats.totalOrderCount }}</div>
          <div class="stat-label">总订单数</div>
          <div class="stat-action">查看详情 →</div>
        </div>
      </el-card>
      <el-card class="stat-card" @click="goToOrders(true)">
        <div class="stat-content">
          <div class="stat-number">{{ stats.todayOrderCount }}</div>
          <div class="stat-label">今日订单</div>
          <div class="stat-action">查看详情 →</div>
        </div>
      </el-card>
      <el-card class="stat-card" @click="goToOrders">
        <div class="stat-content">
          <div class="stat-number">¥{{ stats.totalSales ? stats.totalSales.toFixed(2) : '0.00' }}</div>
          <div class="stat-label">总销售额</div>
          <div class="stat-action">查看详情 →</div>
        </div>
      </el-card>
      <el-card class="stat-card" @click="goToOrders(true)">
        <div class="stat-content">
          <div class="stat-number">¥{{ stats.todaySales ? stats.todaySales.toFixed(2) : '0.00' }}</div>
          <div class="stat-label">今日销售额</div>
          <div class="stat-action">查看详情 →</div>
        </div>
      </el-card>
      <el-card class="stat-card" @click="goToUsers">
        <div class="stat-content">
          <div class="stat-number">{{ stats.userCount }}</div>
          <div class="stat-label">用户总数</div>
          <div class="stat-action">查看详情 →</div>
        </div>
      </el-card>
      <el-card class="stat-card" @click="goToProducts">
        <div class="stat-content">
          <div class="stat-number">{{ stats.productCount }}</div>
          <div class="stat-label">商品总数</div>
          <div class="stat-action">查看详情 →</div>
        </div>
      </el-card>
    </div>

    <div class="dashboard-charts">
      <el-card class="chart-card">
        <template #header>
          <div class="card-header">
            <span>销售趋势</span>
          </div>
        </template>
        <div class="chart-content">
          <div ref="salesChartRef" class="chart-container"></div>
        </div>
      </el-card>
      <el-card class="chart-card">
        <template #header>
          <div class="card-header">
            <span>热门商品</span>
          </div>
        </template>
        <div class="chart-content">
          <el-table :data="hotProducts" style="width: 100%">
            <el-table-column prop="name" label="商品名称" />
            <el-table-column prop="salesCount" label="销售数量" width="100" />
            <el-table-column prop="price" label="单价" width="100">
              <template #default="scope">
                ¥{{ scope.row.price.toFixed(2) }}
              </template>
            </el-table-column>
          </el-table>
        </div>
      </el-card>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '../../stores/user'
import { adminAPI } from '../../api'
import * as echarts from 'echarts'

const router = useRouter()
const userStore = useUserStore()
const stats = ref({
  productCount: 0,
  totalOrderCount: 0,
  todayOrderCount: 0,
  totalSales: 0,
  todaySales: 0,
  userCount: 0
})
const salesTrend = ref([])
const hotProducts = ref([])
const salesChartRef = ref(null)
let salesChart = null

let isMounted = true

onMounted(async () => {
  isMounted = true
  await loadStats()
  await loadSalesTrend()
  await loadHotProducts()
  // 初始化销售趋势图表
  setTimeout(() => {
    initSalesChart()
  }, 100)
})

onUnmounted(() => {
  isMounted = false
  // 销毁图表实例
  if (salesChart) {
    salesChart.dispose()
    salesChart = null
  }
})

// 监听销售趋势数据变化，更新图表
watch(salesTrend, () => {
  if (salesChart) {
    updateSalesChart()
  }
}, { deep: true })

const loadStats = async () => {
  try {
    const response = await adminAPI.dashboard.getStats()
    if (isMounted) {
      stats.value = response.data.data
      // 打印总订单数到控制台
      console.log('总订单数:', stats.value.totalOrderCount)
    }
  } catch (error) {
    console.error('获取统计数据失败:', error)
  }
}

const loadSalesTrend = async () => {
  try {
    const response = await adminAPI.dashboard.getSalesTrend()
    if (isMounted) {
      salesTrend.value = response.data.data || []
    }
  } catch (error) {
    console.error('获取销售趋势失败:', error)
    // 使用模拟数据
    if (isMounted) {
      salesTrend.value = [
        { date: '2026-04-15', sales: 1200.00, orderCount: 5 },
        { date: '2026-04-16', sales: 1500.00, orderCount: 7 },
        { date: '2026-04-17', sales: 1800.00, orderCount: 8 },
        { date: '2026-04-18', sales: 1300.00, orderCount: 6 },
        { date: '2026-04-19', sales: 2000.00, orderCount: 10 },
        { date: '2026-04-20', sales: 1600.00, orderCount: 7 },
        { date: '2026-04-21', sales: 1900.00, orderCount: 9 }
      ]
    }
  }
}

const loadHotProducts = async () => {
  try {
    const response = await adminAPI.dashboard.getHotProducts()
    if (isMounted) {
      hotProducts.value = response.data.data || []
    }
  } catch (error) {
    console.error('获取热门商品失败:', error)
    // 使用模拟数据
    if (isMounted) {
      hotProducts.value = [
        { id: 1, name: '现代简约沙发', salesCount: 150, price: 2999.00 },
        { id: 2, name: '北欧风格茶几', salesCount: 120, price: 899.00 },
        { id: 3, name: '实木餐桌', salesCount: 90, price: 1999.00 },
        { id: 4, name: '舒适床垫', salesCount: 80, price: 1299.00 },
        { id: 5, name: '现代办公椅', salesCount: 70, price: 599.00 }
      ]
    }
  }
}

// 初始化销售趋势图表
const initSalesChart = () => {
  if (!salesChartRef.value) return
  
  salesChart = echarts.init(salesChartRef.value)
  updateSalesChart()
  
  // 监听窗口大小变化，调整图表大小
  window.addEventListener('resize', () => {
    salesChart.resize()
  })
}

// 更新销售趋势图表
const updateSalesChart = () => {
  if (!salesChart) return
  
  const dates = salesTrend.value.map(item => item.date)
  const salesData = salesTrend.value.map(item => item.sales)
  const orderCountData = salesTrend.value.map(item => item.orderCount)
  
  const option = {
    tooltip: {
      trigger: 'axis',
      axisPointer: {
        type: 'cross',
        label: {
          backgroundColor: '#6a7985'
        }
      }
    },
    legend: {
      data: ['销售额', '订单数量']
    },
    grid: {
      left: '3%',
      right: '4%',
      bottom: '3%',
      containLabel: true
    },
    xAxis: [
      {
        type: 'category',
        boundaryGap: false,
        data: dates
      }
    ],
    yAxis: [
      {
        type: 'value',
        name: '销售额',
        axisLabel: {
          formatter: '¥{value}'
        }
      },
      {
        type: 'value',
        name: '订单数量',
        axisLabel: {
          formatter: '{value}单'
        }
      }
    ],
    series: [
      {
        name: '销售额',
        type: 'line',
        stack: 'Total',
        areaStyle: {
          opacity: 0.3
        },
        data: salesData
      },
      {
        name: '订单数量',
        type: 'line',
        yAxisIndex: 1,
        data: orderCountData
      }
    ]
  }
  
  salesChart.setOption(option)
}

// 跳转到商品管理页面
const goToProducts = () => {
  router.push('/admin/products')
}

// 跳转到订单管理页面
const goToOrders = (isToday = false) => {
  if (isToday) {
    const today = new Date()
    const year = today.getFullYear()
    const month = String(today.getMonth() + 1).padStart(2, '0')
    const day = String(today.getDate()).padStart(2, '0')
    const startDate = `${year}-${month}-${day}`
    const endDate = startDate
    router.push({
      path: '/admin/orders',
      query: { startDate, endDate }
    })
  } else {
    router.push('/admin/orders')
  }
}

// 跳转到用户管理页面
const goToUsers = () => {
  router.push('/admin/users')
}
</script>

<style scoped>
.dashboard {
  padding: 20px;
}

.dashboard-header {
  margin-bottom: 30px;
}

.dashboard-header h2 {
  margin: 0 0 10px 0;
  font-size: 24px;
  font-weight: bold;
  color: #333;
}

.dashboard-header p {
  margin: 0;
  color: #666;
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  grid-template-rows: repeat(2, 1fr);
  gap: 20px;
  margin-bottom: 30px;
}

.stat-card {
  aspect-ratio: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.3s;
  border-radius: 8px;
}

.stat-card:hover {
  transform: translateY(-5px);
  box-shadow: 0 4px 16px rgba(0,0,0,0.15);
}

.stat-content {
  text-align: center;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
}

.stat-number {
  font-size: 32px;
  font-weight: bold;
  color: #409eff;
  margin-bottom: 8px;
}

.stat-label {
  font-size: 12px;
  color: #666;
  margin-bottom: 8px;
}

.stat-action {
  font-size: 12px;
  color: #409eff;
  opacity: 0;
  transition: opacity 0.3s;
}

.stat-card:hover .stat-action {
  opacity: 1;
}

.dashboard-charts {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 20px;
}

.chart-card {
  height: 400px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.chart-content {
  height: 250px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.chart-container {
  width: 100%;
  height: 100%;
}
</style>