<template>
  <div class="order-detail">
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
            <router-link to="/orders" class="nav-item">我的订单</router-link>
            <button @click="logout" class="btn">退出</button>
          </template>
          <template v-else>
            <router-link to="/login" class="btn">登录</router-link>
            <router-link to="/register" class="btn btn-primary">注册</router-link>
          </template>
        </div>
      </div>
    </header>

    <!-- 订单详情 -->
    <div class="order-detail-section">
      <div class="container">
        <h2 class="page-title">订单详情</h2>
        
        <div v-if="loading" class="loading">
          <el-loading v-model="loading" text="加载中..." />
        </div>
        
        <div v-else-if="orderDetail" class="order-detail-content">
          <!-- 订单基本信息 -->
          <div class="order-info">
            <h3>订单信息</h3>
            <div class="info-item">
              <span class="label">订单号：</span>
              <span class="value">{{ orderDetail.orderNumber }}</span>
            </div>
            <div class="info-item">
              <span class="label">订单状态：</span>
              <span :class="['status', orderDetail.status]">{{ getStatusText(orderDetail.status) }}</span>
            </div>
            <div class="info-item">
              <span class="label">总金额：</span>
              <span class="value total">¥{{ orderDetail.totalPrice }}</span>
            </div>
            <div class="info-item">
              <span class="label">创建时间：</span>
              <span class="value">{{ orderDetail.createTime }}</span>
            </div>
          </div>
          
          <!-- 收货地址 -->
          <div class="address-info" v-if="address">
            <h3>收货信息</h3>
            <div class="info-item">
              <span class="label">收货人：</span>
              <span class="value">{{ address.receiver }} {{ address.phone }}</span>
            </div>
            <div class="info-item">
              <span class="label">收货地址：</span>
              <span class="value">{{ address.province }}{{ address.city }}{{ address.district }}{{ address.detail }}</span>
            </div>
          </div>
          
          <!-- 商品信息 -->
          <div class="product-info">
            <h3>商品信息</h3>
            <div class="product-list">
              <div v-for="item in orderItems" :key="item.id" class="product-item">
                <img :src="item.productImage" :alt="item.productName" class="product-image">
                <div class="product-details">
                  <span class="product-name">{{ item.productName }}</span>
                  <div class="product-price">
                    <span>¥{{ item.price }}</span>
                    <span>x{{ item.quantity }}</span>
                    <span class="subtotal">¥{{ item.totalPrice }}</span>
                  </div>
                </div>
              </div>
            </div>
          </div>
          
          <!-- 操作按钮 -->
          <div class="order-actions">
            <el-button @click="goBack">返回订单列表</el-button>
            <el-button v-if="orderDetail.status === 'PENDING'" type="primary" @click="cancelOrder">取消订单</el-button>
            <el-button v-if="orderDetail.status === 'PENDING'" type="success" @click="payOrder">去支付</el-button>
            <el-button v-if="orderDetail.status === 'DELIVERED'" type="primary" @click="confirmReceipt">确认收货</el-button>
          </div>
        </div>
        
        <div v-else class="error">
          <el-empty description="订单不存在" />
          <el-button @click="goBack">返回订单列表</el-button>
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
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '../../stores/user'
import { useCartStore } from '../../stores/cart'
import { orderAPI, addressAPI } from '../../api'
import { ElMessage, ElLoading } from 'element-plus'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const cartStore = useCartStore()

const loading = ref(true)
const orderDetail = ref(null)
const orderItems = ref([])
const address = ref(null)

onMounted(async () => {
  if (!userStore.isAuthenticated) {
    router.push('/login')
    return
  }
  await loadOrderDetail()
  await cartStore.getCartList()
})

const loadOrderDetail = async () => {
  try {
    loading.value = true
    const orderId = route.params.id
    
    // 先获取所有订单，找到对应订单的 orderNumber
    const ordersResponse = await orderAPI.getList()
    const orderList = ordersResponse.data.data || []
    const order = orderList.find(o => o.id == orderId)
    
    if (order) {
      const response = await orderAPI.getDetail(order.orderNo)
      const data = response.data.data
      
      // 转换订单数据格式
      orderDetail.value = {
        id: data.order.id,
        orderNumber: data.order.orderNo,
        status: data.order.status === 0 ? 'PENDING' : 
                data.order.status === 1 ? 'PAID' : 
                data.order.status === 2 ? 'DELIVERED' : 
                data.order.status === 3 ? 'COMPLETED' : 
                data.order.status === 4 ? 'CANCELLED' : 'REFUNDED',
        totalPrice: data.order.totalAmount,
        createTime: data.order.createTime,
        payTime: data.order.payTime,
        deliveryTime: data.order.deliveryTime,
        finishTime: data.order.finishTime
      }
      
      // 转换商品数据
      orderItems.value = (data.items || []).map(item => ({
        id: item.id,
        productId: item.productId,
        productName: item.productName,
        productImage: item.productImage || '',
        quantity: item.quantity,
        price: item.price,
        totalPrice: item.totalPrice
      }))
      
      // 获取地址信息
      if (data.order.addressId) {
        await loadAddress(data.order.addressId)
      }
    } else {
      ElMessage.error('订单不存在')
    }
  } catch (error) {
    console.error('获取订单详情失败:', error)
    ElMessage.error('获取订单详情失败')
  } finally {
    loading.value = false
  }
}

const loadAddress = async (addressId) => {
  try {
    const response = await addressAPI.getList()
    const addresses = response.data.data || []
    const addr = addresses.find(a => a.id == addressId)
    if (addr) {
      address.value = {
        receiver: addr.name,
        phone: addr.phone,
        province: addr.province,
        city: addr.city,
        district: addr.district,
        detail: addr.detailAddress
      }
    }
  } catch (error) {
    console.error('获取地址失败:', error)
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

const goBack = () => {
  router.push('/orders')
}

const cancelOrder = async () => {
  try {
    await orderAPI.cancel(orderDetail.value.orderNumber)
    ElMessage.success('订单已取消')
    await loadOrderDetail()
  } catch (error) {
    ElMessage.error(error.message || '取消订单失败')
  }
}

const payOrder = async () => {
  try {
    await orderAPI.pay(orderDetail.value.orderNumber)
    ElMessage.success('支付成功')
    await loadOrderDetail()
  } catch (error) {
    ElMessage.error(error.message || '支付失败')
  }
}

const confirmReceipt = async () => {
  try {
    await orderAPI.confirmReceipt(orderDetail.value.orderNumber)
    ElMessage.success('确认收货成功')
    await loadOrderDetail()
  } catch (error) {
    ElMessage.error(error.message || '确认收货失败')
  }
}

const logout = () => {
  userStore.logout()
  ElMessage.success('退出成功')
  router.push('/home')
}
</script>

<style scoped>
.order-detail {
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

.order-detail-section {
  flex: 1;
  padding: 40px 0;
}

.page-title {
  margin-bottom: 30px;
  color: #333;
  font-size: 24px;
  font-weight: bold;
}

.order-detail-content {
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 2px 12px rgba(0,0,0,0.1);
  padding: 20px;
}

.order-info,
.address-info,
.product-info {
  margin-bottom: 30px;
  padding-bottom: 20px;
  border-bottom: 1px solid #f0f0f0;
}

.order-info h3,
.address-info h3,
.product-info h3 {
  margin-bottom: 20px;
  color: #333;
  font-size: 18px;
  font-weight: bold;
}

.info-item {
  margin-bottom: 10px;
  display: flex;
  align-items: center;
}

.info-item .label {
  width: 100px;
  color: #666;
}

.info-item .value {
  color: #333;
  flex: 1;
}

.info-item .value.total {
  color: #ff4d4f;
  font-weight: bold;
  font-size: 18px;
}

.info-item .status {
  padding: 4px 12px;
  border-radius: 12px;
  font-size: 14px;
  font-weight: bold;
}

.status.PENDING {
  background: #fff2e8;
  color: #fa8c16;
}

.status.PAID {
  background: #e6f7ff;
  color: #1890ff;
}

.status.DELIVERED {
  background: #f6ffed;
  color: #52c41a;
}

.status.COMPLETED {
  background: #f0f0f0;
  color: #666;
}

.status.CANCELLED {
  background: #fff2f0;
  color: #ff4d4f;
}

.status.REFUNDED {
  background: #fff1f0;
  color: #ff4d4f;
}

.product-list {
  display: flex;
  flex-direction: column;
  gap: 15px;
}

.product-item {
  display: flex;
  align-items: center;
  padding: 15px;
  border: 1px solid #f0f0f0;
  border-radius: 8px;
}

.product-image {
  width: 80px;
  height: 80px;
  object-fit: cover;
  border-radius: 4px;
  margin-right: 15px;
}

.product-details {
  flex: 1;
}

.product-name {
  display: block;
  margin-bottom: 10px;
  color: #333;
  font-size: 16px;
}

.product-price {
  display: flex;
  align-items: center;
  gap: 20px;
}

.product-price span {
  color: #666;
}

.product-price .subtotal {
  color: #333;
  font-weight: bold;
  margin-left: auto;
}

.order-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  margin-top: 30px;
}

.loading {
  height: 400px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.error {
  text-align: center;
  padding: 60px 0;
}

.error .el-button {
  margin-top: 20px;
}
</style>