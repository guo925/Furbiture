<template>
  <UserLayout>
    <section class="shop-container checkout-page">
      <h1>确认订单</h1>

      <el-skeleton v-if="loading" :rows="10" animated />
      <div v-else-if="checkoutItems.length === 0" class="empty">
        <el-empty description="没有可结算的商品" />
        <router-link to="/cart" class="primary-link">返回购物车</router-link>
      </div>

      <div v-else class="checkout-grid">
        <div class="main-column">
          <section class="panel">
            <div class="panel-head">
              <h2>收货地址</h2>
              <router-link to="/address">管理地址</router-link>
            </div>
            <div v-if="addresses.length" class="address-list">
              <button
                v-for="address in addresses"
                :key="address.id"
                :class="{ active: selectedAddressId === address.id }"
                type="button"
                @click="selectedAddressId = address.id"
              >
                <strong>{{ address.name }} {{ address.phone }}</strong>
                <span>{{ address.province }}{{ address.city }}{{ address.district }}{{ address.detailAddress }}</span>
                <em v-if="address.isDefault">默认</em>
              </button>
            </div>
            <el-empty v-else description="还没有收货地址">
              <router-link to="/address" class="primary-link">添加地址</router-link>
            </el-empty>
          </section>

          <section class="panel">
            <div class="panel-head">
              <h2>商品清单</h2>
              <router-link to="/cart">返回修改</router-link>
            </div>
            <article v-for="item in checkoutItems" :key="item.id" class="order-item">
              <img :src="item.product?.mainImage || fallbackImage" :alt="item.product?.name || '商品图片'" loading="lazy">
              <div>
                <router-link :to="`/product/${item.productId}`">{{ item.product?.name || '商品已下架' }}</router-link>
                <span>数量 x {{ item.quantity }}</span>
              </div>
              <strong>¥{{ money((item.product?.price || 0) * (item.quantity || 0)) }}</strong>
            </article>
          </section>
        </div>

        <aside class="summary">
          <h2>订单金额</h2>
          <div class="line">
            <span>商品件数</span>
            <b>{{ checkoutItems.length }} 件</b>
          </div>
          <div class="line">
            <span>商品总价</span>
            <b>¥{{ money(totalPrice) }}</b>
          </div>
          <div class="line">
            <span>运费</span>
            <b>¥0.00</b>
          </div>
          <div class="total">
            <span>实付金额</span>
            <strong>¥{{ money(totalPrice) }}</strong>
          </div>
          <button type="button" :disabled="submitting || !selectedAddressId" @click="submitOrder">
            {{ submitting ? '提交中...' : '提交订单并支付' }}
          </button>
        </aside>
      </div>
    </section>
  </UserLayout>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import UserLayout from '../../components/UserLayout.vue'
import { addressAPI, orderAPI } from '../../api'
import { useUserStore } from '../../stores/user'
import { useCartStore } from '../../stores/cart'
import { FALLBACK_IMAGE } from '../../constants/images'
import { money } from '../../utils/format'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()
const cartStore = useCartStore()
const loading = ref(false)
const submitting = ref(false)
const addresses = ref([])
const selectedAddressId = ref(null)
const fallbackImage = FALLBACK_IMAGE

const selectedCartIds = computed(() => String(route.query.cartItemIds || '').split(',').filter(Boolean).map(Number))
const checkoutItems = computed(() => {
  if (!selectedCartIds.value.length) return cartStore.cartItems
  return cartStore.cartItems.filter(item => selectedCartIds.value.includes(Number(item.id)))
})
const totalPrice = computed(() => checkoutItems.value.reduce((sum, item) => sum + Number(item.product?.price || 0) * Number(item.quantity || 0), 0))

onMounted(async () => {
  if (!userStore.isAuthenticated) {
    router.push('/login')
    return
  }
  await loadData()
})

const loadData = async () => {
  loading.value = true
  try {
    await cartStore.getCartList()
    const addressResponse = await addressAPI.getList()
    addresses.value = (addressResponse.data.data || []).map(addr => ({
      ...addr,
      isDefault: addr.isDefault === 1 || addr.isDefault === true
    }))
    const defaultAddress = addresses.value.find(addr => addr.isDefault)
    selectedAddressId.value = defaultAddress?.id || addresses.value[0]?.id || null
  } catch (error) {
    console.error('加载结算数据失败:', error?.message)
    ElMessage.error('加载结算数据失败')
  } finally {
    loading.value = false
  }
}

const submitOrder = async () => {
  if (!selectedAddressId.value) {
    ElMessage.warning('请选择收货地址')
    return
  }
  if (!checkoutItems.value.length) {
    ElMessage.warning('没有可结算的商品')
    return
  }

  submitting.value = true
  try {
    const cartItemIds = checkoutItems.value.map(item => item.id)
    const response = await orderAPI.create(selectedAddressId.value, cartItemIds)
    await orderAPI.pay(response.data.data.orderNo)
    await cartStore.getCartList()
    ElMessage.success('支付成功，订单已创建')
    router.push('/orders')
  } catch (error) {
    console.error('提交订单失败:', error?.message)
    ElMessage.error(error.response?.data?.message || error.message || '提交订单失败')
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped>
.checkout-page {
  padding-top: 22px;
}

h1 {
  font-size: 28px;
  margin-bottom: 16px;
}

.empty {
  background: #fff;
  border-radius: 8px;
  padding: 30px;
}

.primary-link,
.panel-head a {
  color: #ff5000;
  text-decoration: none;
  font-weight: 700;
}

.checkout-grid {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 320px;
  gap: 18px;
  align-items: start;
}

.main-column {
  display: grid;
  gap: 18px;
}

.panel,
.summary {
  background: #fff;
  border: 1px solid #eee;
  border-radius: 8px;
  padding: 18px;
}

.panel-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 14px;
}

.panel h2,
.summary h2 {
  font-size: 20px;
}

.address-list {
  display: grid;
  gap: 10px;
}

.address-list button {
  position: relative;
  text-align: left;
  border: 1px solid #e5e5e5;
  border-radius: 6px;
  background: #fff;
  padding: 14px;
  cursor: pointer;
}

.address-list button.active {
  border-color: #ff5000;
  background: #fff8f4;
}

.address-list strong,
.address-list span {
  display: block;
}

.address-list span {
  margin-top: 8px;
  color: #666;
  line-height: 1.6;
}

.address-list em {
  position: absolute;
  top: 12px;
  right: 12px;
  color: #ff5000;
  font-style: normal;
  font-size: 12px;
}

.order-item {
  display: grid;
  grid-template-columns: 76px minmax(0, 1fr) 120px;
  align-items: center;
  gap: 14px;
  padding: 12px 0;
  border-top: 1px solid #f1f1f1;
}

.order-item img {
  width: 76px;
  height: 76px;
  object-fit: cover;
  border-radius: 6px;
}

.order-item a,
.order-item span {
  display: block;
}

.order-item a {
  color: #222;
  text-decoration: none;
  font-weight: 700;
}

.order-item span {
  margin-top: 8px;
  color: #999;
}

.order-item strong {
  color: #ff5000;
  text-align: right;
}

.summary {
  position: sticky;
  top: 158px;
}

.line,
.total {
  display: flex;
  justify-content: space-between;
  margin-top: 16px;
  color: #666;
}

.line b {
  color: #333;
}

.total {
  align-items: baseline;
  padding-top: 16px;
  border-top: 1px solid #eee;
}

.total strong {
  color: #ff5000;
  font-size: 28px;
}

.summary button {
  width: 100%;
  height: 44px;
  margin-top: 22px;
  border: 0;
  border-radius: 4px;
  background: #ff5000;
  color: #fff;
  font-size: 16px;
  font-weight: 700;
  cursor: pointer;
}

.summary button:disabled {
  background: #bbb;
  cursor: not-allowed;
}

@media (max-width: 900px) {
  .checkout-grid {
    grid-template-columns: 1fr;
  }

  .summary {
    position: static;
  }
}

@media (max-width: 560px) {
  .order-item {
    grid-template-columns: 64px 1fr;
  }

  .order-item strong {
    grid-column: 2;
    text-align: left;
  }
}
</style>
