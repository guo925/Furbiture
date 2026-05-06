<template>
  <div class="product-detail">
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

    <!-- 商品详情 -->
    <div class="detail-section" v-if="product">
      <div class="container">
        <div class="detail-content">
          <div class="product-images">
            <el-image
              :src="product.mainImage"
              :alt="product.name"
              class="main-image"
              :preview-src-list="[product.mainImage]"
              fit="cover"
            />
          </div>
          <div class="product-info">
            <h1 class="product-name">{{ product.name }}</h1>
            <p class="product-price">¥{{ product.price }}</p>
            <div class="product-desc">
              <h3>商品描述</h3>
              <p>{{ product.description }}</p>
            </div>
            <div class="product-stock">
              <span>库存: {{ product.stock }}</span>
            </div>
            <div class="product-actions">
              <el-input-number 
                v-model="quantity" 
                :min="1" 
                :max="product.stock"
                size="large"
              />
              <button @click="addToCart" class="btn btn-primary">加入购物车</button>
              <button @click="buyNow" class="btn btn-success">立即购买</button>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 加载中 -->
    <div class="loading" v-else>
      <el-skeleton :rows="10" animated />
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
import { useRouter, useRoute } from 'vue-router'
import { useUserStore } from '../../stores/user'
import { useCartStore } from '../../stores/cart'
import { productAPI } from '../../api'
import { ElMessage } from 'element-plus'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()
const cartStore = useCartStore()

const product = ref(null)
const quantity = ref(1)

onMounted(async () => {
  const productId = route.params.id
  await loadProductDetail(productId)
  if (userStore.isAuthenticated) {
    await cartStore.getCartList()
  }
})

const loadProductDetail = async (id) => {
  try {
    const response = await productAPI.getDetail(id)
    product.value = response.data.data.product
  } catch (error) {
    console.error('获取商品详情失败:', error)
    ElMessage.error('获取商品详情失败')
    router.push('/products')
  }
}

const addToCart = async () => {
  if (!userStore.isAuthenticated) {
    router.push('/login')
    return
  }
  
  try {
    await cartStore.addToCart(product.value.id, quantity.value)
    ElMessage.success('添加购物车成功')
  } catch (error) {
    ElMessage.error(error.message || '添加购物车失败')
  }
}

const buyNow = async () => {
  if (!userStore.isAuthenticated) {
    router.push('/login')
    return
  }
  
  try {
    // 先添加到购物车，不显示提示
    await cartStore.addToCart(product.value.id, quantity.value, false)
    // 跳转到购物车
    router.push('/cart')
  } catch (error) {
    ElMessage.error(error.message || '操作失败')
  }
}

const logout = () => {
  userStore.logout()
  ElMessage.success('退出成功')
  router.push('/home')
}
</script>

<style scoped>
.product-detail {
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

.nav-item:hover {
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

.btn-success {
  background: #67c23a;
  color: #fff;
  border-color: #67c23a;
}

.btn-success:hover {
  background: #85ce61;
  border-color: #85ce61;
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

.detail-section {
  flex: 1;
  padding: 40px 0;
}

.detail-content {
  display: flex;
  gap: 40px;
  background: #fff;
  padding: 30px;
  border-radius: 8px;
  box-shadow: 0 2px 12px rgba(0,0,0,0.1);
}

.product-images {
  flex: 1;
  max-width: 500px;
}

.main-image {
  width: 100%;
  height: 400px;
  object-fit: cover;
  border-radius: 8px;
}

.product-info {
  flex: 1;
  min-width: 400px;
}

.product-name {
  font-size: 24px;
  font-weight: bold;
  color: #333;
  margin-bottom: 20px;
}

.product-price {
  font-size: 32px;
  font-weight: bold;
  color: #f56c6c;
  margin-bottom: 30px;
}

.product-desc {
  margin-bottom: 30px;
}

.product-desc h3 {
  font-size: 18px;
  font-weight: bold;
  color: #333;
  margin-bottom: 10px;
}

.product-desc p {
  color: #666;
  line-height: 1.6;
}

.product-stock {
  margin-bottom: 30px;
  color: #666;
}

.product-actions {
  display: flex;
  gap: 20px;
  align-items: center;
}

.loading {
  padding: 60px 0;
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