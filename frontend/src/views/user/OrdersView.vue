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

      <div v-else-if="orders.length === 0" class="empty">
        <el-empty description="没有找到相关订单" />
        <router-link to="/products" class="primary-link">去挑选商品</router-link>
      </div>

      <div v-else class="order-list">
        <article v-for="order in orders" :key="order.id" class="order-card">
          <header class="order-card-head">
            <div>
              <span>{{ formatDate(order.createTime) }}</span>
              <button type="button" @click="copyText(order.orderNo)">订单号 {{ order.orderNo }}</button>
            </div>
            <el-tag :type="getOrderStatusMeta(order.status).type" effect="light">{{ getOrderStatusMeta(order.status).label }}</el-tag>
          </header>

          <div class="order-card-body">
            <div class="goods-list">
              <router-link
                v-for="item in order.items"
                :key="item.id"
                class="goods-row"
                :to="`/product/${item.productId}`"
              >
                <img :src="item.productImage || fallbackImage" :alt="item.productName" loading="lazy">
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

      <div v-if="!loading && total > 0" class="orders-pagination">
        <el-pagination
          v-model:current-page="page"
          v-model:page-size="size"
          :page-sizes="[10, 20, 50]"
          :total="total"
          layout="total, sizes, prev, pager, next"
          @size-change="handlePageSizeChange"
          @current-change="loadOrders"
        />
      </div>
    </section>
  </UserLayout>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import UserLayout from '../../components/UserLayout.vue'
import { cartAPI, orderAPI } from '../../api'
import { useCartStore } from '../../stores/cart'
import { useUserStore } from '../../stores/user'
import { getOrderStatusMeta } from '../../constants/orderStatus'
import { FALLBACK_IMAGE } from '../../constants/images'
import { money, formatDate } from '../../utils/format'

const router = useRouter()
const userStore = useUserStore()
const cartStore = useCartStore()

const loading = ref(false)
const orders = ref([])
const total = ref(0)
const page = ref(1)
const size = ref(10)
const statusCounts = ref({})
const activeStatus = ref('all')
const keyword = ref('')
const appliedKeyword = ref('')
const dateRange = ref([])
const appliedDateRange = ref([])
const fallbackImage = FALLBACK_IMAGE

// 筛选页签用「动作视角」措辞（回答"我该做什么"：待付款/待收货/待评价），
// 与订单状态徽章的「状态视角」措辞（回答"这单现在是什么状态"，由 constants/orderStatus.js 提供）是两回事，
// 因此本数组刻意不与 getOrderStatusMeta 统一，请勿"顺手"改掉。
const statusTabs = [
  { label: '全部', value: 'all' },
  { label: '待付款', value: '0' },
  { label: '待发货', value: '1' },
  { label: '待收货', value: '2' },
  { label: '待评价', value: '3' },
  { label: '退款/售后', value: 'service' }
]

// 「退款/售后」一个页签覆盖两种状态：4 已取消、5 已退款。其余页签与状态码一一对应。
const SERVICE_STATUSES = [4, 5]

onMounted(async () => {
  if (!userStore.isAuthenticated) {
    router.push('/login')
    return
  }
  await loadOrders()
  await cartStore.getCartList()
})

/**
 * 页签取值 → 接口 status 参数。
 * 用逗号拼接的字符串而非数组：Spring 的集合绑定按逗号切分，而 axios 对数组的默认序列化格式
 * （`status=4&status=5` 还是 `status[]=4`）依赖版本，字符串写法不依赖序列化器行为。
 */
const statusParamOf = tabValue => {
  if (tabValue === 'all') return undefined
  if (tabValue === 'service') return SERVICE_STATUSES.join(',')
  return tabValue
}

/** 关键词 / 日期这层筛选条件：列表与角标接口共用，保证两者口径一致 */
const filterParams = () => {
  const params = {}
  if (appliedKeyword.value) params.keyword = appliedKeyword.value
  if (appliedDateRange.value?.length === 2) {
    params.startDate = appliedDateRange.value[0]
    params.endDate = appliedDateRange.value[1]
  }
  return params
}

/**
 * 加载当前页订单与页签角标。
 *
 * 两件事都放服务端：列表分页后，若角标仍按客户端那点数据统计，
 * 数字只会覆盖"当前这一页"（此前是"最近 100 笔"）。两个请求并发，共用同一套筛选条件。
 */
const loadOrders = async () => {
  try {
    loading.value = true
    const filters = filterParams()
    const status = statusParamOf(activeStatus.value)
    const [listResponse, statsResponse] = await Promise.all([
      orderAPI.getList({ ...filters, ...(status ? { status } : {}), page: page.value, size: size.value }),
      orderAPI.getStatusCounts(filters)
    ])
    const pageData = listResponse.data.data || {}
    orders.value = (pageData.records || []).map(normalizeOrder)
    total.value = Number(pageData.total || 0)
    statusCounts.value = statsResponse.data.data || {}
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

/**
 * 页签角标数字，取自服务端的按状态统计。
 * 「全部」不单独请求：各状态数量之和就是总数，避免多一份可能与之矛盾的数字。
 */
const orderCount = value => {
  const counts = statusCounts.value || {}
  const statuses = value === 'all'
    ? Object.keys(counts)
    : (value === 'service' ? SERVICE_STATUSES : [Number(value)])
  return statuses.reduce((sum, status) => sum + Number(counts[status] || 0), 0)
}

/** 提交关键词/日期筛选。条件变了就必须回到第 1 页，否则会停在一个新结果集里不存在的页码上 */
const applyFilters = () => {
  appliedKeyword.value = keyword.value.trim()
  appliedDateRange.value = dateRange.value || []
  page.value = 1
  return loadOrders()
}

const resetFilters = () => {
  keyword.value = ''
  appliedKeyword.value = ''
  dateRange.value = []
  appliedDateRange.value = []
  activeStatus.value = 'all'
  page.value = 1
  return loadOrders()
}

const handleStatusChange = () => {
  applyFilters()
}

const handlePageSizeChange = () => {
  // 每页条数变化后原页码可能超出总页数，统一回到第 1 页
  page.value = 1
  return loadOrders()
}

/**
 * 支付/取消/收货后重新加载。
 * 订单状态变了就可能不再属于当前页签，当前页因此可能变空——回退一页，
 * 避免用户看到"明明有订单却是空列表"。
 */
const reloadAfterAction = async () => {
  await loadOrders()
  if (orders.value.length === 0 && page.value > 1) {
    page.value -= 1
    await loadOrders()
  }
}

const payOrder = async order => {
  try {
    await orderAPI.pay(order.orderNo)
    ElMessage.success('支付成功')
    await reloadAfterAction()
  } catch (error) {
    ElMessage.error(error.response?.data?.msg || '支付失败')
  }
}

const cancelOrder = async order => {
  try {
    await ElMessageBox.confirm('确定取消该订单吗？', '取消订单', { type: 'warning' })
    await orderAPI.cancel(order.orderNo)
    ElMessage.success('订单已取消')
    await reloadAfterAction()
  } catch (error) {
    if (error !== 'cancel') ElMessage.error(error.response?.data?.msg || '取消订单失败')
  }
}

const confirmReceipt = async order => {
  try {
    await ElMessageBox.confirm('确认已经收到商品？', '确认收货', { type: 'warning' })
    await orderAPI.confirmReceipt(order.orderNo)
    ElMessage.success('确认收货成功')
    await reloadAfterAction()
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
  color: var(--color-primary);
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

.orders-pagination {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
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
  color: var(--color-primary);
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

  /* 窄屏下分页控件会换行，居中比右对齐更好读 */
  .orders-pagination {
    justify-content: center;
  }

  .orders-pagination :deep(.el-pagination) {
    flex-wrap: wrap;
    row-gap: 8px;
    justify-content: center;
  }
}
</style>
