<template>
  <div class="user-profile">
    <!-- 导航栏 -->
    <header class="navbar">
      <div class="container">
        <div class="logo">
          <router-link to="/">
            <el-icon class="logo-icon"><Goods /></el-icon>
            <span class="logo-text">家具商城</span>
          </router-link>
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
            <router-link to="/profile" class="nav-item active">个人中心</router-link>
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

    <!-- 个人中心内容 -->
    <div class="profile-section">
      <div class="container">
        <!-- 用户信息卡片 -->
        <div class="user-card">
          <div class="avatar-section">
            <div class="avatar">
              <el-icon class="avatar-icon"><User /></el-icon>
            </div>
            <div class="user-info">
              <h2 class="username">{{ userInfo.username }}</h2>
              <p class="user-role">普通用户</p>
            </div>
          </div>
          <div class="stats-row">
            <div class="stat-item">
              <div class="stat-value">{{ orderStats.total || 0 }}</div>
              <div class="stat-label">全部订单</div>
            </div>
            <div class="stat-item">
              <div class="stat-value">{{ orderStats.pending || 0 }}</div>
              <div class="stat-label">待付款</div>
            </div>
            <div class="stat-item">
              <div class="stat-value">{{ orderStats.shipped || 0 }}</div>
              <div class="stat-label">待收货</div>
            </div>
            <div class="stat-item">
              <div class="stat-value">{{ orderStats.reviewed || 0 }}</div>
              <div class="stat-label">待评价</div>
            </div>
          </div>
        </div>

        <div class="profile-content">
          <!-- 左侧菜单 -->
          <aside class="sidebar">
            <div class="menu-item" :class="{ active: activeTab === 'profile' }" @click="activeTab = 'profile'">
              <el-icon><User /></el-icon>
              <span>基本信息</span>
            </div>
            <div class="menu-item" :class="{ active: activeTab === 'orders' }" @click="goToOrders">
              <el-icon><List /></el-icon>
              <span>我的订单</span>
            </div>
            <div class="menu-item" :class="{ active: activeTab === 'address' }" @click="goToAddress">
              <el-icon><House /></el-icon>
              <span>收货地址</span>
            </div>
            <div class="menu-item" :class="{ active: activeTab === 'password' }" @click="activeTab = 'password'">
              <el-icon><Lock /></el-icon>
              <span>修改密码</span>
            </div>
          </aside>

          <!-- 右侧内容区 -->
          <main class="main-content">
            <!-- 基本信息 -->
            <div v-if="activeTab === 'profile'" class="content-card">
              <h3 class="card-title">基本信息</h3>
              <el-form :model="userInfo" label-width="120px" class="info-form">
                <el-form-item label="用户名">
                  <el-input v-model="userInfo.username" placeholder="请输入用户名" />
                </el-form-item>
                <el-form-item label="邮箱">
                  <el-input v-model="userInfo.email" placeholder="请输入邮箱" />
                </el-form-item>
                <el-form-item label="手机号">
                  <el-input v-model="userInfo.phone" placeholder="请输入手机号" />
                </el-form-item>
                <el-form-item label="姓名">
                  <el-input v-model="userInfo.name" placeholder="请输入姓名" />
                </el-form-item>
                <el-form-item label="性别">
                  <el-radio-group v-model="userInfo.gender">
                    <el-radio :label="0">男</el-radio>
                    <el-radio :label="1">女</el-radio>
                    <el-radio :label="2">保密</el-radio>
                  </el-radio-group>
                </el-form-item>
                <el-form-item class="form-actions">
                  <el-button type="primary" @click="updateProfile" class="btn-save">保存修改</el-button>
                  <el-button @click="resetForm">重置</el-button>
                </el-form-item>
              </el-form>
            </div>

            <!-- 修改密码 -->
            <div v-if="activeTab === 'password'" class="content-card">
              <h3 class="card-title">修改密码</h3>
              <el-form :model="passwordForm" label-width="120px" class="info-form">
                <el-form-item label="当前密码">
                  <el-input type="password" v-model="passwordForm.oldPassword" placeholder="请输入当前密码" />
                </el-form-item>
                <el-form-item label="新密码">
                  <el-input type="password" v-model="passwordForm.newPassword" placeholder="请输入新密码（至少6位）" />
                </el-form-item>
                <el-form-item label="确认新密码">
                  <el-input type="password" v-model="passwordForm.confirmPassword" placeholder="请再次输入新密码" />
                </el-form-item>
                <el-form-item class="form-actions">
                  <el-button type="primary" @click="submitPasswordChange" class="btn-save">确认修改</el-button>
                  <el-button @click="resetPasswordForm">重置</el-button>
                </el-form-item>
              </el-form>
            </div>
          </main>
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
import { userAPI, orderAPI } from '../../api'
import { ElMessage } from 'element-plus'
import { User, List, House, Lock, Goods } from '@element-plus/icons-vue'

const router = useRouter()
const userStore = useUserStore()
const cartStore = useCartStore()

const activeTab = ref('profile')
const loading = ref(false)

const userInfo = ref({
  username: '',
  email: '',
  phone: '',
  name: '',
  gender: 0
})

const passwordForm = ref({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})

const orderStats = ref({
  total: 0,
  pending: 0,
  shipped: 0,
  reviewed: 0
})

onMounted(async () => {
  if (!userStore.isAuthenticated) {
    router.push('/login')
    return
  }
  await Promise.all([
    loadUserInfo(),
    loadOrderStats(),
    cartStore.getCartList()
  ])
})

const loadUserInfo = async () => {
  try {
    loading.value = true
    const response = await userAPI.getCurrentUser()
    const user = response.data.data
    userInfo.value = {
      username: user.username || '',
      email: user.email || '',
      phone: user.phone || '',
      name: user.name || '',
      gender: user.gender || 0
    }
    userStore.updateUserInfo(userInfo.value)
  } catch (error) {
    console.error('获取用户信息失败:', error)
    ElMessage.error('获取用户信息失败')
  } finally {
    loading.value = false
  }
}

const loadOrderStats = async () => {
  try {
    const response = await orderAPI.getOrderStats()
    orderStats.value = response.data.data || { total: 0, pending: 0, shipped: 0, reviewed: 0 }
  } catch (error) {
    console.error('获取订单统计失败:', error)
    // 如果API不存在，保持默认值
  }
}

const updateProfile = async () => {
  try {
    await userAPI.updateUser(userInfo.value)
    userStore.updateUserInfo(userInfo.value)
    ElMessage.success('个人信息更新成功')
  } catch (error) {
    console.error('更新个人信息失败:', error)
    ElMessage.error(error.message || '更新个人信息失败')
  }
}

const resetForm = () => {
  loadUserInfo()
}

const resetPasswordForm = () => {
  passwordForm.value = {
    oldPassword: '',
    newPassword: '',
    confirmPassword: ''
  }
}

const submitPasswordChange = async () => {
  try {
    if (!passwordForm.value.oldPassword) {
      ElMessage.error('请输入当前密码')
      return
    }
    
    if (!passwordForm.value.newPassword) {
      ElMessage.error('请输入新密码')
      return
    }
    
    if (passwordForm.value.newPassword !== passwordForm.value.confirmPassword) {
      ElMessage.error('两次输入的密码不一致')
      return
    }
    
    if (passwordForm.value.newPassword.length < 6) {
      ElMessage.error('密码长度不能少于6位')
      return
    }
    
    await userAPI.changePassword({
      oldPassword: passwordForm.value.oldPassword,
      newPassword: passwordForm.value.newPassword
    })
    ElMessage.success('密码修改成功')
    activeTab.value = 'profile'
    resetPasswordForm()
  } catch (error) {
    console.error('修改密码失败:', error)
    ElMessage.error(error.message || '修改密码失败')
  }
}

const goToOrders = () => {
  router.push('/orders')
}

const goToAddress = () => {
  router.push('/address')
}

const logout = () => {
  userStore.logout()
  ElMessage.success('退出成功')
  router.push('/home')
}
</script>

<style scoped>
.user-profile {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  background-color: #f5f7fa;
}

/* 导航栏 */
.navbar {
  background-color: #ffffff;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
  padding: 12px 0;
  position: sticky;
  top: 0;
  z-index: 100;
}

.navbar .container {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.logo {
  display: flex;
  align-items: center;
}

.logo a {
  display: flex;
  align-items: center;
  text-decoration: none;
  color: #333;
  font-weight: bold;
}

.logo-icon {
  font-size: 28px;
  color: #667eea;
  margin-right: 8px;
}

.logo-text {
  font-size: 20px;
  font-weight: 600;
  color: #333;
}

.nav {
  display: flex;
  align-items: center;
  gap: 30px;
}

.nav-item {
  text-decoration: none;
  color: #666;
  font-size: 15px;
  padding: 8px 12px;
  border-radius: 6px;
  transition: all 0.3s ease;
}

.nav-item:hover {
  color: #667eea;
  background-color: #f0f5ff;
}

.nav-item.active {
  color: #667eea;
  font-weight: 500;
  background-color: #f0f5ff;
}

.user {
  display: flex;
  align-items: center;
  gap: 15px;
}

.welcome {
  font-size: 14px;
  color: #666;
  margin-right: 10px;
}

.btn {
  padding: 8px 16px;
  font-size: 14px;
  border: none;
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.3s ease;
  text-decoration: none;
  background-color: #f5f5f5;
  color: #666;
}

.btn:hover {
  background-color: #e8e8e8;
}

.btn-primary {
  background-color: #667eea;
  color: #fff;
}

.btn-primary:hover {
  background-color: #5a6fd6;
}

.cart-badge {
  background-color: #ff4d4f;
  color: #fff;
  font-size: 12px;
  padding: 2px 6px;
  border-radius: 10px;
  margin-left: 4px;
}

.profile-section {
  flex: 1;
  padding: 40px 0;
}

.container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 20px;
}

/* 用户卡片 */
.user-card {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-radius: 16px;
  padding: 40px;
  margin-bottom: 30px;
  color: #fff;
  box-shadow: 0 10px 40px rgba(102, 126, 234, 0.3);
}

.avatar-section {
  display: flex;
  align-items: center;
  margin-bottom: 30px;
}

.avatar {
  width: 100px;
  height: 100px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.2);
  display: flex;
  align-items: center;
  justify-content: center;
  margin-right: 25px;
  border: 3px solid rgba(255, 255, 255, 0.3);
}

.avatar-icon {
  font-size: 48px;
}

.user-info {
  display: flex;
  flex-direction: column;
}

.username {
  font-size: 28px;
  font-weight: bold;
  margin: 0 0 8px 0;
}

.user-role {
  font-size: 14px;
  opacity: 0.8;
}

.stats-row {
  display: flex;
  justify-content: space-around;
  padding-top: 25px;
  border-top: 1px solid rgba(255, 255, 255, 0.2);
}

.stat-item {
  text-align: center;
}

.stat-value {
  font-size: 28px;
  font-weight: bold;
  margin-bottom: 5px;
}

.stat-label {
  font-size: 14px;
  opacity: 0.8;
}

/* 内容区域 */
.profile-content {
  display: flex;
  gap: 24px;
}

/* 侧边栏 */
.sidebar {
  width: 200px;
  background: #fff;
  border-radius: 12px;
  padding: 20px 0;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
}

.menu-item {
  display: flex;
  align-items: center;
  padding: 16px 24px;
  color: #666;
  cursor: pointer;
  transition: all 0.3s ease;
  font-size: 15px;
}

.menu-item:hover {
  background-color: #f0f5ff;
  color: #1890ff;
}

.menu-item.active {
  background-color: #e6f7ff;
  color: #1890ff;
  border-left: 3px solid #1890ff;
}

.menu-item el-icon {
  margin-right: 12px;
  font-size: 18px;
}

/* 主内容区 */
.main-content {
  flex: 1;
}

.content-card {
  background: #fff;
  border-radius: 12px;
  padding: 30px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
}

.card-title {
  font-size: 18px;
  font-weight: bold;
  color: #333;
  margin-bottom: 24px;
  padding-bottom: 15px;
  border-bottom: 1px solid #f0f0f0;
}

.info-form {
  max-width: 500px;
}

.disabled-input {
  background-color: #f5f5f5;
}

.form-actions {
  padding-left: 120px !important;
}

.btn-save {
  margin-right: 12px;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .user-card {
    padding: 25px;
  }
  
  .avatar {
    width: 70px;
    height: 70px;
    margin-right: 15px;
  }
  
  .avatar-icon {
    font-size: 32px;
  }
  
  .username {
    font-size: 22px;
  }
  
  .stat-value {
    font-size: 22px;
  }
  
  .stats-row {
    flex-wrap: wrap;
    gap: 15px;
  }
  
  .stat-item {
    flex: 1;
    min-width: 80px;
  }
  
  .profile-content {
    flex-direction: column;
  }
  
  .sidebar {
    width: 100%;
    display: flex;
    overflow-x: auto;
  }
  
  .menu-item {
    flex-shrink: 0;
    border-left: none;
    border-bottom: 3px solid transparent;
    padding: 12px 20px;
  }
  
  .menu-item.active {
    border-left: none;
    border-bottom: 3px solid #1890ff;
    background-color: transparent;
  }
  
  .form-actions {
    padding-left: 0 !important;
  }
}
</style>
