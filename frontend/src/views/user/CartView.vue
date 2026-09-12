<template>
  <UserLayout>
    <section class="shop-container cart-page">
      <div class="page-head">
        <div>
          <h1>购物车</h1>
          <p>已选 {{ selectedItems.length }} 件，合计 ¥{{ money(selectedTotal) }}</p>
        </div>
        <router-link to="/products">继续购物</router-link>
      </div>

      <el-skeleton v-if="cartStore.loading" :rows="8" animated />
      <div v-else-if="cartStore.cartItems.length === 0" class="empty">
        <el-empty description="购物车是空的" />
        <router-link to="/products" class="primary-link">去挑选商品</router-link>
      </div>

      <div v-else class="cart-wrap">
        <div class="cart-toolbar">
          <el-checkbox :model-value="allChecked" :indeterminate="isIndeterminate" @change="toggleAll">全选</el-checkbox>
          <button type="button" @click="clearSelected" :disabled="selectedIds.length === 0 || clearing">删除选中</button>
          <button type="button" @click="clearCart" :disabled="clearing">清空购物车</button>
        </div>

        <div class="cart-list">
          <article v-for="item in cartStore.cartItems" :key="item.id" class="cart-item">
            <el-checkbox :model-value="selectedIds.includes(item.id)" @change="checked => toggleItem(item.id, checked)" />
            <img :src="item.product?.mainImage || fallbackImage" :alt="item.product?.name || '商品图片'">
            <div class="item-info">
              <router-link :to="`/product/${item.productId}`">{{ item.product?.name || '商品已下架' }}</router-link>
              <span>{{ item.product?.brand || item.product?.categoryName || '家具商品' }}</span>
            </div>
            <strong>¥{{ money(item.product?.price) }}</strong>
            <el-input-number v-model="item.quantity" :min="1" :max="99" @change="value => updateQuantity(item.id, value)" />
            <strong class="subtotal">¥{{ money((item.product?.price || 0) * (item.quantity || 0)) }}</strong>
            <el-button :icon="Delete" text type="danger" :loading="removingIds.includes(item.id)" @click="removeItem(item.id)">删除</el-button>
          </article>
        </div>

        <div class="settlement">
          <span>已选 <b>{{ selectedItems.length }}</b> 件</span>
          <span>合计 <strong>¥{{ money(selectedTotal) }}</strong></span>
          <button type="button" :disabled="selectedIds.length === 0" @click="checkout">去结算</button>
        </div>
      </div>
    </section>
  </UserLayout>
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { Delete } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import UserLayout from '../../components/UserLayout.vue'
import { useUserStore } from '../../stores/user'
import { useCartStore } from '../../stores/cart'

const router = useRouter()
const userStore = useUserStore()
const cartStore = useCartStore()
const selectedIds = ref([])
/** 单条删除的 loading 标记：只让被操作的那一行转圈，不阻塞整页 */
const removingIds = ref([])
/** 批量操作（删除选中 / 清空购物车）进行中标记，用于禁用按钮防重复点击 */
const clearing = ref(false)
const fallbackImage = 'https://images.unsplash.com/photo-1555041469-a586c61ea9bc?auto=format&fit=crop&w=300&q=80'

const selectedItems = computed(() => cartStore.cartItems.filter(item => selectedIds.value.includes(item.id)))
const selectedTotal = computed(() => selectedItems.value.reduce((sum, item) => sum + Number(item.product?.price || 0) * Number(item.quantity || 0), 0))
const allChecked = computed(() => cartStore.cartItems.length > 0 && selectedIds.value.length === cartStore.cartItems.length)
const isIndeterminate = computed(() => selectedIds.value.length > 0 && selectedIds.value.length < cartStore.cartItems.length)

onMounted(async () => {
  if (!userStore.isAuthenticated) {
    router.push('/login')
    return
  }
  await cartStore.getCartList()
  selectedIds.value = cartStore.cartItems.map(item => item.id)
})

watch(() => cartStore.cartItems.map(item => item.id), ids => {
  selectedIds.value = selectedIds.value.filter(id => ids.includes(id))
})

const toggleAll = (checked) => {
  selectedIds.value = checked ? cartStore.cartItems.map(item => item.id) : []
}

const toggleItem = (id, checked) => {
  selectedIds.value = checked ? [...new Set([...selectedIds.value, id])] : selectedIds.value.filter(itemId => itemId !== id)
}

const updateQuantity = async (id, quantity) => {
  try {
    await cartStore.updateCartItem(id, quantity)
    // 不再弹成功提示：el-input-number 每次加减都会触发，弹提示会立刻刷屏
  } catch (error) {
    ElMessage.error(error.message || '更新失败')
    // 接口失败时本地 quantity 已被 v-model 改过，重新拉取以服务端为准
    await cartStore.getCartList()
  }
}

const removeItem = async (id) => {
  try {
    await ElMessageBox.confirm('确定从购物车移除这件商品吗？', '移除商品', {
      type: 'warning',
      confirmButtonText: '移除',
      cancelButtonText: '取消'
    })
  } catch {
    return // 用户点了取消
  }

  removingIds.value = [...removingIds.value, id]
  try {
    await cartStore.removeCartItem(id)
    selectedIds.value = selectedIds.value.filter(itemId => itemId !== id)
    ElMessage.success('已移除')
  } catch (error) {
    ElMessage.error(error.message || '删除失败')
  } finally {
    removingIds.value = removingIds.value.filter(itemId => itemId !== id)
  }
}

const clearSelected = async () => {
  const count = selectedIds.value.length
  if (count === 0) return

  try {
    await ElMessageBox.confirm(
      `将移除选中的 ${count} 件商品，移除后需重新加入，确定继续吗？`,
      '删除选中商品',
      { type: 'warning', confirmButtonText: '删除', cancelButtonText: '取消' }
    )
  } catch {
    return
  }

  clearing.value = true
  try {
    // 逐个删除是既有实现（后端未提供批量删除接口），这里补上整体失败提示，
    // 避免中途失败时用户以为全部删完了
    for (const id of [...selectedIds.value]) {
      await cartStore.removeCartItem(id)
    }
    selectedIds.value = []
    ElMessage.success('已删除选中商品')
  } catch (error) {
    ElMessage.error(error.message || '部分商品删除失败，请刷新后重试')
    await cartStore.getCartList()
  } finally {
    clearing.value = false
  }
}

const clearCart = async () => {
  try {
    await ElMessageBox.confirm(
      '将清空购物车中的全部商品，此操作不可恢复，确定继续吗？',
      '清空购物车',
      { type: 'warning', confirmButtonText: '清空', cancelButtonText: '取消' }
    )
  } catch {
    return
  }

  clearing.value = true
  try {
    await cartStore.clearCart()
    selectedIds.value = []
    ElMessage.success('购物车已清空')
  } catch (error) {
    ElMessage.error(error.message || '清空失败')
  } finally {
    clearing.value = false
  }
}

const checkout = () => {
  router.push({ path: '/checkout', query: { cartItemIds: selectedIds.value.join(',') } })
}

const money = (value) => Number(value || 0).toFixed(2)
</script>

<style scoped>
.cart-page {
  padding-top: 22px;
}

.page-head {
  display: flex;
  justify-content: space-between;
  align-items: end;
  margin-bottom: 16px;
}

.page-head h1 {
  font-size: 28px;
}

.page-head p {
  margin-top: 6px;
  color: #888;
}

.page-head a,
.primary-link {
  color: #ff5000;
  text-decoration: none;
  font-weight: 700;
}

.empty {
  background: #fff;
  border-radius: 8px;
  padding: 30px;
}

.cart-wrap {
  background: #fff;
  border-radius: 8px;
  border: 1px solid #eee;
  overflow: hidden;
}

.cart-toolbar {
  display: flex;
  align-items: center;
  gap: 18px;
  height: 48px;
  padding: 0 18px;
  border-bottom: 1px solid #eee;
}

.cart-toolbar button {
  border: 0;
  background: transparent;
  color: #666;
  cursor: pointer;
}

.cart-toolbar button:disabled {
  color: #bbb;
  cursor: not-allowed;
}

.cart-list {
  display: grid;
}

.cart-item {
  display: grid;
  grid-template-columns: 34px 92px minmax(180px, 1fr) 110px 150px 120px 70px;
  align-items: center;
  gap: 14px;
  padding: 16px 18px;
  border-bottom: 1px solid #f0f0f0;
}

.cart-item img {
  width: 92px;
  height: 92px;
  object-fit: cover;
  border-radius: 6px;
  background: #f3f3f3;
}

.item-info a,
.item-info span {
  display: block;
}

.item-info a {
  color: #222;
  text-decoration: none;
  font-weight: 700;
  line-height: 1.5;
}

.item-info a:hover {
  color: #ff5000;
}

.item-info span {
  margin-top: 8px;
  color: #999;
  font-size: 13px;
}

.subtotal {
  color: #ff5000;
}

.settlement {
  position: sticky;
  bottom: 0;
  height: 62px;
  display: flex;
  justify-content: flex-end;
  align-items: center;
  gap: 24px;
  padding-left: 18px;
  background: #fff;
  box-shadow: 0 -4px 14px rgba(0, 0, 0, 0.06);
}

.settlement b,
.settlement strong {
  color: #ff5000;
}

.settlement strong {
  font-size: 24px;
}

.settlement button {
  align-self: stretch;
  width: 150px;
  border: 0;
  background: #ff5000;
  color: #fff;
  font-size: 18px;
  font-weight: 700;
  cursor: pointer;
}

.settlement button:disabled {
  background: #bbb;
  cursor: not-allowed;
}

@media (max-width: 920px) {
  .cart-item {
    grid-template-columns: 34px 82px 1fr;
  }

  .cart-item > strong,
  .cart-item :deep(.el-input-number),
  .cart-item :deep(.el-button) {
    grid-column: 3;
  }
}

@media (max-width: 560px) {
  .page-head {
    align-items: flex-start;
    flex-direction: column;
    gap: 8px;
  }

  .settlement {
    gap: 10px;
    font-size: 13px;
  }

  .settlement button {
    width: 104px;
    font-size: 16px;
  }
}
</style>
