<template>
  <div class="merchant-dashboard">
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
          <router-link to="/merchant/profile" class="nav-item">个人中心</router-link>
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
        <h1 class="page-title">商家中心</h1>

        <!-- 统计卡片 -->
        <div class="stats-grid">
          <router-link to="/merchant/products" class="stat-card">
            <div class="stat-icon">📦</div>
            <div class="stat-info">
              <div class="stat-value">{{ stats.productCount }}</div>
              <div class="stat-label">商品数量</div>
            </div>
          </router-link>

          <router-link to="/merchant/orders?status=1" class="stat-card">
            <div class="stat-icon">📋</div>
            <div class="stat-info">
              <div class="stat-value">{{ stats.pendingOrderCount }}</div>
              <div class="stat-label">待处理订单</div>
            </div>
          </router-link>

          <router-link to="/merchant/orders?status=3" class="stat-card">
            <div class="stat-icon">💰</div>
            <div class="stat-info">
              <div class="stat-value">¥{{ stats.totalSales || 0 }}</div>
              <div class="stat-label">总销售额</div>
            </div>
          </router-link>

          <router-link to="/merchant/orders" class="stat-card">
            <div class="stat-icon">📅</div>
            <div class="stat-info">
              <div class="stat-value">{{ stats.todayOrderCount }}</div>
              <div class="stat-label">今日订单</div>
            </div>
          </router-link>
        </div>

        <!-- 快捷操作 -->
        <div class="quick-actions">
          <h2>快捷操作</h2>
          <div class="actions-grid">
            <router-link to="/merchant/products" class="action-card">
              <div class="action-icon">➕</div>
              <div class="action-text">添加商品</div>
            </router-link>
            <router-link to="/merchant/orders" class="action-card">
              <div class="action-icon">📋</div>
              <div class="action-text">处理订单</div>
            </router-link>
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
import { merchantAPI } from '../../api'
import { ElMessage } from 'element-plus'

const router = useRouter()
const userStore = useUserStore()

const stats = ref({
  productCount: 0,
  pendingOrderCount: 0,
  totalSales: 0,
  todayOrderCount: 0
})

onMounted(async () => {
  if (!userStore.isAuthenticated) {
    router.push('/login')
    return
  }
  await loadStats()
})

const loadStats = async () => {
  try {
    const response = await merchantAPI.dashboard.getStats()
    stats.value = response.data.data || stats.value
  } catch (error) {
    console.error('获取统计数据失败:', error)
    ElMessage.error('获取统计数据失败')
  }
}

const logout = () => {
  localStorage.removeItem('furniture_token')
  localStorage.removeItem('furniture_user')
  router.push('/login')
}
</script>

<style scoped>
.merchant-dashboard {
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

.stats-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
  gap: 20px;
  margin-bottom: 40px;
}

.stat-card {
  background: #fff;
  padding: 24px;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.1);
  display: flex;
  align-items: center;
  gap: 20px;
  text-decoration: none;
  color: inherit;
  transition: all 0.3s;
  cursor: pointer;
}

.stat-card:hover {
  transform: translateY(-5px);
  box-shadow: 0 8px 24px rgba(64, 158, 255, 0.2);
}

.stat-icon {
  font-size: 48px;
}

.stat-value {
  font-size: 32px;
  font-weight: bold;
  color: #409eff;
  margin-bottom: 8px;
}

.stat-label {
  color: #666;
  font-size: 14px;
}

.quick-actions {
  background: #fff;
  padding: 24px;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.1);
}

.quick-actions h2 {
  font-size: 20px;
  margin-bottom: 20px;
  color: #333;
}

.actions-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 20px;
}

.action-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 30px;
  background: #f5f5f5;
  border-radius: 8px;
  text-decoration: none;
  transition: all 0.3s;
}

.action-card:hover {
  background: #409eff;
  color: #fff;
  transform: translateY(-5px);
}

.action-icon {
  font-size: 48px;
  margin-bottom: 15px;
}

.action-text {
  font-size: 16px;
  color: #333;
}

.action-card:hover .action-text {
  color: #fff;
}

.footer {
  background: #fff;
  padding: 20px 0;
  text-align: center;
  color: #666;
  margin-top: auto;
}
</style>