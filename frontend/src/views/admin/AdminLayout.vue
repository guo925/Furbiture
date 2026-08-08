<template>
  <el-container class="admin-layout">
    <!-- 侧边栏 -->
    <el-aside :width="isCollapse ? '64px' : '220px'" class="admin-aside">
      <div class="aside-header">
        <div class="logo-icon">F</div>
        <transition name="fade">
          <span v-show="!isCollapse" class="logo-text">Furbiture 管理</span>
        </transition>
      </div>

      <el-menu
        :default-active="activeMenu"
        :collapse="isCollapse"
        :collapse-transition="false"
        background-color="#1a1a2e"
        text-color="#a0aec0"
        active-text-color="#ffffff"
        router
        class="aside-menu"
      >
        <el-menu-item index="/admin/dashboard">
          <el-icon><DataAnalysis /></el-icon>
          <template #title>控制台</template>
        </el-menu-item>
        <el-menu-item index="/admin/products">
          <el-icon><Goods /></el-icon>
          <template #title>商品管理</template>
        </el-menu-item>
        <el-menu-item index="/admin/categories">
          <el-icon><Grid /></el-icon>
          <template #title>分类管理</template>
        </el-menu-item>
        <el-menu-item index="/admin/orders">
          <el-icon><Document /></el-icon>
          <template #title>订单管理</template>
        </el-menu-item>
        <el-menu-item index="/admin/users">
          <el-icon><User /></el-icon>
          <template #title>用户管理</template>
        </el-menu-item>
      </el-menu>

      <div class="aside-footer">
        <el-button text class="collapse-btn" @click="isCollapse = !isCollapse">
          <el-icon><Fold v-if="!isCollapse" /><Expand v-else /></el-icon>
        </el-button>
      </div>
    </el-aside>

    <!-- 主区域 -->
    <el-container>
      <!-- 顶部栏 -->
      <el-header class="admin-header">
        <div class="header-left">
          <el-breadcrumb separator="/">
            <el-breadcrumb-item>管理后台</el-breadcrumb-item>
            <el-breadcrumb-item>{{ currentTitle }}</el-breadcrumb-item>
          </el-breadcrumb>
        </div>
        <div class="header-right">
          <el-button circle @click="theme.toggleTheme()" :title="theme.isDark.value ? '切换亮色' : '切换深色'" style="margin-right: 12px">
            <el-icon><Sunny v-if="theme.isDark.value" /><Moon v-else /></el-icon>
          </el-button>
          <el-tag type="success" size="small" effect="dark" style="margin-right: 16px">
            <el-icon><CircleCheck /></el-icon> 系统运行中
          </el-tag>
          <el-dropdown trigger="click">
            <div class="user-info">
              <el-avatar :size="32" icon="UserFilled" />
              <span class="user-name">{{ username }}</span>
              <el-icon><ArrowDown /></el-icon>
            </div>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item disabled>
                  <el-icon><User /></el-icon> {{ username }}
                </el-dropdown-item>
                <el-dropdown-item disabled>
                  <el-icon><Key /></el-icon> 管理员
                </el-dropdown-item>
                <el-dropdown-item divided @click="handleLogout">
                  <el-icon><SwitchButton /></el-icon> 退出登录
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>

      <!-- 内容区 -->
      <el-main class="admin-main">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { ref, computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import {
  DataAnalysis, Goods, Grid, Document, User,
  Fold, Expand, ArrowDown, CircleCheck, Key, SwitchButton,
  Sunny, Moon
} from '@element-plus/icons-vue'
import { useTheme } from '../../composables/useTheme'

const theme = useTheme()
import { ElMessageBox } from 'element-plus'
import { useUserStore } from '../../stores/user'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()
const isCollapse = ref(false)
const username = computed(() => userStore.user?.username || '管理员')

const activeMenu = computed(() => route.path)

const titleMap = {
  '/admin/dashboard': '控制台',
  '/admin/products': '商品管理',
  '/admin/categories': '分类管理',
  '/admin/orders': '订单管理',
  '/admin/users': '用户管理'
}
const currentTitle = computed(() => titleMap[route.path] || '')

const handleLogout = async () => {
  await ElMessageBox.confirm('确定要退出登录吗？', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  })
  userStore.logout()
  router.push('/login')
}
</script>

<style scoped>
.admin-layout {
  height: 100vh;
  overflow: hidden;
}

/* 侧边栏 */
.admin-aside {
  background: linear-gradient(180deg, #1a1a2e 0%, #16213e 100%);
  display: flex;
  flex-direction: column;
  transition: width 0.3s;
  overflow: hidden;
  box-shadow: 2px 0 8px rgba(0, 0, 0, 0.15);
}

.aside-header {
  display: flex;
  align-items: center;
  padding: 20px 16px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
}

.logo-icon {
  width: 32px;
  height: 32px;
  background: linear-gradient(135deg, #667eea, #764ba2);
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-weight: 700;
  font-size: 16px;
  flex-shrink: 0;
}

.logo-text {
  margin-left: 12px;
  font-size: 16px;
  font-weight: 600;
  color: #fff;
  white-space: nowrap;
}

.aside-menu {
  flex: 1;
  border-right: none;
  padding-top: 8px;
}

.aside-menu .el-menu-item {
  margin: 2px 8px;
  border-radius: 8px;
  height: 44px;
  line-height: 44px;
}

.aside-menu .el-menu-item:hover {
  background: rgba(255, 255, 255, 0.08) !important;
}

.aside-menu .el-menu-item.is-active {
  background: linear-gradient(135deg, #667eea, #764ba2) !important;
}

.aside-footer {
  padding: 12px;
  border-top: 1px solid rgba(255, 255, 255, 0.08);
  display: flex;
  justify-content: center;
}

.collapse-btn {
  color: #a0aec0;
  font-size: 18px;
}

/* 顶部栏 */
.admin-header {
  background: #fff;
  border-bottom: 1px solid #edf2f7;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24px;
  height: 56px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05);
}

.header-left :deep(.el-breadcrumb__inner) {
  color: #718096;
  font-weight: 400;
}

.header-left :deep(.el-breadcrumb__inner.is-link:hover) {
  color: #667eea;
}

.header-right {
  display: flex;
  align-items: center;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  padding: 4px 8px;
  border-radius: 8px;
  transition: background 0.2s;
}

.user-info:hover {
  background: #f7fafc;
}

.user-name {
  font-size: 14px;
  color: #2d3748;
  font-weight: 500;
}

/* 主内容 */
.admin-main {
  background: #f7fafc;
  padding: 20px;
  overflow-y: auto;
  min-height: calc(100vh - 56px);
}

/* 过渡动画 */
.fade-enter-active, .fade-leave-active {
  transition: opacity 0.3s;
}
.fade-enter-from, .fade-leave-to {
  opacity: 0;
}
</style>
