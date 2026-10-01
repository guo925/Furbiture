<template>
  <el-container class="admin-layout">
    <!-- 侧边栏：桌面端（>1024px）固定显示；窄屏改用下方的抽屉，避免挤压内容区 -->
    <el-aside v-if="!isNarrow" :width="isCollapse ? '64px' : '220px'" class="admin-aside">
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
        <el-menu-item v-for="item in navItems" :key="item.path" :index="item.path">
          <el-icon><component :is="item.icon" /></el-icon>
          <template #title>{{ item.label }}</template>
        </el-menu-item>
      </el-menu>

      <div class="aside-footer">
        <el-button text class="collapse-btn" @click="isCollapse = !isCollapse">
          <el-icon><Fold v-if="!isCollapse" /><Expand v-else /></el-icon>
        </el-button>
      </div>
    </el-aside>

    <!-- 窄屏抽屉导航：菜单项与侧边栏复用同一份 navItems，只维护一处 -->
    <el-drawer
      v-model="drawerVisible"
      direction="ltr"
      size="220px"
      :with-header="false"
      class="admin-nav-drawer"
      aria-label="导航菜单"
    >
      <div class="aside-header">
        <div class="logo-icon">F</div>
        <span class="logo-text">Furbiture 管理</span>
      </div>
      <el-menu
        :default-active="activeMenu"
        background-color="#1a1a2e"
        text-color="#a0aec0"
        active-text-color="#ffffff"
        router
        class="aside-menu"
        @select="drawerVisible = false"
      >
        <el-menu-item v-for="item in navItems" :key="item.path" :index="item.path">
          <el-icon><component :is="item.icon" /></el-icon>
          <template #title>{{ item.label }}</template>
        </el-menu-item>
      </el-menu>
    </el-drawer>

    <!-- 主区域 -->
    <el-container>
      <!-- 顶部栏 -->
      <el-header class="admin-header">
        <div class="header-left">
          <!-- 窄屏下侧边栏已收起，用汉堡按钮唤出抽屉导航 -->
          <el-button v-if="isNarrow" text class="nav-toggle" aria-label="打开导航菜单" @click="drawerVisible = true">
            <el-icon :size="20"><Menu /></el-icon>
          </el-button>
          <el-breadcrumb separator="/">
            <el-breadcrumb-item v-if="!isNarrow">管理后台</el-breadcrumb-item>
            <el-breadcrumb-item>{{ currentTitle }}</el-breadcrumb-item>
          </el-breadcrumb>
        </div>
        <div class="header-right">
          <el-button circle @click="theme.toggleTheme()" :title="theme.isDark.value ? '切换亮色' : '切换深色'" style="margin-right: 12px">
            <el-icon><Sunny v-if="theme.isDark.value" /><Moon v-else /></el-icon>
          </el-button>
          <el-tag v-if="!isNarrow" type="success" size="small" effect="dark" style="margin-right: 16px">
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
import { ref, computed, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import {
  DataAnalysis, Goods, Grid, Document, User,
  Fold, Expand, ArrowDown, CircleCheck, Key, SwitchButton,
  Sunny, Moon, Menu
} from '@element-plus/icons-vue'
import { useTheme } from '../../composables/useTheme'
import { useBreakpoint } from '../../composables/useBreakpoint'
import { useUserStore } from '../../stores/user'
import { confirm } from '../../composables/useConfirm'

const theme = useTheme()
const { isNarrow } = useBreakpoint()

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()
const isCollapse = ref(false)
const drawerVisible = ref(false)
const username = computed(() => userStore.user?.username || '管理员')

const activeMenu = computed(() => route.path)

/**
 * 导航菜单唯一数据源：侧边栏与窄屏抽屉两处模板共用，
 * 面包屑标题也从这里取，避免"菜单改了忘了改标题"的经典漏改。
 */
const navItems = [
  { path: '/admin/dashboard', label: '控制台', icon: DataAnalysis },
  { path: '/admin/products', label: '商品管理', icon: Goods },
  { path: '/admin/categories', label: '分类管理', icon: Grid },
  { path: '/admin/orders', label: '订单管理', icon: Document },
  { path: '/admin/users', label: '用户管理', icon: User }
]

const currentTitle = computed(() => navItems.find(item => item.path === route.path)?.label || '')

// 窗口从窄屏拖回桌面时收起抽屉，否则抽屉会残留在已经显示侧边栏的界面上
watch(isNarrow, (narrow) => {
  if (!narrow) drawerVisible.value = false
})

const handleLogout = async () => {
  if (!await confirm('确定要退出登录吗？')) return
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
  background: var(--color-primary-gradient);
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
  background: var(--color-primary-gradient) !important;
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

.nav-toggle {
  margin-right: 8px;
  padding: 0 6px;
  color: #718096;
}

.header-left :deep(.el-breadcrumb__inner) {
  color: #718096;
  font-weight: 400;
}

.header-left :deep(.el-breadcrumb__inner.is-link:hover) {
  color: var(--color-primary);
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

/* ===== 响应式：断点取值见 composables/useBreakpoint.js ===== */

/* 平板及以下：内容区与顶栏内边距收窄，把宽度还给数据本身 */
@media (max-width: 1024px) {
  .admin-header {
    padding: 0 12px;
  }

  .admin-main {
    padding: 12px;
  }
}

/* 移动端：顶栏挤不下完整用户名，只留头像与下拉箭头 */
@media (max-width: 640px) {
  .user-name {
    display: none;
  }
}
</style>

<style>
/* el-drawer 的 DOM 由 Element Plus 渲染，scoped 选择器命中不到，故单独放全局块。
   选择器一律以 .admin-nav-drawer 起头，不会污染其他页面。 */
.admin-nav-drawer .el-drawer__body {
  padding: 0;
  background: linear-gradient(180deg, #1a1a2e 0%, #16213e 100%);
  overflow: hidden;
}
</style>
