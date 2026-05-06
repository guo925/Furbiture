<template>
  <div class="orders">
    <!-- 导航栏 -->
    <header class="navbar">
      <div class="container">
        <div class="logo">
          <router-link to="/">家具商城</router-link>
        </div>
        <nav class="nav">
          <router-link to="/home" class="nav-item">首页</router-link>
          <router-link to="/products" class="nav-item">商品列表</router-link>
          <router-link to="/cart" class="nav-item">
            购物车
            <span v-if="cartStore.totalQuantity > 0" class="cart-badge">{{ cartStore.totalQuantity }}</span>
          </router-link>
        </nav>
        <div class="user">
          <template v-if="userStore.isAuthenticated">
            <span class="welcome">欢迎, {{ userStore.user.username }}</span>
            <router-link to="/profile" class="nav-item">个人中心</router-link>
            <router-link to="/orders" class="nav-item active">我的订单</router-link>
            <button @click="logout" class="btn">退出</button>
          </template>
          <template v-else>
            <router-link to="/login" class="btn">登录</router-link>
            <router-link to="/register" class="btn btn-primary">注册</router-link>
          </template>
        </div>
      </div>
    </header>

    <!-- 订单列表 -->
    <div class="orders-section">
      <div class="container">
        <h2 class="page-title">我的订单</h2>

        <div v-if="orders.length === 0" class="empty-orders">
          <el-empty description="暂无订单" />
          <router-link to="/products" class="btn btn-primary">去购物</router-link>
        </div>

        <div v-else class="orders-list">
          <div v-for="order in orders" :key="order.id" class="order-item">
            <div class="order-header">
              <span class="order-id">订单号: {{ order.orderNumber }}</span>
              <span :class="['order-status', order.status]">{{ getStatusText(order.status) }}</span>
            </div>
            <div class="order-content">
              <div v-for="item in order.orderItems" :key="item.id" class="order-product">
                <img :src="item.productImage" :alt="item.productName" class="product-image">
                <div class="product-info">
                  <span class="product-name">{{ item.productName }}</span>
                  <div class="product-price">
                    <span>¥{{ item.price }}</span>
                    <span>x{{ item.quantity }}</span>
                  </div>
                </div>
              </div>
            </div>
            <div class="order-footer">
              <span class="order-total">合计: ¥{{ order.totalPrice }}</span>
              <div class="order-actions">
                <el-button v-if="order.status === 'PENDING'" type="primary" @click="cancelOrder(order.id)">取消订单</el-button>
                <el-button v-if="order.status === 'PENDING'" type="success" @click="payOrder(order.id)">去支付</el-button>
                <el-button v-if="order.status === 'DELIVERED'" type="primary" @click="confirmReceipt(order.id)">确认收货</el-button>
                <el-button @click="viewOrderDetail(order.id)">查看详情</el-button>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

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
import { useCartStore } from '../../stores/cart'
import { orderAPI } from '../../api'
import { ElMessage } from 'element-plus'

const router = useRouter()
const userStore = useUserStore()
const cartStore = useCartStore()

const orders = ref([])

onMounted(async () => {
  if (!userStore.isAuthenticated) {
    router.push('/login')
    return
  }
  await loadOrders()
  await cartStore.getCartList()
})

const loadOrders = async () => {
  try {
    const response = await orderAPI.getList()
    const orderList = response.data.data || []

    // 转换订单数据格式
    orders.value = orderList.map(order => ({
      id: order.id,
      orderNumber: order.orderNo,
      status: order.status === 0 ? 'PENDING' :
              order.status === 1 ? 'PAID' :
              order.status === 2 ? 'DELIVERED' :
              order.status === 3 ? 'COMPLETED' :
              order.status === 4 ? 'CANCELLED' : 'REFUNDED',
      totalPrice: order.totalAmount,
      createTime: order.createTime,
      orderItems: order.orderItems || []
    }))
  } catch (error) {
    console.error('获取订单失败:', error)
    ElMessage.error('获取订单失败')
    orders.value = []
  }
}

const getStatusText = (status) => {
  const statusMap = {
    'PENDING': '待支付',
    'PAID': '已支付',
    'DELIVERED': '已发货',
    'COMPLETED': '已完成',
    'CANCELLED': '已取消',
    'REFUNDED': '已退款'
  }
  return statusMap[status] || status
}

const cancelOrder = async (orderId) => {
  try {
    // 找到对应订单的orderNo
    const order = orders.value.find(o => o.id === orderId)
    if (order) {
      await orderAPI.cancel(order.orderNumber)
      ElMessage.success('订单已取消')
      await loadOrders()
    }
  } catch (error) {
    ElMessage.error(error.message || '取消订单失败')
  }
}

const payOrder = async (orderId) => {
  try {
    // 找到对应订单的orderNo
    const order = orders.value.find(o => o.id === orderId)
    if (order) {
      await orderAPI.pay(order.orderNumber)
      ElMessage.success('支付成功')
      await loadOrders()
    }
  } catch (error) {
    ElMessage.error(error.message || '支付失败')
  }
}

const confirmReceipt = async (orderId) => {
  try {
    const order = orders.value.find(o => o.id === orderId)
    if (order) {
      await orderAPI.confirmReceipt(order.orderNumber)
      ElMessage.success('确认收货成功')
      await loadOrders()
    }
  } catch (error) {
    ElMessage.error(error.message || '确认收货失败')
  }
}

const viewOrderDetail = (orderId) => {
  router.push(`/order/${orderId}`)
}

const logout = () => {
  userStore.logout()
  ElMessage.success('退出成功')
  router.push('/home')
}
</script>

<style scoped>
.orders {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
}

.navbar {
  background: #fff;
  box-shadow: 0 2px 8px rgba(0,0,0,0.1);
  position: sticky;
  top: 0;
  z-index: 100;
}

.container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 20px;
}

.navbar .container {
  display: flex;
  justify-content: space-between;
  align-items: center;
  height: 60px;
}

.logo a {
  font-size: 24px;
  font-weight: bold;
  color: #333;
  text-decoration: none;
}

.nav {
  display: flex;
  gap: 30px;
}

.nav-item {
  color: #666;
  text-decoration: none;
  font-size: 16px;
  transition: color 0.3s;
}

.nav-item:hover,
.nav-item.active {
  color: #409eff;
}

.user {
  display: flex;
  align-items: center;
  gap: 15px;
}

.welcome {
  color: #666;
}

.btn {
  padding: 6px 16px;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  background: #fff;
  color: #666;
  cursor: pointer;
  transition: all 0.3s;
  text-decoration: none;
}

.btn:hover {
  border-color: #409eff;
  color: #409eff;
}

.btn-primary {
  background: #409eff;
  color: #fff;
  border-color: #409eff;
}

.btn-primary:hover {
  background: #66b1ff;
  border-color: #66b1ff;
  color: #fff;
}

.cart-badge {
  background: #f56c6c;
  color: #fff;
  border-radius: 50%;
  font-size: 12px;
  padding: 2px 6px;
  margin-left: 5px;
}

.orders-section {
  flex: 1;
  padding: 40px 0;
}

.page-title {
  font-size: 24px;
  font-weight: bold;
  color: #333;
  margin-bottom: 30px;
}

.empty-orders {
  text-align: center;
  padding: 60px 0;
  background: #f5f7fa;
  border-radius: 8px;
}

.empty-orders .btn {
  margin-top: 20px;
}

.orders-list {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.order-item {
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 2px 12px rgba(0,0,0,0.1);
  overflow: hidden;
}

.order-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 15px 20px;
  background: #f5f7fa;
  border-bottom: 1px solid #e4e7ed;
}

.order-id {
  font-size: 14px;
  color: #666;
}

.order-status {
  font-size: 14px;
  font-weight: bold;
  padding: 2px 8px;
  border-radius: 4px;
}

.order-status.PENDING {
  color: #e6a23c;
  background: #fdf6ec;
  border: 1px solid #fde2a3;
}

.order-status.PAID {
  color: #409eff;
  background: #ecf5ff;
  border: 1px solid #adc6f5;
}

.order-status.DELIVERED {
  color: #67c23a;
  background: #f0f9eb;
  border: 1px solid #b7eb8f;
}

.order-status.COMPLETED {
  color: #909399;
  background: #f5f7fa;
  border: 1px solid #dcdfe6;
}

.order-status.CANCELLED {
  color: #f56c6c;
  background: #fef0f0;
  border: 1px solid #fbc4c4;
}

.order-content {
  padding: 20px;
}

.order-product {
  display: flex;
  align-items: center;
  gap: 15px;
  padding: 10px 0;
  border-bottom: 1px solid #f0f0f0;
}

.order-product:last-child {
  border-bottom: none;
}

.product-image {
  width: 80px;
  height: 80px;
  object-fit: cover;
  border-radius: 4px;
}

.product-info {
  flex: 1;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.product-name {
  color: #333;
  font-size: 14px;
}

.product-price {
  display: flex;
  gap: 20px;
  color: #666;
  font-size: 14px;
}

.order-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 15px 20px;
  background: #f5f7fa;
  border-top: 1px solid #e4e7ed;
}

.order-total {
  font-size: 16px;
  font-weight: bold;
  color: #333;
}

.order-actions {
  display: flex;
  gap: 10px;
}

.footer {
  background: #f5f7fa;
  padding: 30px 0;
  margin-top: auto;
}

.footer p {
  text-align: center;
  color: #666;
  font-size: 14px;
}
</style>