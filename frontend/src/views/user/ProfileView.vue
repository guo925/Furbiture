<template>
  <UserLayout>
    <div class="shop-container profile-page">
      <!-- 用户信息卡片 -->
      <div class="user-card">
        <div class="avatar-section">
          <div class="avatar">
            <el-icon class="avatar-icon"><User /></el-icon>
          </div>
          <div class="user-meta">
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
          <div class="menu-item" @click="goToOrders">
            <el-icon><List /></el-icon>
            <span>我的订单</span>
          </div>
          <div class="menu-item" @click="goToAddress">
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
            <el-form :model="userInfo" label-width="100px" class="info-form">
              <el-form-item label="用户名">
                <el-input v-model="userInfo.username" disabled class="disabled-input" />
              </el-form-item>
              <el-form-item label="邮箱">
                <el-input v-model="userInfo.email" placeholder="请输入邮箱" />
              </el-form-item>
              <el-form-item label="手机号">
                <el-input v-model="userInfo.phone" placeholder="请输入手机号" />
              </el-form-item>
              <el-form-item>
                <el-button type="primary" @click="updateProfile">保存修改</el-button>
                <el-button @click="resetForm">重置</el-button>
              </el-form-item>
            </el-form>
          </div>

          <!-- 修改密码 -->
          <div v-if="activeTab === 'password'" class="content-card">
            <h3 class="card-title">修改密码</h3>
            <el-form :model="passwordForm" label-width="100px" class="info-form">
              <el-form-item label="当前密码">
                <el-input type="password" v-model="passwordForm.oldPassword" placeholder="请输入当前密码" />
              </el-form-item>
              <el-form-item label="新密码">
                <el-input type="password" v-model="passwordForm.newPassword" placeholder="请输入新密码（至少6位）" />
              </el-form-item>
              <el-form-item label="确认密码">
                <el-input type="password" v-model="passwordForm.confirmPassword" placeholder="请再次输入新密码" />
              </el-form-item>
              <el-form-item>
                <el-button type="primary" @click="submitPasswordChange">确认修改</el-button>
                <el-button @click="resetPasswordForm">重置</el-button>
              </el-form-item>
            </el-form>
          </div>
        </main>
      </div>
    </div>
  </UserLayout>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '../../stores/user'
import { useCartStore } from '../../stores/cart'
import { userAPI, orderAPI } from '../../api'
import { ElMessage } from 'element-plus'
import { User, List, House, Lock } from '@element-plus/icons-vue'
import UserLayout from '../../components/UserLayout.vue'

const router = useRouter()
const userStore = useUserStore()
const cartStore = useCartStore()

const activeTab = ref('profile')
const loading = ref(false)

const userInfo = ref({
  username: '',
  email: '',
  phone: ''
})

const passwordForm = ref({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})

const orderStats = ref({
  total: 0, pending: 0, shipped: 0, reviewed: 0
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
      phone: user.phone || ''
    }
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
    // API 不存在时保持默认值
  }
}

const updateProfile = async () => {
  try {
    await userAPI.updateUser({
      email: userInfo.value.email,
      phone: userInfo.value.phone
    })
    userStore.updateUserInfo(userInfo.value)
    ElMessage.success('个人信息更新成功')
  } catch (error) {
    ElMessage.error(error.message || '更新个人信息失败')
  }
}

const resetForm = () => { loadUserInfo() }

const resetPasswordForm = () => {
  passwordForm.value = { oldPassword: '', newPassword: '', confirmPassword: '' }
}

const submitPasswordChange = async () => {
  try {
    if (!passwordForm.value.oldPassword) return ElMessage.error('请输入当前密码')
    if (!passwordForm.value.newPassword) return ElMessage.error('请输入新密码')
    if (passwordForm.value.newPassword !== passwordForm.value.confirmPassword)
      return ElMessage.error('两次输入的密码不一致')
    if (passwordForm.value.newPassword.length < 6)
      return ElMessage.error('密码长度不能少于6位')

    await userAPI.changePassword({
      oldPassword: passwordForm.value.oldPassword,
      newPassword: passwordForm.value.newPassword
    })
    ElMessage.success('密码修改成功')
    activeTab.value = 'profile'
    resetPasswordForm()
  } catch (error) {
    ElMessage.error(error.message || '修改密码失败')
  }
}

const goToOrders = () => router.push('/orders')
const goToAddress = () => router.push('/address')
</script>

<style scoped>
.shop-container {
  width: min(1200px, calc(100% - 32px));
  margin: 0 auto;
}

.profile-page {
  padding: 28px 0;
}

.user-card {
  background: var(--color-primary-gradient, linear-gradient(135deg, #ff5000, #ff7a1a));
  border-radius: var(--radius-xl);
  padding: 36px;
  margin-bottom: 24px;
  color: #fff;
}

.avatar-section {
  display: flex;
  align-items: center;
  margin-bottom: 28px;
}

.avatar {
  width: 88px;
  height: 88px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.2);
  display: flex;
  align-items: center;
  justify-content: center;
  margin-right: 22px;
  border: 3px solid rgba(255, 255, 255, 0.3);
}

.avatar-icon { font-size: 42px; }

.username { font-size: 26px; font-weight: 700; margin: 0 0 6px 0; }
.user-role { font-size: 13px; opacity: 0.8; margin: 0; }

.stats-row {
  display: flex;
  justify-content: space-around;
  padding-top: 22px;
  border-top: 1px solid rgba(255, 255, 255, 0.2);
}

.stat-item { text-align: center; }
.stat-value { font-size: 26px; font-weight: 700; margin-bottom: 4px; }
.stat-label { font-size: 13px; opacity: 0.8; }

.profile-content { display: flex; gap: 22px; }

.sidebar {
  width: 180px;
  background: var(--color-bg-card, #fff);
  border-radius: var(--radius-lg);
  padding: 12px 0;
  box-shadow: var(--shadow-sm);
  flex-shrink: 0;
}

.menu-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 14px 20px;
  color: var(--color-text-secondary);
  cursor: pointer;
  font-size: 14px;
  transition: all var(--transition-fast);
}

.menu-item:hover { color: var(--color-primary); background: var(--color-primary-light); }
.menu-item.active { color: var(--color-primary); border-left: 3px solid var(--color-primary); background: var(--color-primary-light); }

.main-content { flex: 1; min-width: 0; }

.content-card {
  background: var(--color-bg-card, #fff);
  border-radius: var(--radius-lg);
  padding: 28px;
  box-shadow: var(--shadow-sm);
}

.card-title {
  font-size: 18px;
  font-weight: 700;
  margin-bottom: 22px;
  padding-bottom: 14px;
  border-bottom: 1px solid var(--color-border-light);
}

.info-form { max-width: 460px; }
.disabled-input { --el-input-bg-color: #f5f5f5; }

@media (max-width: 768px) {
  .user-card { padding: 22px; }
  .avatar { width: 64px; height: 64px; }
  .avatar-icon { font-size: 30px; }
  .username { font-size: 20px; }
  .stats-row { flex-wrap: wrap; gap: 12px; }
  .stat-item { flex: 1; min-width: 70px; }
  .profile-content { flex-direction: column; }
  .sidebar { width: 100%; display: flex; overflow-x: auto; }
  .menu-item { flex-shrink: 0; border-left: none !important; padding: 10px 16px; white-space: nowrap; }
}
</style>
