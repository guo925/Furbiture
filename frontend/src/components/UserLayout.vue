<template>
  <div class="shop-shell">
    <div class="top-strip">
      <div class="shop-container top-strip-inner">
        <span>欢迎来到橙家优选</span>
        <div class="top-links">
          <button type="button" class="theme-btn" @click="theme.toggleTheme()" :title="theme.isDark.value ? '切换亮色' : '切换深色'">
            <el-icon><Sunny v-if="theme.isDark.value" /><Moon v-else /></el-icon>
          </button>
          <template v-if="userStore.isAuthenticated">
            <router-link to="/profile">{{ userStore.user?.username || '个人中心' }}</router-link>
            <router-link to="/orders">我的订单</router-link>
            <router-link to="/favorites">我的收藏</router-link>
            <button type="button" @click="logout">退出</button>
          </template>
          <template v-else>
            <router-link to="/login">登录</router-link>
            <router-link to="/register">免费注册</router-link>
          </template>
        </div>
      </div>
    </div>

    <header class="shop-header">
      <div class="shop-container header-inner">
        <router-link to="/home" class="brand">
          <span class="brand-mark">橙</span>
          <span>
            <strong>橙家优选</strong>
            <small>品质家具商城</small>
          </span>
        </router-link>

        <div class="search-bar" ref="searchRef">
          <el-input
            v-model="keyword"
            placeholder="搜索沙发、床、餐桌、收纳柜"
            @focus="showDropdown = true"
            @blur="onBlur"
            @keyup.enter="submitSearch"
          >
            <template #append>
              <el-button native-type="submit" color="#ff5000" @click="submitSearch">搜索</el-button>
            </template>
          </el-input>
          <!-- 搜索下拉：历史 + 建议 -->
          <div v-if="showDropdown && (searchHistory.length || suggestions.length)" class="search-dropdown">
            <div v-if="searchHistory.length" class="dropdown-section">
              <div class="dropdown-head">
                <span>搜索历史</span>
                <button type="button" @click="clearSearchHistory">清空</button>
              </div>
              <div class="dropdown-list">
                <span v-for="word in searchHistory" :key="word" @mousedown.prevent="searchWord(word)">{{ word }}</span>
              </div>
            </div>
            <div v-if="suggestions.length" class="dropdown-section">
              <div class="dropdown-head"><span>搜索建议</span></div>
              <div class="dropdown-list">
                <span v-for="word in suggestions" :key="word" @mousedown.prevent="searchWord(word)">{{ word }}</span>
              </div>
            </div>
          </div>
          <div class="quick-words">
            <button v-for="word in quickWords" :key="word" type="button" @click="searchWord(word)">{{ word }}</button>
          </div>
        </div>

        <router-link to="/cart" class="cart-entry">
          <el-icon><ShoppingCart /></el-icon>
          <span>购物车</span>
          <em v-if="cartStore.totalQuantity > 0">{{ cartStore.totalQuantity }}</em>
        </router-link>
      </div>

      <nav class="main-nav">
        <div class="shop-container nav-inner">
          <router-link to="/home">首页</router-link>
          <router-link to="/products">全部商品</router-link>
          <router-link to="/orders">我的订单</router-link>
          <router-link to="/address">地址管理</router-link>
        </div>
      </nav>
    </header>

    <main class="shop-main">
      <slot />
    </main>

    <footer class="shop-footer">
      <div class="shop-container footer-grid">
        <div>
          <strong>橙家优选</strong>
          <p>家具、软装、家居灵感一站式选购。</p>
        </div>
        <span>正品保障</span>
        <span>48小时发货</span>
        <span>无忧售后</span>
      </div>
    </footer>
  </div>
</template>

<script setup>
import { ref, watch, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ShoppingCart, Sunny, Moon } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { useUserStore } from '../stores/user'
import { useCartStore } from '../stores/cart'
import { useSearchHistory } from '../composables/useSearchHistory'
import { useTheme } from '../composables/useTheme'
import request from '../api/request'

const theme = useTheme()
const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const cartStore = useCartStore()
const { getHistory, addSearch, clearHistory } = useSearchHistory()
const keyword = ref(route.query.keyword || '')
const quickWords = ['沙发', '床', '餐桌', '收纳', '北欧']
const showDropdown = ref(false)
const suggestions = ref([])
const searchHistory = ref(getHistory())
let debounceTimer = null

watch(() => route.query.keyword, value => {
  keyword.value = value || ''
})

onMounted(() => {
  if (userStore.isAuthenticated) {
    cartStore.getCartList()
  }
})

const submitSearch = () => {
  if (keyword.value && keyword.value.trim()) {
    addSearch(keyword.value.trim())
    searchHistory.value = getHistory()
  }
  showDropdown.value = false
  router.push({ path: '/products', query: keyword.value ? { keyword: keyword.value } : {} })
}

const searchWord = (word) => {
  keyword.value = word
  submitSearch()
}

const clearSearchHistory = () => {
  clearHistory()
  searchHistory.value = []
}

// 输入时防抖获取搜索建议
watch(keyword, (val) => {
  if (debounceTimer) clearTimeout(debounceTimer)
  if (!val || !val.trim()) {
    suggestions.value = []
    return
  }
  debounceTimer = setTimeout(async () => {
    try {
      const res = await request.get('/products/suggestions', { params: { keyword: val.trim() } })
      suggestions.value = res.data.data || []
    } catch {
      suggestions.value = []
    }
  }, 200)
})

const onBlur = () => {
  // 延迟关闭以确保 mousedown 先触发
  setTimeout(() => { showDropdown.value = false }, 150)
}

const logout = () => {
  userStore.logout()
  ElMessage.success('退出成功')
  router.push('/home')
}
</script>

<style scoped>
.shop-shell {
  min-height: 100vh;
  background: #f6f6f6;
  color: #222;
}

.shop-container {
  width: min(1200px, calc(100% - 32px));
  margin: 0 auto;
}

.top-strip {
  background: #f5f5f5;
  border-bottom: 1px solid #e8e8e8;
  font-size: 13px;
  color: #666;
}

.top-strip-inner,
.top-links,
.header-inner,
.nav-inner,
.footer-grid {
  display: flex;
  align-items: center;
}

.top-strip-inner {
  justify-content: space-between;
  height: 34px;
}

.top-links {
  gap: 16px;
}

.top-links a,
.top-links button,
.nav-inner a {
  color: inherit;
  text-decoration: none;
  background: none;
  border: 0;
  cursor: pointer;
  font: inherit;
}

.top-links a:hover,
.top-links button:hover,
.nav-inner a:hover,
.nav-inner a.router-link-active {
  color: #ff5000;
}

.shop-header {
  background: #fff;
  position: sticky;
  top: 0;
  z-index: 20;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);
}

.header-inner {
  height: 104px;
  gap: 28px;
}

.brand {
  display: flex;
  align-items: center;
  gap: 10px;
  min-width: 190px;
  color: #ff5000;
  text-decoration: none;
}

.brand-mark {
  width: 44px;
  height: 44px;
  border-radius: 8px;
  background: var(--color-primary-gradient);
  color: #fff;
  display: grid;
  place-items: center;
  font-size: 24px;
  font-weight: 800;
}

.brand strong,
.brand small {
  display: block;
}

.brand strong {
  font-size: 24px;
  line-height: 1.1;
}

.brand small {
  margin-top: 4px;
  color: #777;
  font-size: 12px;
}

.search-bar {
  flex: 1;
  min-width: 260px;
  position: relative;
}

.search-dropdown {
  position: absolute;
  top: 100%;
  left: 0;
  right: 0;
  background: #fff;
  border-radius: 0 0 var(--radius-md) var(--radius-md);
  box-shadow: var(--shadow-lg);
  z-index: 30;
  padding: 8px 0;
  margin-top: 2px;
  border: 1px solid var(--color-border-light);
}

.dropdown-section {
  padding: 4px 12px;
}

.dropdown-section + .dropdown-section {
  border-top: 1px solid var(--color-border-light);
}

.dropdown-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 6px 0;
  font-size: 12px;
  color: var(--color-text-muted);
}

.dropdown-head button {
  border: 0;
  background: none;
  color: var(--color-primary);
  cursor: pointer;
  font-size: 12px;
}

.dropdown-list {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  padding-bottom: 6px;
}

.dropdown-list span {
  padding: 4px 12px;
  border-radius: var(--radius-full);
  background: var(--color-primary-light);
  color: var(--color-text-secondary);
  cursor: pointer;
  font-size: 13px;
  transition: all var(--transition-fast);
}

.dropdown-list span:hover {
  background: var(--color-primary);
  color: #fff;
}

.search-bar :deep(.el-input__wrapper) {
  border-radius: 6px 0 0 6px;
  box-shadow: 0 0 0 2px #ff5000 inset;
}

.search-bar :deep(.el-input-group__append) {
  background: #ff5000;
  border-color: #ff5000;
  box-shadow: none;
}

.search-bar :deep(.el-button) {
  color: #fff;
  border: 0;
  font-weight: 700;
}

.quick-words {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-top: 8px;
}

.quick-words button {
  border: 0;
  background: transparent;
  color: #888;
  cursor: pointer;
  font-size: 12px;
}

.quick-words button:hover {
  color: #ff5000;
}

.cart-entry {
  width: 118px;
  height: 42px;
  border: 1px solid #ffcab8;
  border-radius: 6px;
  color: #ff5000;
  background: #fff7f3;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  text-decoration: none;
  position: relative;
}

.cart-entry em {
  position: absolute;
  top: -9px;
  right: -9px;
  min-width: 20px;
  height: 20px;
  padding: 0 6px;
  border-radius: 10px;
  background: #ff0036;
  color: #fff;
  font-size: 12px;
  line-height: 20px;
  text-align: center;
  font-style: normal;
}

.main-nav {
  border-top: 1px solid #f1f1f1;
}

.nav-inner {
  height: 38px;
  gap: 34px;
  color: #333;
  font-weight: 700;
}

.shop-main {
  min-height: calc(100vh - 236px);
}

.shop-footer {
  margin-top: 40px;
  background: #fff;
  border-top: 1px solid #e8e8e8;
  color: #666;
}

.footer-grid {
  min-height: 96px;
  justify-content: space-between;
  gap: 20px;
}

.footer-grid strong {
  color: #222;
  font-size: 18px;
}

.footer-grid p {
  margin-top: 6px;
  font-size: 13px;
}

.footer-grid span {
  font-weight: 700;
}

@media (max-width: 820px) {
  .top-strip-inner {
    height: auto;
    padding: 8px 0;
    align-items: flex-start;
    gap: 8px;
  }

  .header-inner {
    height: auto;
    padding: 14px 0;
    flex-wrap: wrap;
  }

  .brand {
    min-width: 0;
  }

  .cart-entry {
    width: 104px;
  }

  .nav-inner {
    overflow-x: auto;
    gap: 24px;
  }

  .footer-grid {
    flex-wrap: wrap;
    padding: 20px 0;
  }
}
</style>
