<template>
  <div class="home">
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

    <!-- 轮播图 -->
    <div class="carousel">
      <el-carousel :interval="5000" height="400px">
        <el-carousel-item v-for="(item, index) in carouselItems" :key="index">
          <img :src="item.image" :alt="item.title" class="carousel-image">
        </el-carousel-item>
      </el-carousel>
    </div>

    <!-- 分类导航 -->
    <div class="categories">
      <div class="container">
        <h2 class="section-title">商品分类</h2>
        <div class="category-list">
          <div 
            v-for="category in categories" 
            :key="category.id"
            class="category-item"
            @click="goToCategory(category.id)"
          >
            <div class="category-icon">
              <img v-if="category.icon" :src="category.icon" :alt="category.name" class="category-image">
              <span v-else class="category-placeholder">{{ category.name.charAt(0) }}</span>
            </div>
            <span class="category-name">{{ category.name }}</span>
          </div>
        </div>
      </div>
    </div>

    <!-- 热门商品 -->
    <div class="hot-products">
      <div class="container">
        <h2 class="section-title">热门商品</h2>
        <div class="product-list">
          <div 
            v-for="product in hotProducts" 
            :key="product.id"
            class="product-item"
            @click="goToProduct(product.id)"
          >
            <div class="product-image">
              <img :src="product.mainImage" :alt="product.name">
            </div>
            <div class="product-info">
              <h3 class="product-name">{{ product.name }}</h3>
              <p class="product-price">¥{{ product.price }}</p>
              <button @click.stop="addToCart(product.id)" class="add-to-cart">加入购物车</button>
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
import { productAPI } from '../../api'
import { ElMessage } from 'element-plus'

const router = useRouter()
const userStore = useUserStore()
const cartStore = useCartStore()

const carouselItems = ref([
  { image: 'https://trae-api-cn.mchost.guru/api/ide/v1/text_to_image?prompt=modern%20furniture%20store%20banner%20with%20sofa%20and%20dining%20table&image_size=landscape_16_9', title: '现代家具' },
  { image: 'https://trae-api-cn.mchost.guru/api/ide/v1/text_to_image?prompt=luxury%20furniture%20collection%20showroom&image_size=landscape_16_9', title: '豪华家具' },
  { image: 'https://trae-api-cn.mchost.guru/api/ide/v1/text_to_image?prompt=minimalist%20furniture%20design%20interior&image_size=landscape_16_9', title: '极简风格' }
])

const categories = ref([])
const hotProducts = ref([])

onMounted(async () => {
  await loadCategories()
  await loadHotProducts()
  if (userStore.isAuthenticated) {
    await userStore.getCurrentUser()
    await cartStore.getCartList()
  }
})

const loadCategories = async () => {
  try {
    const response = await productAPI.getCategories()
    categories.value = response.data.data
  } catch (error) {
    console.error('获取分类失败:', error)
  }
}

const loadHotProducts = async () => {
  try {
    const response = await productAPI.getList({ page: 1, size: 8 })
    hotProducts.value = response.data.data.records
  } catch (error) {
    console.error('获取商品失败:', error)
  }
}

const goToCategory = (categoryId) => {
  router.push({ path: '/products', query: { categoryId } })
}

const goToProduct = (productId) => {
  router.push(`/product/${productId}`)
}

const addToCart = async (productId) => {
  if (!userStore.isAuthenticated) {
    router.push('/login')
    return
  }
  
  try {
    await cartStore.addToCart(productId)
    ElMessage.success('添加购物车成功')
  } catch (error) {
    ElMessage.error(error.message || '添加购物车失败')
  }
}

const logout = () => {
  userStore.logout()
  ElMessage.success('退出成功')
  router.push('/home')
}
</script>

<style scoped>
.home {
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

.cart-badge {
  background: #f56c6c;
  color: #fff;
  border-radius: 50%;
  font-size: 12px;
  padding: 2px 6px;
  margin-left: 5px;
}

.carousel {
  margin: 20px 0;
}

.carousel-image {
  width: 100%;
  height: 400px;
  object-fit: cover;
}

.categories {
  margin: 40px 0;
}

.section-title {
  font-size: 24px;
  font-weight: bold;
  color: #333;
  margin-bottom: 20px;
  text-align: center;
}

.category-list {
  display: grid;
  grid-template-columns: repeat(6, 1fr);
  gap: 20px;
}

.category-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 20px;
  background: #f5f7fa;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.3s;
}

.category-item:hover {
  background: #ecf5ff;
  transform: translateY(-5px);
}

.category-icon {
  width: 80px;
  height: 80px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 10px;
  overflow: hidden;
  background: #f0f0f0;
}

.category-image {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.category-placeholder {
  width: 100%;
  height: 100%;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: #fff;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 28px;
  font-weight: bold;
}

.category-name {
  font-size: 16px;
  color: #333;
}

.hot-products {
  margin: 40px 0;
}

.product-list {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 20px;
}

.product-item {
  background: #fff;
  border-radius: 8px;
  overflow: hidden;
  box-shadow: 0 2px 8px rgba(0,0,0,0.1);
  cursor: pointer;
  transition: all 0.3s;
}

.product-item:hover {
  transform: translateY(-5px);
  box-shadow: 0 4px 16px rgba(0,0,0,0.15);
}

.product-image {
  height: 200px;
  overflow: hidden;
}

.product-image img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.3s;
}

.product-item:hover .product-image img {
  transform: scale(1.1);
}

.product-info {
  padding: 15px;
}

.product-name {
  font-size: 16px;
  font-weight: bold;
  color: #333;
  margin-bottom: 10px;
  height: 48px;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.product-price {
  font-size: 18px;
  font-weight: bold;
  color: #f56c6c;
  margin-bottom: 15px;
}

.add-to-cart {
  width: 100%;
  padding: 8px;
  background: #409eff;
  color: #fff;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  transition: background 0.3s;
}

.add-to-cart:hover {
  background: #66b1ff;
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