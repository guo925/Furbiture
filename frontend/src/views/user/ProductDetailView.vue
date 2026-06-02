<template>
  <UserLayout>
    <section class="shop-container detail-page" v-if="product">
      <div class="crumb">首页 / 商品详情 / {{ product.name }}</div>

      <div class="detail-card">
        <div class="gallery">
          <el-image :src="activeImage" :preview-src-list="imageList" fit="cover" class="main-image" />
          <div class="thumbs">
            <button
              v-for="image in imageList"
              :key="image"
              :class="{ active: activeImage === image }"
              type="button"
              @click="activeImage = image"
            >
              <img :src="image" :alt="product.name">
            </button>
          </div>
        </div>

        <div class="info">
          <h1>{{ product.name }}</h1>
          <p class="subtitle">{{ product.description || '精选品质家具，适合现代家庭空间。' }}</p>

          <div class="price-box">
            <span>促销价</span>
            <strong>¥{{ money(product.price) }}</strong>
            <em>已售 {{ product.sales || 0 }}</em>
          </div>

          <dl class="basic-info">
            <div>
              <dt>品牌</dt>
              <dd>{{ product.brand || '橙家优选' }}</dd>
            </div>
            <div>
              <dt>分类</dt>
              <dd>{{ product.categoryName || '家居家具' }}</dd>
            </div>
            <div>
              <dt>库存</dt>
              <dd>{{ product.stock || 0 }} 件</dd>
            </div>
          </dl>

          <div v-if="specs.length" class="specs">
            <div v-for="spec in specs" :key="spec.id || spec.specName" class="spec-row">
              <span>{{ spec.specName }}</span>
              <button type="button" class="active">{{ spec.specValue }}</button>
            </div>
          </div>

          <div class="quantity-row">
            <span>数量</span>
            <el-input-number v-model="quantity" :min="1" :max="Math.max(product.stock || 1, 1)" />
          </div>

          <div class="actions">
            <button class="buy" type="button" :disabled="!canBuy" @click="buyNow">立即购买</button>
            <button class="cart" type="button" :disabled="!canBuy" @click="addToCart">加入购物车</button>
          </div>
        </div>
      </div>

      <div class="detail-extra">
        <h2>商品详情</h2>
        <p>{{ product.description || '暂无更多详情，建议联系商家确认尺寸、材质和配送范围。' }}</p>
      </div>
    </section>

    <section v-else class="shop-container loading">
      <el-skeleton :rows="10" animated />
    </section>
  </UserLayout>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import UserLayout from '../../components/UserLayout.vue'
import { productAPI } from '../../api'
import { useUserStore } from '../../stores/user'
import { useCartStore } from '../../stores/cart'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()
const cartStore = useCartStore()

const product = ref(null)
const images = ref([])
const specs = ref([])
const activeImage = ref('')
const quantity = ref(1)

const imageList = computed(() => {
  const list = images.value.map(item => item.imageUrl).filter(Boolean)
  if (product.value?.mainImage) list.unshift(product.value.mainImage)
  return [...new Set(list)].length ? [...new Set(list)] : ['https://images.unsplash.com/photo-1555041469-a586c61ea9bc?auto=format&fit=crop&w=900&q=80']
})

const canBuy = computed(() => product.value && product.value.stock > 0)

onMounted(async () => {
  await loadProductDetail(route.params.id)
})

const loadProductDetail = async (id) => {
  try {
    const response = await productAPI.getDetail(id)
    const data = response.data.data || {}
    product.value = data.product
    images.value = data.images || []
    specs.value = data.specs || []
    activeImage.value = imageList.value[0]
  } catch (error) {
    console.error('获取商品详情失败:', error)
    ElMessage.error('获取商品详情失败')
    router.push('/products')
  }
}

const addToCart = async () => {
  if (!userStore.isAuthenticated) {
    router.push('/login')
    return
  }

  try {
    await cartStore.addToCart(product.value.id, quantity.value)
    ElMessage.success('已加入购物车')
  } catch (error) {
    ElMessage.error(error.message || '添加购物车失败')
  }
}

const buyNow = async () => {
  if (!userStore.isAuthenticated) {
    router.push('/login')
    return
  }

  try {
    await cartStore.addToCart(product.value.id, quantity.value)
    router.push('/cart')
  } catch (error) {
    ElMessage.error(error.message || '操作失败')
  }
}

const money = (value) => Number(value || 0).toFixed(2)
</script>

<style scoped>
.detail-page {
  padding-top: 18px;
}

.crumb {
  color: #999;
  font-size: 13px;
  margin-bottom: 12px;
}

.detail-card {
  display: grid;
  grid-template-columns: 460px minmax(0, 1fr);
  gap: 34px;
  padding: 24px;
  background: #fff;
  border-radius: 8px;
  border: 1px solid #eee;
}

.main-image {
  width: 100%;
  aspect-ratio: 1 / 1;
  border-radius: 8px;
  overflow: hidden;
  background: #f3f3f3;
}

.thumbs {
  display: grid;
  grid-template-columns: repeat(5, 1fr);
  gap: 10px;
  margin-top: 12px;
}

.thumbs button {
  aspect-ratio: 1 / 1;
  border: 2px solid transparent;
  border-radius: 6px;
  padding: 0;
  overflow: hidden;
  cursor: pointer;
  background: #f5f5f5;
}

.thumbs button.active {
  border-color: #ff5000;
}

.thumbs img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.info h1 {
  font-size: 26px;
  line-height: 1.35;
}

.subtitle {
  margin-top: 8px;
  color: #777;
  line-height: 1.7;
}

.price-box {
  display: flex;
  align-items: baseline;
  gap: 14px;
  margin: 18px 0;
  padding: 16px;
  border-radius: 8px;
  background: #fff3ed;
}

.price-box span {
  color: #888;
}

.price-box strong {
  color: #ff5000;
  font-size: 34px;
}

.price-box em {
  margin-left: auto;
  color: #999;
  font-style: normal;
}

.basic-info {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  border: 1px solid #eee;
  border-radius: 8px;
  overflow: hidden;
}

.basic-info div {
  padding: 12px;
  border-right: 1px solid #eee;
}

.basic-info div:last-child {
  border-right: 0;
}

.basic-info dt {
  color: #999;
  font-size: 12px;
}

.basic-info dd {
  margin-top: 6px;
  color: #333;
  font-weight: 700;
}

.specs {
  margin-top: 18px;
}

.spec-row,
.quantity-row {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-top: 12px;
}

.spec-row span,
.quantity-row span {
  width: 44px;
  color: #888;
}

.spec-row button {
  min-width: 96px;
  height: 34px;
  border: 1px solid #ff5000;
  border-radius: 4px;
  color: #ff5000;
  background: #fff;
}

.actions {
  display: flex;
  gap: 14px;
  margin-top: 28px;
}

.actions button {
  width: 168px;
  height: 46px;
  border-radius: 4px;
  font-size: 16px;
  font-weight: 700;
  cursor: pointer;
}

.actions button:disabled {
  opacity: 0.55;
  cursor: not-allowed;
}

.buy {
  border: 1px solid #ff5000;
  background: #fff3ed;
  color: #ff5000;
}

.cart {
  border: 1px solid #ff5000;
  background: #ff5000;
  color: #fff;
}

.detail-extra {
  margin-top: 18px;
  padding: 22px;
  background: #fff;
  border-radius: 8px;
  border: 1px solid #eee;
}

.detail-extra h2 {
  font-size: 22px;
  margin-bottom: 12px;
}

.detail-extra p {
  color: #666;
  line-height: 1.8;
}

.loading {
  padding-top: 30px;
}

@media (max-width: 900px) {
  .detail-card {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 560px) {
  .detail-card {
    padding: 14px;
  }

  .basic-info {
    grid-template-columns: 1fr;
  }

  .basic-info div {
    border-right: 0;
    border-bottom: 1px solid #eee;
  }

  .actions {
    flex-direction: column;
  }

  .actions button {
    width: 100%;
  }
}
</style>
