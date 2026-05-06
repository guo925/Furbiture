<template>
  <div class="merchant-orders">
    <!-- 导航栏 -->
    <header class="navbar">
      <div class="container">
        <div class="logo">
          <router-link to="/merchant">家具商城-商家管理</router-link>
        </div>
        <nav class="nav">
          <router-link to="/merchant" class="nav-item">首页</router-link>
          <router-link to="/merchant/products" class="nav-item">商品管理</router-link>
          <router-link to="/merchant/categories" class="nav-item">商品分类</router-link>
          <router-link to="/merchant/orders" class="nav-item">订单管理</router-link>
        </nav>
        <div class="user">
          <template v-if="userStore.isAuthenticated">
            <span class="welcome">欢迎, {{ userStore.user.username }}</span>
            <button @click="logout" class="btn">退出</button>
          </template>
        </div>
      </div>
    </header>

    <!-- 内容区域 -->
    <div class="content">
      <div class="container">
        <h1 class="page-title">订单管理</h1>

        <!-- 订单筛选 -->
        <div class="filter-section">
          <el-select v-model="filterStatus" placeholder="订单状态" clearable @change="loadOrders">
            <el-option label="全部" :value="null" />
            <el-option label="待付款" :value="0" />
            <el-option label="已付款" :value="1" />
            <el-option label="已发货" :value="2" />
            <el-option label="已完成" :value="3" />
            <el-option label="已取消" :value="4" />
          </el-select>
        </div>

        <!-- 订单列表 -->
        <div class="order-list" v-if="orders.length > 0">
          <div v-for="order in orders" :key="order.id" class="order-item">
            <div class="order-header">
              <div class="order-no">订单号: {{ order.orderNo }}</div>
              <div class="order-status">
                <el-tag :type="getStatusType(order.status)">
                  {{ getStatusText(order.status) }}
                </el-tag>
              </div>
            </div>
            <div class="order-info">
              <div class="order-amount">¥{{ order.totalAmount }}</div>
              <div class="order-time">{{ formatTime(order.createTime) }}</div>
            </div>
            <div class="order-actions">
              <el-button @click="handleDetail(order.orderNo)" size="small">查看详情</el-button>
              <el-button
                v-if="order.status === 1"
                type="primary"
                @click="handleShip(order.orderNo)"
                size="small"
              >
                发货
              </el-button>
            </div>
          </div>
        </div>

        <el-empty v-else description="暂无订单" />

        <!-- 分页 -->
        <el-pagination
          v-if="total > 0"
          v-model:current-page="currentPage"
          :page-size="pageSize"
          :total="total"
          layout="prev, pager, next"
          @current-change="loadOrders"
          class="pagination"
        />
      </div>
    </div>

    <!-- 订单详情对话框 -->
    <el-dialog v-model="detailDialogVisible" title="订单详情" width="700px">
      <div v-if="currentOrder" class="order-detail">
        <div class="detail-section">
          <h3>订单信息</h3>
          <div class="detail-row">
            <span class="label">订单号:</span>
            <span class="value">{{ currentOrder.orderNo }}</span>
          </div>
          <div class="detail-row">
            <span class="label">订单状态:</span>
            <el-tag :type="getStatusType(currentOrder.status)">
              {{ getStatusText(currentOrder.status) }}
            </el-tag>
          </div>
          <div class="detail-row">
            <span class="label">总金额:</span>
            <span class="value price">¥{{ currentOrder.totalAmount }}</span>
          </div>
          <div class="detail-row">
            <span class="label">下单时间:</span>
            <span class="value">{{ formatTime(currentOrder.createTime) }}</span>
          </div>
          <div class="detail-row" v-if="currentOrder.payTime">
            <span class="label">支付时间:</span>
            <span class="value">{{ formatTime(currentOrder.payTime) }}</span>
          </div>
          <div class="detail-row" v-if="currentOrder.deliveryTime">
            <span class="label">发货时间:</span>
            <span class="value">{{ formatTime(currentOrder.deliveryTime) }}</span>
          </div>
          <div class="detail-row" v-if="currentOrder.finishTime">
            <span class="label">完成时间:</span>
            <span class="value">{{ formatTime(currentOrder.finishTime) }}</span>
          </div>
        </div>

        <div class="detail-section" v-if="orderItems.length > 0">
          <h3>商品列表</h3>
          <div v-for="item in orderItems" :key="item.id" class="order-item-product">
            <div class="product-image">
              <img :src="item.productImage || '/placeholder.png'" :alt="item.productName" />
            </div>
            <div class="product-info">
              <div class="product-name">{{ item.productName }}</div>
              <div class="product-price">¥{{ item.price }} x {{ item.quantity }}</div>
            </div>
            <div class="product-subtotal">
              ¥{{ (item.price * item.quantity).toFixed(2) }}
            </div>
          </div>
        </div>
      </div>
    </el-dialog>

    <!-- 页脚 -->
    <footer class="footer">
      <div class="container">
        <p>&copy; 2026 家具商城. 保留所有权利.</p>
      </div>
    </footer>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '../../stores/user'
import { merchantAPI } from '../../api'
import { ElMessage } from 'element-plus'

const router = useRouter()
const userStore = useUserStore()

const orders = ref([])
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)
const filterStatus = ref(null)

const detailDialogVisible = ref(false)
const currentOrder = ref(null)
const orderItems = ref([])

const statusMap = {
  0: { text: '待付款', type: 'warning' },
  1: { text: '已付款', type: 'primary' },
  2: { text: '已发货', type: 'info' },
  3: { text: '已完成', type: 'success' },
  4: { text: '已取消', type: 'info' },
  5: { text: '已退款', type: 'danger' }
}

onMounted(async () => {
  if (!userStore.isAuthenticated) {
    router.push('/login')
    return
  }
  await loadOrders()
})

const loadOrders = async () => {
  try {
    const params = {
      page: currentPage.value,
      size: pageSize.value
    }
    if (filterStatus.value !== null) {
      params.status = filterStatus.value
    }

    const response = await merchantAPI.orders.getList(params)
    orders.value = response.data.data.records || []
    total.value = response.data.data.total || 0
  } catch (error) {
    console.error('获取订单列表失败:', error)
    ElMessage.error('获取订单列表失败')
  }
}

const getStatusText = (status) => {
  return statusMap[status]?.text || '未知'
}

const getStatusType = (status) => {
  return statusMap[status]?.type || 'info'
}

const formatTime = (time) => {
  if (!time) return '-'
  const date = new Date(time)
  return date.toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  })
}

const handleDetail = async (orderNo) => {
  try {
    const response = await merchantAPI.orders.getDetail(orderNo)
    currentOrder.value = response.data.data.order
    orderItems.value = response.data.data.orderItems || []
    detailDialogVisible.value = true
  } catch (error) {
    console.error('获取订单详情失败:', error)
    ElMessage.error('获取订单详情失败')
  }
}

const handleShip = async (orderNo) => {
  try {
    await merchantAPI.orders.updateStatus(orderNo, 2)
    ElMessage.success('发货成功')
    await loadOrders()
  } catch (error) {
    console.error('发货失败:', error)
    ElMessage.error('发货失败')
  }
}

const logout = () => {
  localStorage.removeItem('furniture_token')
  localStorage.removeItem('furniture_user')
  router.push('/login')
}
</script>

<style scoped>
.merchant-orders {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
}

.navbar {
  background: #fff;
  box-shadow: 0 2px 8px rgba(0,0,0,0.1);
  padding: 0;
  position: sticky;
  top: 0;
  z-index: 100;
}

.navbar .container {
  display: flex;
  align-items: center;
  justify-content: space-between;
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 20px;
  height: 60px;
}

.logo a {
  font-size: 20px;
  font-weight: bold;
  color: #409eff;
  text-decoration: none;
}

.nav {
  display: flex;
  gap: 20px;
}

.nav-item {
  color: #666;
  text-decoration: none;
  padding: 8px 16px;
  border-radius: 4px;
  transition: all 0.3s;
}

.nav-item:hover,
.nav-item.router-link-active {
  background: #409eff;
  color: #fff;
}

.user {
  display: flex;
  align-items: center;
  gap: 10px;
}

.welcome {
  color: #666;
}

.btn {
  padding: 8px 16px;
  border: 1px solid #409eff;
  background: #fff;
  color: #409eff;
  border-radius: 4px;
  cursor: pointer;
  transition: all 0.3s;
}

.btn:hover {
  background: #409eff;
  color: #fff;
}

.content {
  flex: 1;
  padding: 40px 0;
  background: #f5f5f5;
}

.container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 20px;
}

.page-title {
  font-size: 28px;
  margin-bottom: 30px;
  color: #333;
}

.filter-section {
  margin-bottom: 20px;
}

.order-list {
  display: flex;
  flex-direction: column;
  gap: 15px;
}

.order-item {
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.1);
  padding: 20px;
}

.order-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 15px;
  padding-bottom: 15px;
  border-bottom: 1px solid #eee;
}

.order-no {
  font-weight: bold;
  color: #333;
}

.order-info {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 15px;
}

.order-amount {
  font-size: 20px;
  font-weight: bold;
  color: #f56c6c;
}

.order-time {
  color: #999;
  font-size: 14px;
}

.order-actions {
  display: flex;
  gap: 10px;
  justify-content: flex-end;
}

.pagination {
  margin-top: 20px;
  display: flex;
  justify-content: center;
}

.order-detail {
  padding: 10px 0;
}

.detail-section {
  margin-bottom: 30px;
}

.detail-section h3 {
  font-size: 16px;
  margin-bottom: 15px;
  padding-bottom: 10px;
  border-bottom: 2px solid #409eff;
}

.detail-row {
  display: flex;
  margin-bottom: 12px;
  font-size: 14px;
}

.detail-row .label {
  width: 100px;
  color: #666;
}

.detail-row .value {
  color: #333;
  flex: 1;
}

.detail-row .price {
  color: #f56c6c;
  font-weight: bold;
}

.order-item-product {
  display: flex;
  align-items: center;
  padding: 15px 0;
  border-bottom: 1px solid #eee;
}

.product-image {
  width: 80px;
  height: 80px;
  overflow: hidden;
  border-radius: 4px;
  background: #f5f5f5;
  margin-right: 15px;
}

.product-image img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.product-info {
  flex: 1;
}

.product-name {
  font-size: 14px;
  margin-bottom: 5px;
  color: #333;
}

.product-price {
  font-size: 13px;
  color: #666;
}

.product-subtotal {
  font-size: 16px;
  font-weight: bold;
  color: #f56c6c;
}

.footer {
  background: #fff;
  padding: 20px 0;
  text-align: center;
  color: #666;
  margin-top: auto;
}
</style>