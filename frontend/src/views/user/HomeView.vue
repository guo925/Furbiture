<template>
  <UserLayout>
    <section class="home-hero">
      <div class="shop-container hero-grid">
        <aside class="category-panel">
          <h2>主题市场</h2>
          <button v-for="category in categories" :key="category.id" type="button" @click="goToCategory(category.id)">
            <span>{{ category.name }}</span>
            <el-icon><ArrowRight /></el-icon>
          </button>
        </aside>

        <div class="hero-stage" :style="heroStyle">
          <div class="hero-copy">
            <span>家装焕新季</span>
            <h1>把理想客厅搬回家</h1>
            <p>精选沙发、餐桌、床具与收纳家具，按户型和风格快速选购。</p>
            <button type="button" @click="router.push('/products')">立即逛逛</button>
          </div>
        </div>

        <aside class="service-panel">
          <h3>省心服务</h3>
          <div v-for="item in services" :key="item.title" class="service-item">
            <el-icon><component :is="item.icon" /></el-icon>
            <div>
              <strong>{{ item.title }}</strong>
              <span>{{ item.text }}</span>
            </div>
          </div>
        </aside>
      </div>
    </section>

    <section class="shop-container channel-section">
      <div class="channel" v-for="channel in channels" :key="channel.title" @click="router.push(channel.to)">
        <span>{{ channel.kicker }}</span>
        <strong>{{ channel.title }}</strong>
        <small>{{ channel.text }}</small>
      </div>
    </section>

    <section class="shop-container product-section">
      <div class="section-head">
        <div>
          <span>猜你喜欢</span>
          <h2>热门家具</h2>
        </div>
        <router-link to="/products">查看全部</router-link>
      </div>

      <el-skeleton v-if="loading" :rows="8" animated />
      <div v-else-if="hotProducts.length" class="product-grid">
        <ProductCard
          v-for="product in hotProducts"
          :key="product.id"
          :product="product"
          @open="goToProduct"
          @cart="addToCart"
        />
      </div>
      <el-empty v-else description="暂无商品" />
    </section>
  </UserLayout>
</template>

<script setup>
import { computed, markRaw, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ArrowRight, Box, Service, Van } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import UserLayout from '../../components/UserLayout.vue'
import ProductCard from '../../components/ProductCard.vue'
import { categoryAPI, productAPI } from '../../api'
import { useUserStore } from '../../stores/user'
import { useCartStore } from '../../stores/cart'

const router = useRouter()
const userStore = useUserStore()
const cartStore = useCartStore()
const categories = ref([])
const hotProducts = ref([])
const loading = ref(false)

const services = [
  { icon: markRaw(Van), title: '大件配送', text: '覆盖主流城市' },
  { icon: markRaw(Service), title: '售后无忧', text: '订单全程可追踪' },
  { icon: markRaw(Box), title: '正品好货', text: '商家后台统一管理' }
]

const channels = [
  { kicker: '限时精选', title: '客厅中心', text: '沙发 茶几 电视柜', to: '/products?keyword=沙发' },
  { kicker: '空间焕新', title: '卧室好眠', text: '床 垫 床头柜', to: '/products?keyword=床' },
  { kicker: '小户型', title: '收纳升级', text: '柜类 置物架 餐边柜', to: '/products?keyword=收纳' }
]

const heroStyle = computed(() => {
  const image = hotProducts.value[0]?.mainImage
  return image ? { backgroundImage: `linear-gradient(90deg, rgba(0,0,0,.62), rgba(0,0,0,.16)), url("${image}")` } : {}
})

onMounted(async () => {
  await Promise.all([loadCategories(), loadHotProducts()])
})

const loadCategories = async () => {
  try {
    // 分类列表由 categoryAPI 提供（GET /api/categories），返回扁平列表且含 status 字段。
    // 此前误用了 productAPI.getCategories（该方法不存在），导致首页「主题市场」永久空白。
    const response = await categoryAPI.getList()
    categories.value = (response.data.data || []).filter(item => item.status !== 0).slice(0, 9)
  } catch (error) {
    console.error('获取分类失败:', error?.message)
  }
}

const loadHotProducts = async () => {
  loading.value = true
  try {
    const response = await productAPI.getList({ page: 1, size: 12, sortBy: 'sales_desc' })
    hotProducts.value = response.data.data?.records || []
  } catch (error) {
    console.error('获取商品失败:', error?.message)
  } finally {
    loading.value = false
  }
}

const goToCategory = (categoryId) => {
  router.push({ path: '/products', query: { categoryId } })
}

const goToProduct = (productId) => {
  router.push(`/product/${productId}`)
}

const addToCart = async (productId) => {
  if (!userStore.isAuthenticated) {
    router.push('/login')
    return
  }

  try {
    await cartStore.addToCart(productId)
    ElMessage.success('已加入购物车')
  } catch (error) {
    ElMessage.error(error.message || '添加购物车失败')
  }
}
</script>

<style scoped>
.home-hero {
  padding: 18px 0 0;
}

.hero-grid {
  display: grid;
  grid-template-columns: 220px minmax(0, 1fr) 220px;
  gap: 14px;
}

.category-panel,
.service-panel,
.hero-stage,
.channel {
  border-radius: 8px;
}

.category-panel,
.service-panel {
  background: #fff;
  padding: 16px;
}

.category-panel h2,
.service-panel h3 {
  font-size: 18px;
  margin-bottom: 12px;
}

.category-panel button {
  width: 100%;
  height: 36px;
  border: 0;
  background: transparent;
  display: flex;
  align-items: center;
  justify-content: space-between;
  color: #333;
  cursor: pointer;
  border-radius: 4px;
  padding: 0 8px;
}

.category-panel button:hover {
  background: #fff3ed;
  color: #ff5000;
}

.hero-stage {
  min-height: 330px;
  background: linear-gradient(135deg, #654321, #f36b21);
  background-size: cover;
  background-position: center;
  display: flex;
  align-items: center;
  padding: 42px;
  color: #fff;
}

.hero-copy {
  max-width: 430px;
}

.hero-copy span {
  display: inline-flex;
  padding: 4px 10px;
  border-radius: 4px;
  background: rgba(255, 255, 255, 0.18);
  font-size: 13px;
}

.hero-copy h1 {
  margin: 16px 0 10px;
  font-size: 42px;
  letter-spacing: 0;
}

.hero-copy p {
  color: rgba(255, 255, 255, 0.9);
  line-height: 1.7;
}

.hero-copy button {
  margin-top: 24px;
  width: 128px;
  height: 40px;
  border: 0;
  border-radius: 4px;
  background: #ff5000;
  color: #fff;
  font-weight: 700;
  cursor: pointer;
}

.service-item {
  display: flex;
  gap: 10px;
  padding: 13px 0;
  border-bottom: 1px solid #f0f0f0;
}

.service-item:last-child {
  border-bottom: 0;
}

.service-item .el-icon {
  color: #ff5000;
  font-size: 22px;
  margin-top: 2px;
}

.service-item strong,
.service-item span {
  display: block;
}

.service-item span {
  margin-top: 4px;
  color: #888;
  font-size: 13px;
}

.channel-section {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 14px;
  margin-top: 16px;
}

.channel {
  min-height: 112px;
  background: #fff;
  padding: 18px;
  cursor: pointer;
  border: 1px solid transparent;
}

.channel:hover {
  border-color: #ff5000;
}

.channel span,
.section-head span {
  color: #ff5000;
  font-size: 13px;
  font-weight: 700;
}

.channel strong {
  display: block;
  margin: 8px 0;
  font-size: 22px;
}

.channel small {
  color: #888;
}

.product-section {
  margin-top: 28px;
}

.section-head {
  display: flex;
  align-items: end;
  justify-content: space-between;
  margin-bottom: 14px;
}

.section-head h2 {
  font-size: 28px;
}

.section-head a {
  color: #ff5000;
  text-decoration: none;
  font-weight: 700;
}

.product-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 14px;
}

@media (max-width: 980px) {
  .hero-grid {
    grid-template-columns: 1fr;
  }

  .category-panel {
    display: grid;
    grid-template-columns: repeat(3, 1fr);
    gap: 8px;
  }

  .category-panel h2 {
    grid-column: 1 / -1;
  }

  .product-grid,
  .channel-section {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 560px) {
  .hero-stage {
    padding: 28px;
  }

  .hero-copy h1 {
    font-size: 30px;
  }

  .product-grid,
  .channel-section,
  .category-panel {
    grid-template-columns: 1fr;
  }
}
</style>
