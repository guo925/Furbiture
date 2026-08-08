<template>
  <div>
    <section class="seller-panel">
      <div class="panel-toolbar">
        <el-tabs v-model="statusFilter" @tab-change="loadOrders">
          <el-tab-pane label="全部订单" name="all" />
          <el-tab-pane label="待付款" name="0" />
          <el-tab-pane label="待发货" name="1" />
          <el-tab-pane label="已发货" name="2" />
          <el-tab-pane label="已完成" name="3" />
          <el-tab-pane label="已取消" name="4" />
        </el-tabs>
        <div class="search-actions">
          <el-input v-model="keyword" clearable placeholder="搜索订单号" @keyup.enter="loadOrders" />
          <el-button @click="loadOrders">搜索</el-button>
        </div>
      </div>

      <el-table :data="filteredOrders" row-key="id">
        <el-table-column label="订单信息" min-width="260">
          <template #default="{ row }">
            <div class="order-main">
              <strong>{{ row.orderNo }}</strong>
              <span>{{ formatTime(row.createTime) }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="金额" width="130">
          <template #default="{ row }">¥{{ money(row.totalAmount) }}</template>
        </el-table-column>
        <el-table-column label="状态" width="110">
          <template #default="{ row }">
            <el-tag :type="statusMeta(row.status).type">{{ statusMeta(row.status).text }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="发货时效" width="160">
          <template #default="{ row }">{{ shipHint(row) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="handleDetail(row.orderNo)">详情</el-button>
            <el-button v-if="row.status === 1" link type="success" @click="handleShip(row.orderNo)">发货</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-empty v-if="!filteredOrders.length" description="暂无订单" />

      <el-pagination
        v-if="total > 0"
        v-model:current-page="currentPage"
        :page-size="pageSize"
        :total="total"
        layout="total, prev, pager, next"
        @current-change="loadOrders"
      />
    </section>

    <el-drawer v-model="detailDialogVisible" title="订单详情" size="520px">
      <div v-if="currentOrder" class="detail-drawer">
        <el-descriptions :column="1" border>
          <el-descriptions-item label="订单号">{{ currentOrder.orderNo }}</el-descriptions-item>
          <el-descriptions-item label="订单状态">
            <el-tag :type="statusMeta(currentOrder.status).type">{{ statusMeta(currentOrder.status).text }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="实付金额">¥{{ money(currentOrder.totalAmount) }}</el-descriptions-item>
          <el-descriptions-item label="下单时间">{{ formatTime(currentOrder.createTime) }}</el-descriptions-item>
        </el-descriptions>

        <h3>商品明细</h3>
        <div v-for="item in orderItems" :key="item.id" class="detail-goods">
          <img :src="item.productImage || fallbackImage" :alt="item.productName">
          <div>
            <strong>{{ item.productName }}</strong>
            <span>¥{{ money(item.price) }} x {{ item.quantity }}</span>
          </div>
          <b>¥{{ money(item.price * item.quantity) }}</b>
        </div>

        <el-button v-if="currentOrder.status === 1" type="primary" @click="handleShip(currentOrder.orderNo)">确认发货</el-button>
      </div>
    </el-drawer>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { merchantAPI } from '../../api'
import { useUserStore } from '../../stores/user'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const orders = ref([])
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)
const statusFilter = ref(route.query.status ? String(route.query.status) : 'all')
const keyword = ref('')
const detailDialogVisible = ref(false)
const currentOrder = ref(null)
const orderItems = ref([])
const fallbackImage = 'https://images.unsplash.com/photo-1555041469-a586c61ea9bc?auto=format&fit=crop&w=300&q=80'

const filteredOrders = computed(() => orders.value.filter(order => {
  const matchesKeyword = !keyword.value || order.orderNo.includes(keyword.value.trim())
  return matchesKeyword
}))

onMounted(async () => {
  if (!userStore.isAuthenticated) {
    router.push('/login')
    return
  }
  await loadOrders()
})

const loadOrders = async () => {
  const params = { page: currentPage.value, size: pageSize.value }
  if (statusFilter.value !== 'all') params.status = Number(statusFilter.value)
  const response = await merchantAPI.orders.getList(params)
  orders.value = response.data.data.records || []
  total.value = response.data.data.total || 0
}

const handleDetail = async orderNo => {
  const response = await merchantAPI.orders.getDetail(orderNo)
  currentOrder.value = response.data.data.order
  orderItems.value = response.data.data.orderItems || []
  detailDialogVisible.value = true
}

const handleShip = async orderNo => {
  await ElMessageBox.confirm('确认该订单已经完成拣货并发货？', '订单发货', { type: 'warning' })
  await merchantAPI.orders.updateStatus(orderNo, 2)
  ElMessage.success('发货成功')
  detailDialogVisible.value = false
  await loadOrders()
}

const statusMeta = status => ({
  0: { text: '待付款', type: 'warning' },
  1: { text: '待发货', type: 'primary' },
  2: { text: '已发货', type: 'success' },
  3: { text: '已完成', type: 'info' },
  4: { text: '已取消', type: 'info' },
  5: { text: '已退款', type: 'danger' }
}[status] || { text: '未知', type: 'info' })

const shipHint = order => order.status === 1 ? '建议24小时内发货' : '-'
const money = value => Number(value || 0).toFixed(2)
const formatTime = time => time ? String(time).replace('T', ' ').slice(0, 16) : '-'
</script>

<style scoped>
.seller-panel {
  background: #fff;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  padding: 18px;
}

.panel-toolbar {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: center;
  margin-bottom: 14px;
}

.search-actions {
  display: flex;
  gap: 10px;
}

.order-main strong,
.order-main span {
  display: block;
}

.order-main span {
  margin-top: 6px;
  color: #909399;
}

.detail-drawer h3 {
  margin: 22px 0 12px;
}

.detail-goods {
  display: grid;
  grid-template-columns: 64px 1fr auto;
  gap: 12px;
  align-items: center;
  padding: 12px 0;
  border-bottom: 1px solid #ebeef5;
}

.detail-goods img {
  width: 64px;
  height: 64px;
  object-fit: cover;
  border-radius: 6px;
}

.detail-goods span {
  display: block;
  margin-top: 6px;
  color: #909399;
}

@media (max-width: 760px) {
  .panel-toolbar {
    display: grid;
  }
}
</style>
