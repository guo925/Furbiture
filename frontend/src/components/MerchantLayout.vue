<template>
  <div class="seller-layout">
    <aside class="seller-sidebar">
      <router-link to="/merchant" class="seller-brand">
        <span class="brand-mark">家</span>
        <span class="brand-copy">
          <small>家具商城</small>
          <strong>卖家中心</strong>
        </span>
      </router-link>

      <nav class="seller-nav">
        <router-link v-for="item in navItems" :key="item.path" :to="item.path" class="seller-nav-item">
          <el-icon><component :is="item.icon" /></el-icon>
          <span>{{ item.label }}</span>
        </router-link>
      </nav>
    </aside>

    <main class="seller-main">
      <header class="seller-topbar">
        <div>
          <h1>{{ title }}</h1>
          <p>{{ subtitle }}</p>
        </div>
        <div class="seller-account">
          <el-button size="small" circle @click="theme.toggleTheme()" :title="theme.isDark.value ? '切换亮色' : '切换深色'">
            <el-icon><Sunny v-if="theme.isDark.value" /><Moon v-else /></el-icon>
          </el-button>
          <el-tag type="success" effect="light" round>营业中</el-tag>
          <span>{{ userStore.user?.username || '商家' }}</span>
          <el-button size="small" @click="logout">退出</el-button>
        </div>
      </header>

      <section class="seller-content">
        <router-view />
      </section>
    </main>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '../stores/user'
import { DataBoard, Goods, List, Tickets, User, Sunny, Moon } from '@element-plus/icons-vue'
import { useTheme } from '../composables/useTheme'

const theme = useTheme()

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const title = computed(() => route.meta.title || '卖家中心')
const subtitle = computed(() => '管理商品、订单、类目和店铺资料')

const navItems = [
  { path: '/merchant', label: '经营工作台', icon: DataBoard },
  { path: '/merchant/products', label: '商品管理', icon: Goods },
  { path: '/merchant/orders', label: '订单管理', icon: Tickets },
  { path: '/merchant/categories', label: '分类管理', icon: List },
  { path: '/merchant/profile', label: '店铺资料', icon: User }
]

const logout = () => {
  userStore.logout()
  router.push('/login')
}
</script>

<style scoped>
.seller-layout {
  min-height: 100vh;
  display: grid;
  grid-template-columns: 236px 1fr;
  background:
    linear-gradient(180deg, #fff7ed 0, rgba(255, 247, 237, 0) 240px),
    #f4f6f9;
}

.seller-sidebar {
  background: linear-gradient(180deg, #202936 0%, #17202b 100%);
  color: #fff;
  padding: 18px 14px 24px;
  position: sticky;
  top: 0;
  height: 100vh;
  box-shadow: 8px 0 24px rgba(15, 23, 42, 0.08);
}

.seller-brand {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 10px 24px;
  color: #fff;
  text-decoration: none;
}

.brand-mark {
  width: 42px;
  height: 42px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: 10px;
  background: var(--color-primary-gradient);
  color: #fff;
  font-size: 20px;
  font-weight: 800;
  box-shadow: 0 10px 22px rgba(255, 122, 26, 0.24);
}

.brand-copy {
  display: grid;
  gap: 2px;
}

.brand-copy small {
  font-size: 13px;
  color: #cbd5e1;
}

.brand-copy strong {
  font-size: 22px;
  letter-spacing: 0;
}

.seller-nav {
  display: grid;
  gap: 6px;
}

.seller-nav-item {
  height: 44px;
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 0 14px;
  border-radius: 8px;
  color: #d7dde7;
  text-decoration: none;
  font-weight: 500;
  transition: background-color 0.16s ease, color 0.16s ease;
}

.seller-nav-item:hover,
.seller-nav-item.router-link-active {
  color: #fff;
  background: rgba(255, 122, 26, 0.18);
}

.seller-nav-item.router-link-active {
  box-shadow: inset 3px 0 0 var(--color-primary);
}

.seller-main {
  min-width: 0;
}

.seller-topbar {
  min-height: 82px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20px;
  padding: 16px 30px;
  background: rgba(255, 255, 255, 0.92);
  border-bottom: 1px solid #e7eaf0;
  backdrop-filter: blur(10px);
  position: sticky;
  top: 0;
  z-index: 10;
}

.seller-topbar h1 {
  margin: 0 0 6px;
  font-size: 26px;
  color: #17202b;
}

.seller-topbar p {
  margin: 0;
  color: #6b7280;
}

.seller-account {
  display: flex;
  align-items: center;
  gap: 12px;
  white-space: nowrap;
  min-height: 38px;
  padding: 0 0 0 16px;
  border-left: 1px solid #edf0f5;
}

.seller-content {
  padding: 24px 30px 44px;
}

@media (max-width: 860px) {
  .seller-layout {
    grid-template-columns: 1fr;
  }

  .seller-sidebar {
    position: static;
    height: auto;
  }

  .seller-nav {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .seller-topbar {
    align-items: flex-start;
    flex-direction: column;
  }
}
</style>
