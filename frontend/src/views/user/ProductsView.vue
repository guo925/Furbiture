<template>
  <div class="products">
    <!-- 导航栏 -->
    <header class="navbar">
      <div class="container">
        <div class="logo">
          <router-link to="/">家具商城</router-link>
        </div>
        <nav class="nav">
          <router-link to="/home" class="nav-item">首页</router-link>
          <router-link to="/products" class="nav-item active">商品列表</router-link>
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

    <!-- 搜索和筛选 -->
    <div class="filter-section">
      <div class="container">
        <div class="filter-bar">
          <div class="search-box">
            <el-input
              v-model="searchQuery"
              placeholder="搜索商品"
              prefix-icon="Search"
              style="width: 300px"
            >
              <template #append>
                <el-button @click="search">搜索</el-button>
              </template>
            </el-input>
          </div>
          <div class="category-filter">
            <el-select v-model="selectedCategory" placeholder="选择分类">
              <el-option label="全部分类" value=""></el-option>
              <el-option
                v-for="category in categories"
                :key="category.id"
                :label="category.name"
                :value="category.id"
              ></el-option>
            </el-select>
          </div>
          <div class="sort-filter">
            <el-select v-model="sortBy" placeholder="排序方式">
              <el-option label="默认排序" value=""></el-option>
              <el-option label="价格从低到高" value="price_asc"></el-option>
              <el-option label="价格从高到低" value="price_desc"></el-option>
            </el-select>
          </div>
        </div>
      </div>
    </div>

    <!-- 商品列表 -->
    <div class="product-section">
      <div class="container">
        <div class="product-grid" v-if="!loading">
          <div
            v-for="product in products"
            :key="product.id"
            class="product-card"
            @click="goToDetail(product.id)"
          >
            <div class="product-image">
              <img :src="product.mainImage" :alt="product.name">
            </div>
            <div class="product-info">
              <h3 class="product-name">{{ product.name }}</h3>
              <p class="product-price">¥{{ product.price }}</p>
              <div class="product-buttons">
                <button @click.stop="addToCart(product.id)" class="btn btn-primary">加入购物车</button>
              </div>
            </div>
          </div>
        </div>
        <div v-else class="loading">
          <el-skeleton :rows="8" animated />
        </div>

        <!-- 分页 -->
        <div class="pagination" v-if="!loading && total > 0">
          <el-pagination
            v-model:current-page="currentPage"
            v-model:page-size="pageSize"
            :page-sizes="[12, 24, 36]"
            layout="total, sizes, prev, pager, next, jumper"
            :total="total"
            @size-change="handleSizeChange"
            @current-change="handleCurrentChange"
          />
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
import { ref, onMounted, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useUserStore } from '../../stores/user'
import { useCartStore } from '../../stores/cart'
import { productAPI } from '../../api'
import { ElMessage } from 'element-plus'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()
const cartStore = useCartStore()

const searchQuery = ref('')
const selectedCategory = ref('')
const sortBy = ref('')
const currentPage = ref(1)
const pageSize = ref(12)
const products = ref([])
const total = ref(0)
const loading = ref(false)
const categories = ref([])

onMounted(async () => {
  await loadCategories()
  await loadProducts()
  if (userStore.isAuthenticated) {
    await cartStore.getCartList()
  }

  if (route.query.categoryId) {
    selectedCategory.value = route.query.categoryId
  }
})

watch([selectedCategory, sortBy, searchQuery], () => {
  currentPage.value = 1
  loadProducts()
})

const loadCategories = async () => {
  try {
    const response = await productAPI.getCategories()
    categories.value = response.data.data
  } catch (error) {
    console.error('获取分类失败:', error)
  }
}

const loadProducts = async () => {
  try {
    loading.value = true
    const params = {
      page: currentPage.value,
      size: pageSize.value
    }

    if (selectedCategory.value) {
      params.categoryId = selectedCategory.value
    }
    if (searchQuery.value) {
      params.keyword = searchQuery.value
    }
    if (sortBy.value) {
      params.sortBy = sortBy.value
    }

    const response = await productAPI.getList(params)
    products.value = response.data.data.records
    total.value = response.data.data.total
  } catch (error) {
    console.error('获取商品失败:', error)
  } finally {
    loading.value = false
  }
}

const search = () => {
  currentPage.value = 1
  loadProducts()
}

const handleSizeChange = (size) => {
  pageSize.value = size
  loadProducts()
}

const handleCurrentChange = (page) => {
  currentPage.value = page
  loadProducts()
}

const goToDetail = (productId) => {
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
.products {
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

.filter-section {
  background: #f5f7fa;
  padding: 20px 0;
  border-bottom: 1px solid #e4e7ed;
}

.filter-bar {
  display: flex;
  gap: 20px;
  align-items: center;
}

.search-box {
  flex: 1;
}

.category-filter,
.sort-filter {
  min-width: 150px;
}

.product-section {
  flex: 1;
  padding: 30px 0;
}

.product-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 20px;
}

.product-card {
  background: #fff;
  border-radius: 8px;
  overflow: hidden;
  box-shadow: 0 2px 8px rgba(0,0,0,0.1);
  cursor: pointer;
  transition: all 0.3s;
}

.product-card:hover {
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

.product-card:hover .product-image img {
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

.product-buttons {
  display: flex;
  gap: 10px;
}

.loading {
  padding: 40px 0;
}

.pagination {
  margin-top: 30px;
  display: flex;
  justify-content: center;
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