<template>
  <div class="admin-layout">
    <!-- 侧边栏 -->
    <aside class="sidebar" :class="{ collapsed: sidebarCollapsed }">
      <div class="sidebar-header">
        <h3>管理后台</h3>
      </div>
      <nav class="sidebar-nav">
        <router-link to="/admin/dashboard" class="nav-item">
          <el-icon><House /></el-icon>
          <span>控制台</span>
        </router-link>
        <router-link to="/admin/products" class="nav-item">
          <el-icon><Goods /></el-icon>
          <span>商品管理</span>
        </router-link>
        <router-link to="/admin/categories" class="nav-item">
          <el-icon><List /></el-icon>
          <span>分类管理</span>
        </router-link>
        <router-link to="/admin/orders" class="nav-item">
          <el-icon><Ticket /></el-icon>
          <span>订单管理</span>
        </router-link>
        <router-link to="/admin/users" class="nav-item">
          <el-icon><User /></el-icon>
          <span>用户管理</span>
        </router-link>
      </nav>
      <div class="sidebar-footer">
        <button @click="logout" class="btn">退出登录</button>
      </div>
    </aside>

    <!-- 主内容区 -->
    <main class="main-content">
      <!-- 顶部导航 -->
      <header class="main-header">
        <div class="header-left">
          <button @click="toggleSidebar" class="menu-btn">
            <el-icon><Menu /></el-icon>
          </button>
        </div>
        <div class="header-right">
          <span class="username">{{ userStore.user?.username }}</span>
        </div>
      </header>

      <!-- 页面内容 -->
      <div class="content-wrapper">
        <router-view />
      </div>
    </main>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '../../stores/user'
import { House, Goods, List, Ticket, User, Menu } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'

const router = useRouter()
const userStore = useUserStore()
const sidebarCollapsed = ref(false)

onMounted(() => {
  if (!userStore.isAuthenticated || userStore.user?.role !== 'ADMIN') {
    router.push('/login')
  }
})

const toggleSidebar = () => {
  sidebarCollapsed.value = !sidebarCollapsed.value
}

const logout = () => {
  userStore.logout()
  ElMessage.success('退出成功')
  router.push('/login')
}
</script>

<style scoped>
.admin-layout {
  display: flex;
  min-height: 100vh;
  background: #f5f7fa;
}

.sidebar {
  width: 200px;
  background: #2c3e50;
  color: #fff;
  transition: width 0.3s;
  position: relative;
  min-height: 100vh;
}

.sidebar.collapsed {
  width: 60px;
}

.sidebar-header {
  padding: 20px;
  border-bottom: 1px solid #34495e;
}

.sidebar-header h3 {
  margin: 0;
  font-size: 18px;
  font-weight: bold;
}

.sidebar-nav {
  padding: 10px 0;
}

.nav-item {
  display: flex;
  align-items: center;
  padding: 12px 20px;
  color: #ecf0f1;
  text-decoration: none;
  transition: background 0.3s;
}

.nav-item:hover {
  background: #34495e;
}

.nav-item.router-link-active {
  background: #3498db;
}

.sidebar-footer {
  position: absolute;
  bottom: 0;
  width: 100%;
  padding: 20px;
  border-top: 1px solid #34495e;
  box-sizing: border-box;
}

.btn {
  width: 100%;
  padding: 10px;
  background: #e74c3c;
  color: #fff;
  border: none;
  border-radius: 4px;
  cursor: pointer;
}

.main-content {
  flex: 1;
  display: flex;
  flex-direction: column;
}

.main-header {
  height: 60px;
  background: #fff;
  padding: 0 20px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  box-shadow: 0 2px 4px rgba(0,0,0,0.1);
}

.menu-btn {
  background: none;
  border: none;
  cursor: pointer;
  font-size: 20px;
}

.username {
  font-size: 14px;
  color: #333;
}

.content-wrapper {
  flex: 1;
  padding: 20px;
  overflow-y: auto;
}
</style>