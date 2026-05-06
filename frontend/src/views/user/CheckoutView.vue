<template>
  <div class="checkout">
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

    <!-- 结算内容 -->
    <div class="checkout-section">
      <div class="container">
        <h2 class="section-title">确认订单</h2>
        
        <div v-if="loading" class="loading">
          <el-skeleton :rows="8" animated />
        </div>
        
        <div v-else-if="cartStore.cartItems.length === 0" class="empty-cart">
          <el-empty description="购物车是空的" />
          <router-link to="/products" class="btn btn-primary">去购物</router-link>
        </div>
        
        <div v-else class="checkout-content">
          <!-- 收货地址 -->
          <div class="address-section">
            <h3 class="section-subtitle">收货地址</h3>
            <div class="address-list">
              <div 
                v-for="address in addresses" 
                :key="address.id"
                :class="['address-item', { active: selectedAddressId === address.id }]"
                @click="selectedAddressId = address.id"
              >
                <div class="address-info">
                  <div class="address-header">
                    <span class="name">{{ address.name }}</span>
                    <span class="phone">{{ address.phone }}</span>
                    <el-tag v-if="address.isDefault" size="small" type="primary">默认</el-tag>
                  </div>
                  <div class="address-detail">{{ address.province }}{{ address.city }}{{ address.district }}{{ address.detailAddress }}</div>
                </div>
              </div>
              <div class="address-item add-address">
                <router-link to="/address">
                  <el-icon><Plus /></el-icon>
                  <span>添加新地址</span>
                </router-link>
              </div>
            </div>
          </div>

          <!-- 商品清单 -->
          <div class="order-items">
            <h3 class="section-subtitle">商品清单</h3>
            <el-table :data="cartStore.cartItems" style="width: 100%">
              <el-table-column label="商品名称" min-width="200">
                <template #default="scope">
                  {{ scope.row.product?.name || '商品已下架' }}
                </template>
              </el-table-column>
              <el-table-column label="单价" width="120">
                <template #default="scope">
                  ¥{{ scope.row.product?.price || 0 }}
                </template>
              </el-table-column>
              <el-table-column label="数量" width="120">
                <template #default="scope">
                  {{ scope.row.quantity }}
                </template>
              </el-table-column>
              <el-table-column label="小计" width="120">
                <template #default="scope">
                  ¥{{ (scope.row.product?.price || 0) * (scope.row.quantity || 0) }}
                </template>
              </el-table-column>
            </el-table>
          </div>

          <!-- 订单金额 -->
          <div class="order-summary">
            <h3 class="section-subtitle">订单金额</h3>
            <div class="summary-content">
              <div class="summary-item">
                <span>商品总价</span>
                <span>¥{{ isNaN(cartStore.totalPrice) ? '0.00' : cartStore.totalPrice.toFixed(2) }}</span>
              </div>
              <div class="summary-item">
                <span>运费</span>
                <span>¥0.00</span>
              </div>
              <div class="summary-item total">
                <span>实付金额</span>
                <span class="total-price">¥{{ isNaN(cartStore.totalPrice) ? '0.00' : cartStore.totalPrice.toFixed(2) }}</span>
              </div>
            </div>
          </div>

          <!-- 支付按钮 -->
          <div class="checkout-actions">
            <button @click="router.push('/cart')" class="btn">返回购物车</button>
            <button @click="handlePayment" class="btn btn-primary pay-btn">提交订单</button>
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
import { ref, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '../../stores/user'
import { useCartStore } from '../../stores/cart'
import { Plus } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { addressAPI, orderAPI, cartAPI } from '../../api'

const router = useRouter()
const userStore = useUserStore()
const cartStore = useCartStore()

const loading = ref(false)
const addresses = ref([])
const selectedAddressId = ref(null)

onMounted(async () => {
  if (!userStore.isAuthenticated) {
    router.push('/login')
    return
  }
  await loadData()
})

const loadData = async () => {
  loading.value = true
  try {
    await cartStore.getCartList()

    const addressResponse = await addressAPI.getList()
    addresses.value = (addressResponse.data.data || []).map(addr => ({
      id: addr.id,
      name: addr.name,
      phone: addr.phone,
      province: addr.province,
      city: addr.city,
      district: addr.district,
      detailAddress: addr.detailAddress,
      isDefault: addr.isDefault === 1
    }))

    if (addresses.value.length > 0) {
      const defaultAddress = addresses.value.find(addr => addr.isDefault)
      selectedAddressId.value = defaultAddress ? defaultAddress.id : addresses.value[0].id
    }
  } catch (error) {
    console.error('加载数据失败:', error)
    ElMessage.error('加载数据失败')
  } finally {
    loading.value = false
  }
}

const handlePayment = async () => {
  if (!selectedAddressId.value) {
    ElMessage.warning('请选择收货地址')
    return
  }
  
  if (cartStore.cartItems.length === 0) {
    ElMessage.warning('购物车为空')
    return
  }
  
  loading.value = true
  try {
    // 构建订单数据（使用后端期望的格式）
    const cartItemIds = cartStore.cartItems.map(item => item.id)
    
    // 创建订单
    const response = await orderAPI.create(selectedAddressId.value, cartItemIds)
    
    // 模拟支付
    await orderAPI.pay(response.data.data.orderNo)
    
    // 清空购物车
    await cartStore.clearCart()
    
    ElMessage.success('支付成功！订单已创建')
    
    // 跳转到订单列表
    router.push('/orders')
  } catch (error) {
    console.error('支付失败:', error)
    ElMessage.error('支付失败，请稍后重试')
  } finally {
    loading.value = false
  }
}

const logout = () => {
  userStore.logout()
  ElMessage.success('退出成功')
  router.push('/home')
}
</script>

<style scoped>
.checkout {
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

.checkout-section {
  flex: 1;
  padding: 30px 0;
}

.section-title {
  font-size: 24px;
  font-weight: bold;
  color: #333;
  margin-bottom: 30px;
  text-align: center;
}

.section-subtitle {
  font-size: 18px;
  font-weight: bold;
  color: #333;
  margin-bottom: 20px;
}

.loading {
  padding: 40px 0;
}

.empty-cart {
  text-align: center;
  padding: 60px 0;
}

.empty-cart .btn {
  margin-top: 20px;
}

.checkout-content {
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.1);
  padding: 30px;
}

.address-section {
  margin-bottom: 30px;
}

.address-list {
  display: flex;
  gap: 20px;
  flex-wrap: wrap;
}

.address-item {
  flex: 1;
  min-width: 300px;
  padding: 20px;
  border: 2px solid #e4e7ed;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.3s;
}

.address-item:hover {
  border-color: #c0c4cc;
}

.address-item.active {
  border-color: #409eff;
  background: #ecf5ff;
}

.address-header {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 10px;
}

.name {
  font-weight: bold;
  color: #333;
}

.phone {
  color: #666;
}

.address-detail {
  color: #666;
  line-height: 1.5;
}

.add-address {
  border: 2px dashed #dcdfe6;
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 120px;
}

.add-address a {
  display: flex;
  align-items: center;
  gap: 10px;
  color: #409eff;
  text-decoration: none;
}

.order-items {
  margin-bottom: 30px;
}

.order-summary {
  margin-bottom: 30px;
  border-top: 1px solid #e4e7ed;
  padding-top: 20px;
}

.summary-content {
  background: #f5f7fa;
  padding: 20px;
  border-radius: 8px;
}

.summary-item {
  display: flex;
  justify-content: space-between;
  margin-bottom: 10px;
  color: #666;
}

.summary-item.total {
  font-weight: bold;
  margin-top: 10px;
  padding-top: 10px;
  border-top: 1px solid #e4e7ed;
}

.total-price {
  color: #f56c6c;
  font-size: 18px;
}

.checkout-actions {
  display: flex;
  justify-content: flex-end;
  gap: 15px;
  margin-top: 30px;
}

.pay-btn {
  font-size: 16px;
  padding: 10px 30px;
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