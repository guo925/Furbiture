<template>
  <UserLayout>
    <div class="shop-container order-detail-page">
      <div class="page-head">
        <h1>订单详情</h1>
        <el-button @click="goBack" text>← 返回订单列表</el-button>
      </div>

      <el-skeleton v-if="loading" :rows="8" animated />

      <div v-else-if="orderDetail" class="detail-grid">
        <!-- 订单基本信息 -->
        <section class="panel">
          <h2>订单信息</h2>
          <el-descriptions :column="2" border>
            <el-descriptions-item label="订单号">{{ orderDetail.orderNumber }}</el-descriptions-item>
            <el-descriptions-item label="状态">
              <el-tag :type="statusMeta.type">{{ statusMeta.text }}</el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="总金额">
              <span class="price">¥{{ orderDetail.totalPrice }}</span>
            </el-descriptions-item>
            <el-descriptions-item label="下单时间">{{ orderDetail.createTime }}</el-descriptions-item>
            <el-descriptions-item v-if="orderDetail.payTime" label="付款时间">{{ orderDetail.payTime }}</el-descriptions-item>
            <el-descriptions-item v-if="orderDetail.deliveryTime" label="发货时间">{{ orderDetail.deliveryTime }}</el-descriptions-item>
          </el-descriptions>
        </section>

        <!-- 物流时间线 -->
        <section v-if="logisticsEvents.length" class="panel">
          <h2>物流追踪</h2>
          <el-timeline>
            <el-timeline-item
              v-for="event in logisticsEvents"
              :key="event.time"
              :timestamp="event.time"
              :type="event.type"
              :icon="event.icon"
            >
              {{ event.text }}
            </el-timeline-item>
          </el-timeline>
        </section>

        <!-- 收货地址 -->
        <section v-if="address" class="panel">
          <h2>收货信息</h2>
          <div class="address-card">
            <strong>{{ address.receiver }} {{ address.phone }}</strong>
            <span>{{ address.province }}{{ address.city }}{{ address.district }}{{ address.detail }}</span>
          </div>
        </section>

        <!-- 商品明细 -->
        <section class="panel">
          <h2>商品明细</h2>
          <div v-for="item in orderItems" :key="item.id" class="goods-row">
            <img :src="item.productImage || fallbackImage" :alt="item.productName">
            <div class="goods-info">
              <router-link :to="`/product/${item.productId}`">{{ item.productName }}</router-link>
              <span>¥{{ item.price }} × {{ item.quantity }}</span>
            </div>
            <strong>¥{{ item.totalPrice }}</strong>
          </div>
        </section>

        <!-- 操作按钮 -->
        <div class="action-bar">
          <el-button @click="goBack">返回订单列表</el-button>
          <el-button v-if="canCancel" type="danger" @click="cancelOrder">取消订单</el-button>
          <el-button v-if="canPay" type="success" @click="payOrder">去支付</el-button>
          <el-button v-if="canConfirm" type="primary" @click="confirmReceipt">确认收货</el-button>
        </div>
      </div>

      <div v-else class="empty">
        <el-empty description="订单不存在" />
        <el-button @click="goBack">返回订单列表</el-button>
      </div>
    </div>
  </UserLayout>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '../../stores/user'
import { orderAPI, addressAPI } from '../../api'
import { ElMessage } from 'element-plus'
import { Clock, Van, Check } from '@element-plus/icons-vue'
import UserLayout from '../../components/UserLayout.vue'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const loading = ref(true)
const orderDetail = ref(null)
const orderItems = ref([])
const address = ref(null)
const fallbackImage = 'https://images.unsplash.com/photo-1555041469-a586c61ea9bc?auto=format&fit=crop&w=300&q=80'

const statusMap = {
  0: { text: '待付款', type: 'warning' },
  1: { text: '已付款', type: 'primary' },
  2: { text: '已发货', type: 'success' },
  3: { text: '已完成', type: 'info' },
  4: { text: '已取消', type: 'info' },
  5: { text: '已退款', type: 'danger' }
}

const statusMeta = computed(() => {
  const status = orderDetail.value?.status
  return statusMap[status] || { text: '未知', type: 'info' }
})

const canCancel = computed(() => orderDetail.value?.status === 0)
const canPay = computed(() => orderDetail.value?.status === 0)
const canConfirm = computed(() => orderDetail.value?.status === 2)

const logisticsEvents = computed(() => {
  if (!orderDetail.value) return []
  const events = []
  const order = orderDetail.value
  events.push({ time: order.createTime, text: '订单已提交', type: 'primary', icon: Clock })
  if (order.payTime) events.push({ time: order.payTime, text: '买家已付款', type: 'success', icon: Check })
  if (order.deliveryTime) events.push({ time: order.deliveryTime, text: '商家已发货，运输中', type: 'warning', icon: Van })
  if (order.finishTime) events.push({ time: order.finishTime, text: '订单已完成', type: 'success', icon: Check })
  return events
})

onMounted(async () => {
  if (!userStore.isAuthenticated) {
    router.push('/login')
    return
  }
  await loadOrderDetail()
})

const loadOrderDetail = async () => {
  loading.value = true
  try {
    const orderNo = route.params.id
    const response = await orderAPI.getDetail(orderNo)
    const data = response.data.data

    orderDetail.value = {
      id: data.order.id,
      orderNumber: data.order.orderNo,
      status: data.order.status,
      totalPrice: data.order.totalAmount,
      createTime: data.order.createTime,
      payTime: data.order.payTime,
      deliveryTime: data.order.deliveryTime,
      finishTime: data.order.finishTime
    }

    orderItems.value = (data.items || []).map(item => ({
      id: item.id,
      productId: item.productId,
      productName: item.productName,
      productImage: item.productImage || '',
      quantity: item.quantity,
      price: item.price,
      totalPrice: item.totalPrice || (Number(item.price || 0) * Number(item.quantity || 0)).toFixed(2)
    }))

    if (data.order.addressId) {
      await loadAddress(data.order.addressId)
    }
  } catch (error) {
    console.error('获取订单详情失败:', error)
    ElMessage.error('获取订单详情失败')
  } finally {
    loading.value = false
  }
}

const loadAddress = async (addressId) => {
  try {
    const response = await addressAPI.getList()
    const addresses = response.data.data || []
    const addr = addresses.find(a => a.id == addressId)
    if (addr) {
      address.value = {
        receiver: addr.name,
        phone: addr.phone,
        province: addr.province,
        city: addr.city,
        district: addr.district,
        detail: addr.detailAddress
      }
    }
  } catch (error) { /* ignore */ }
}

const goBack = () => router.push('/orders')

const cancelOrder = async () => {
  try {
    await orderAPI.cancel(orderDetail.value.orderNumber)
    ElMessage.success('订单已取消')
    await loadOrderDetail()
  } catch (error) {
    ElMessage.error(error.message || '取消订单失败')
  }
}

const payOrder = async () => {
  try {
    await orderAPI.pay(orderDetail.value.orderNumber)
    ElMessage.success('支付成功')
    await loadOrderDetail()
  } catch (error) {
    ElMessage.error(error.message || '支付失败')
  }
}

const confirmReceipt = async () => {
  try {
    await orderAPI.confirmReceipt(orderDetail.value.orderNumber)
    ElMessage.success('确认收货成功')
    await loadOrderDetail()
  } catch (error) {
    ElMessage.error(error.message || '确认收货失败')
  }
}
</script>

<style scoped>
.shop-container {
  width: min(1200px, calc(100% - 32px));
  margin: 0 auto;
}

.order-detail-page {
  padding: 28px 0;
}

.page-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 22px;
}

.page-head h1 { font-size: 28px; margin: 0; }

.detail-grid { display: grid; gap: 18px; }

.panel {
  background: var(--color-bg-card, #fff);
  border: 1px solid var(--color-border-light, #f0f0f0);
  border-radius: var(--radius-md);
  padding: 20px;
}

.panel h2 { font-size: 18px; margin: 0 0 16px; }

.price { color: var(--color-primary); font-weight: 700; font-size: 18px; }

.address-card { display: grid; gap: 8px; }
.address-card span { color: var(--color-text-secondary); }

.goods-row {
  display: grid;
  grid-template-columns: 68px 1fr auto;
  align-items: center;
  gap: 14px;
  padding: 12px 0;
  border-top: 1px solid var(--color-border-light, #f0f0f0);
}

.goods-row:first-child { border-top: 0; }

.goods-row img {
  width: 68px; height: 68px;
  object-fit: cover;
  border-radius: var(--radius-sm);
  background: #f3f3f3;
}

.goods-info a {
  display: block;
  color: var(--color-text-primary);
  text-decoration: none;
  font-weight: 600;
}
.goods-info a:hover { color: var(--color-primary); }
.goods-info span { display: block; margin-top: 6px; color: var(--color-text-muted); font-size: 13px; }

.goods-row strong { color: var(--color-primary); }

.action-bar {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  padding-top: 12px;
}

.empty {
  background: var(--color-bg-card, #fff);
  border-radius: var(--radius-md);
  padding: 40px;
  text-align: center;
}

@media (max-width: 600px) {
  .goods-row { grid-template-columns: 56px 1fr; }
  .goods-row strong { grid-column: 2; }
  .page-head { flex-direction: column; align-items: flex-start; gap: 10px; }
}
</style>
