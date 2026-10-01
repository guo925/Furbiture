<template>
  <UserLayout>
    <div class="shop-container favorites-page">
      <div class="page-head">
        <h1>我的收藏</h1>
        <p>{{ favorites.length }} 件商品</p>
      </div>

      <div v-if="loading" class="loading-wrap">
        <el-skeleton :rows="6" animated />
      </div>

      <div v-else-if="favorites.length" class="fav-grid">
        <article v-for="fav in favorites" :key="fav.id" class="fav-card">
          <img :src="fav.productImage || fallbackImage" :alt="fav.productName" loading="lazy" @click="goToProduct(fav.productId)">
          <div class="fav-info">
            <router-link :to="`/product/${fav.productId}`">{{ fav.productName }}</router-link>
            <strong>¥{{ money(fav.productPrice) }}</strong>
          </div>
          <div class="fav-actions">
            <el-button size="small" type="primary" @click="addToCart(fav.productId)">
              <el-icon><ShoppingCart /></el-icon> 加入购物车
            </el-button>
            <el-button size="small" type="danger" text @click="removeFav(fav)">
              <el-icon><Delete /></el-icon>
            </el-button>
          </div>
        </article>
      </div>

      <div v-else class="empty">
        <el-empty description="还没有收藏商品">
          <el-button type="primary" @click="$router.push('/products')">去逛逛</el-button>
        </el-empty>
      </div>
    </div>
  </UserLayout>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ShoppingCart, Delete } from '@element-plus/icons-vue'
import UserLayout from '../../components/UserLayout.vue'
import { useUserStore } from '../../stores/user'
import { useCartStore } from '../../stores/cart'
import { favoriteAPI } from '../../api/modules/favorite'
import { FALLBACK_IMAGE } from '../../constants/images'
import { money } from '../../utils/format'

const router = useRouter()
const userStore = useUserStore()
const cartStore = useCartStore()
const favorites = ref([])
const loading = ref(false)
const fallbackImage = FALLBACK_IMAGE

onMounted(async () => {
  if (!userStore.isAuthenticated) { router.push('/login'); return }
  await loadFavorites()
})

const loadFavorites = async () => {
  loading.value = true
  try {
    const res = await favoriteAPI.getList()
    favorites.value = res.data.data || []
  } catch {
    ElMessage.error('获取收藏列表失败')
  } finally {
    loading.value = false
  }
}

const goToProduct = (id) => router.push(`/product/${id}`)

const addToCart = async (productId) => {
  try {
    await cartStore.addToCart(productId)
    ElMessage.success('已加入购物车')
  } catch (e) {
    ElMessage.error(e.message || '添加失败')
  }
}

const removeFav = async (fav) => {
  try {
    await favoriteAPI.toggle(fav.productId)
    favorites.value = favorites.value.filter(f => f.id !== fav.id)
    ElMessage.success('已取消收藏')
  } catch {
    ElMessage.error('操作失败')
  }
}
</script>

<style scoped>
.shop-container { width: min(1200px, calc(100% - 32px)); margin: 0 auto; }
.favorites-page { padding: 28px 0; }
.page-head { margin-bottom: 20px; }
.page-head h1 { font-size: 28px; margin: 0 0 6px; }
.page-head p { color: var(--color-text-muted); margin: 0; }

.fav-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 14px;
}

.fav-card {
  background: var(--color-bg-card, #fff);
  border-radius: var(--radius-md);
  border: 1px solid var(--color-border-light);
  overflow: hidden;
}

.fav-card img {
  width: 100%;
  aspect-ratio: 1;
  object-fit: cover;
  cursor: pointer;
}

.fav-info {
  padding: 12px;
}

.fav-info a {
  display: block;
  color: var(--color-text-primary);
  text-decoration: none;
  font-weight: 600;
  line-height: 1.4;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.fav-info a:hover { color: var(--color-primary); }

.fav-info strong {
  display: block;
  margin-top: 8px;
  color: var(--color-primary);
  font-size: 18px;
}

.fav-actions {
  display: flex;
  gap: 8px;
  padding: 0 12px 12px;
  align-items: center;
}

.loading-wrap { background: #fff; border-radius: var(--radius-md); padding: 20px; }
.empty { background: #fff; border-radius: var(--radius-md); padding: 40px; }

@media (max-width: 980px) { .fav-grid { grid-template-columns: repeat(2, 1fr); } }
@media (max-width: 560px) { .fav-grid { grid-template-columns: 1fr; } }
</style>
