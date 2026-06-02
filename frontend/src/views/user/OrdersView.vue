<template>
  <UserLayout>
    <section class="shop-container orders-page">
      <div class="page-head">
        <div>
          <h1>我的订单</h1>
          <p>按状态、商品或订单号快速定位订单</p>
        </div>
        <router-link to="/products">继续购物</router-link>
      </div>

      <div class="order-toolbar">
        <el-tabs v-model="activeStatus" @tab-change="handleStatusChange">
          <el-tab-pane v-for="tab in statusTabs" :key="tab.value" :name="tab.value">
            <template #label>
              <span>{{ tab.label }}</span>
              <em v-if="orderCount(tab.value)">{{ orderCount(tab.value) }}</em>
            </template>
          </el-tab-pane>
        </el-tabs>

        <div class="filters">
          <el-input
            v-model="keyword"
            clearable
            placeholder="商品名称 / 订单号"
            @keyup.enter="applyFilters"
            @clear="applyFilters"
          />
          <el-date-picker
            v-model="dateRange"
            type="daterange"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            value-format="YYYY-MM-DD"
            @change="applyFilters"
          />
          <el-button type="primary" @click="applyFilters">搜索</el-button>
          <el-button @click="resetFilters">重置</el-button>
        </div>
      </div>

      <el-skeleton v-if="loading" :rows="8" animated />

      <div v-else-if="filteredOrders.length === 0" class="empty">
        <el-empty description="没有找到相关订单" />
        <router-link to="/products" class="primary-link">去挑选商品</router-link>
      </div>

      <div v-else class="order-list">
        <article v-for="order in filteredOrders" :key="order.id" class="order-card">
          <header class="order-card-head">
            <div>
              <span>{{ formatDate(order.createTime) }}</span>
              <button type="button" @click="copyText(order.orderNo)">订单号 {{ order.orderNo }}</button>
            </div>
            <el-tag :type="statusMeta(order.status).type" effect="light">{{ statusMeta(order.status).label }}</el-tag>
          </header>

          <div class="order-card-body">
            <div class="goods-list">
              <router-link
                v-for="item in order.items"
                :key="item.id"
                class="goods-row"
                :to="`/product/${item.productId}`"
              >
                <img :src="item.productImage || fallbackImage" :alt="item.productName">
                <div>
                  <strong>{{ item.productName }}</strong>
                  <span>单价 ¥{{ money(item.price) }} x {{ item.quantity }}</span>
                </div>
                <b>¥{{ money(itemSubtotal(item)) }}</b>
              </router-link>
            </div>

            <aside class="order-summary">
              <span>共 {{ order.itemCount }} 件</span>
              <strong>实付 ¥{{ money(order.totalAmount) }}</strong>
              <small>{{ timelineText(order) }}</small>
            </aside>
          </div>

          <footer class="order-card-foot">
            <div class="service-links">
              <button type="button" @click="copyText(order.orderNo)">复制订单号</button>
              <button type="button" @click="buyAgain(order)">再次购买</button>
              <button v-if="order.status === 3" type="button" @click="reviewOrder(order)">评价</button>
              <button v-if="[1, 2, 3].includes(order.status)" type="button" @click="afterSale(order)">申请售后</button>
            </div>
            <div class="primary-actions">
              <el-button v-if="order.status === 0" @click="cancelOrder(order)">取消订单</el-button>
              <el-button v-if="order.status === 0" type="primary" @click="payOrder(order)">去支付</el-button>
              <el-button v-if="order.status === 2" type="primary" @click="confirmReceipt(order)">确认收货</el-button>
              <el-button @click="viewOrderDetail(order)">查看详情</el-button>
            </div>
          </footer>
        </article>
      </div>
    </section>
  </UserLayout>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import UserLayout from '../../components/UserLayout.vue'
import { cartAPI, orderAPI } from '../../api'
import { useCartStore } from '../../stores/cart'
import { useUserStore } from '../../stores/user'

const router = useRouter()
const userStore = useUserStore()
const cartStore = useCartStore()

const loading = ref(false)
const orders = ref([])
const activeStatus = ref('all')
const keyword = ref('')
const appliedKeyword = ref('')
const dateRange = ref([])
const appliedDateRange = ref([])
const fallbackImage = 'https://images.unsplash.com/photo-1555041469-a586c61ea9bc?auto=format&fit=crop&w=300&q=80'

const statusTabs = [
  { label: '全部', value: 'all' },
  { label: '待付款', value: '0' },
  { label: '待发货', value: '1' },
  { label: '待收货', value: '2' },
  { label: '待评价', value: '3' },
  { label: '退款/售后', value: 'service' }
]

onMounted(async () => {
  if (!userStore.isAuthenticated) {
    router.push('/login')
    return
  }
  await loadOrders()
  await cartStore.getCartList()
})

const filteredOrders = computed(() => {
  return orders.value.filter(order => {
    const matchesStatus = activeStatus.value === 'all'
      || (activeStatus.value === 'service' ? [4, 5].includes(order.status) : order.status === Number(activeStatus.value))
    const text = `${order.orderNo} ${order.items.map(item => item.productName).join(' ')}`.toLowerCase()
    const matchesKeyword = !appliedKeyword.value || text.includes(appliedKeyword.value.toLowerCase())
    const matchesDate = !appliedDateRange.value?.length || inDateRange(order.createTime, appliedDateRange.value)
    return matchesStatus && matchesKeyword && matchesDate
  })
})

const loadOrders = async () => {
  try {
    loading.value = true
    const response = await orderAPI.getList()
    orders.value = (response.data.data || []).map(normalizeOrder)
  } catch (error) {
    ElMessage.error(error.response?.data?.msg || '获取订单失败')
  } finally {
    loading.value = false
  }
}

const normalizeOrder = (order) => {
  const items = (order.orderItems || []).map(item => ({
    ...item,
    totalPrice: item.totalPrice || item.price * item.quantity
  }))
  return {
    id: order.id,
    orderNo: order.orderNo,
    status: Number(order.status),
    totalAmount: Number(order.totalAmount || 0),
    createTime: order.createTime,
    payTime: order.payTime,
    deliveryTime: order.deliveryTime,
    finishTime: order.finishTime,
    cancelTime: order.cancelTime,
    items,
    itemCount: items.reduce((sum, item) => sum + Number(item.quantity || 0), 0)
  }
}

const statusMeta = status => ({
  0: { label: '待付款', type: 'warning' },
  1: { label: '待发货', type: 'primary' },
  2: { label: '待收货', type: 'success' },
  3: { label: '交易成功', type: 'info' },
  4: { label: '已取消', type: 'danger' },
  5: { label: '已退款', type: 'danger' }
}[status] || { label: '未知状态', type: 'info' })

const orderCount = value => {
  if (value === 'all') return orders.value.length
  if (value === 'service') return orders.value.filter(order => [4, 5].includes(order.status)).length
  return orders.value.filter(order => order.status === Number(value)).length
}

const applyFilters = () => {
  appliedKeyword.value = keyword.value.trim()
  appliedDateRange.value = dateRange.value || []
}

const resetFilters = () => {
  keyword.value = ''
  appliedKeyword.value = ''
  dateRange.value = []
  appliedDateRange.value = []
  activeStatus.value = 'all'
}

const handleStatusChange = () => {
  applyFilters()
}

const payOrder = async order => {
  try {
    await orderAPI.pay(order.orderNo)
    ElMessage.success('支付成功')
    await loadOrders()
  } catch (error) {
    ElMessage.error(error.response?.data?.msg || '支付失败')
  }
}

const cancelOrder = async order => {
  try {
    await ElMessageBox.confirm('确定取消该订单吗？', '取消订单', { type: 'warning' })
    await orderAPI.cancel(order.orderNo)
    ElMessage.success('订单已取消')
    await loadOrders()
  } catch (error) {
    if (error !== 'cancel') ElMessage.error(error.response?.data?.msg || '取消订单失败')
  }
}

const confirmReceipt = async order => {
  try {
    await ElMessageBox.confirm('确认已经收到商品？', '确认收货', { type: 'warning' })
    await orderAPI.confirmReceipt(order.orderNo)
    ElMessage.success('确认收货成功')
    await loadOrders()
  } catch (error) {
    if (error !== 'cancel') ElMessage.error(error.response?.data?.msg || '确认收货失败')
  }
}

const buyAgain = async order => {
  try {
    for (const item of order.items) {
      await cartAPI.add({ productId: item.productId, quantity: item.quantity })
    }
    await cartStore.getCartList()
    ElMessage.success('已加入购物车')
    router.push('/cart')
  } catch (error) {
    ElMessage.error(error.response?.data?.msg || '加入购物车失败')
  }
}

const reviewOrder = () => {
  ElMessage.info('评价功能暂未开放')
}

const afterSale = () => {
  ElMessage.info('售后申请功能暂未开放')
}

const viewOrderDetail = order => {
  router.push(`/order/${order.orderNo}`)
}

const copyText = async text => {
  await copyToClipboard(text)
  ElMessage.success('已复制')
}

const itemSubtotal = item => Number(item.totalPrice || Number(item.price || 0) * Number(item.quantity || 0))
const money = value => Number(value || 0).toFixed(2)
const formatDate = value => value ? String(value).replace('T', ' ').slice(0, 19) : '-'
const inDateRange = (value, range) => {
  const date = String(value || '').slice(0, 10)
  return date >= range[0] && date <= range[1]
}
const copyToClipboard = async text => {
  if (navigator.clipboard?.writeText) {
    await navigator.clipboard.writeText(text)
    return
  }
  const input = document.createElement('textarea')
  input.value = text
  document.body.appendChild(input)
  input.select()
  document.execCommand('copy')
  document.body.removeChild(input)
}
const timelineText = order => {
  if (order.status === 0) return '等待买家付款'
  if (order.status === 1) return `付款时间 ${formatDate(order.payTime)}`
  if (order.status === 2) return `发货时间 ${formatDate(order.deliveryTime)}`
  if (order.status === 3) return `完成时间 ${formatDate(order.finishTime)}`
  if (order.status === 4) return `取消时间 ${formatDate(order.cancelTime)}`
  return '售后处理中'
}
</script>

<style scoped>
.orders-page {
  padding: 28px 20px 48px;
}

.page-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 18px;
}

.page-head h1 {
  margin: 0 0 6px;
  font-size: 24px;
}

.page-head p {
  margin: 0;
  color: #666;
}

.page-head a,
.primary-link {
  color: #409eff;
  text-decoration: none;
}

.order-toolbar {
  background: #fff;
  border: 1px solid #ebeef5;
  border-radius: 8px;
  padding: 0 18px 16px;
  margin-bottom: 16px;
}

.order-toolbar em {
  margin-left: 5px;
  font-style: normal;
  color: #f56c6c;
}

.filters {
  display: grid;
  grid-template-columns: minmax(220px, 1fr) 270px auto auto;
  gap: 10px;
}

.empty {
  padding: 60px 0;
  background: #fff;
  border-radius: 8px;
  text-align: center;
}

.order-list {
  display: grid;
  gap: 14px;
}

.order-card {
  background: #fff;
  border: 1px solid #ebeef5;
  border-radius: 8px;
  overflow: hidden;
}

.order-card-head,
.order-card-foot {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 12px 16px;
  background: #fafafa;
}

.order-card-head div,
.service-links,
.primary-actions {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.order-card button {
  border: 0;
  background: none;
  color: #606266;
  cursor: pointer;
}

.order-card button:hover {
  color: #409eff;
}

.order-card-body {
  display: grid;
  grid-template-columns: 1fr 180px;
}

.goods-list {
  border-right: 1px solid #ebeef5;
}

.goods-row {
  display: grid;
  grid-template-columns: 76px 1fr auto;
  gap: 12px;
  align-items: center;
  padding: 14px 16px;
  color: inherit;
  text-decoration: none;
  border-bottom: 1px solid #f3f4f6;
}

.goods-row:last-child {
  border-bottom: 0;
}

.goods-row img {
  width: 76px;
  height: 76px;
  object-fit: cover;
  border-radius: 6px;
}

.goods-row strong {
  display: block;
  margin-bottom: 8px;
  color: #303133;
}

.goods-row span,
.order-summary span,
.order-summary small {
  color: #909399;
}

.order-summary {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  justify-content: center;
  gap: 8px;
  padding: 16px;
}

.order-summary strong {
  font-size: 18px;
  color: #f56c6c;
}

@media (max-width: 760px) {
  .page-head,
  .order-card-head,
  .order-card-foot {
    align-items: flex-start;
    flex-direction: column;
  }

  .filters,
  .order-card-body {
    grid-template-columns: 1fr;
  }

  .goods-list {
    border-right: 0;
  }

  .order-summary {
    align-items: flex-start;
    border-top: 1px solid #ebeef5;
  }
}
</style>
