<template>
  <div class="admin-page">
    <div class="page-toolbar">
      <div class="toolbar-left">
        <h2>订单管理</h2>
        <el-input v-model="searchOrderNo" placeholder="订单号" clearable class="search-input" @keyup.enter="handleSearch" @clear="handleSearch">
          <template #prefix><el-icon><Search /></el-icon></template>
        </el-input>
        <el-select v-model="filterStatus" placeholder="订单状态" clearable style="width:130px" @change="handleSearch">
          <el-option label="全部" :value="null" />
          <el-option label="待付款" :value="0" />
          <el-option label="已付款" :value="1" />
          <el-option label="已发货" :value="2" />
          <el-option label="已完成" :value="3" />
          <el-option label="已取消" :value="4" />
        </el-select>
        <el-button type="primary" @click="handleSearch">查询</el-button>
      </div>
    </div>

    <div class="table-card">
      <el-table :data="orders" v-loading="loading" stripe>
        <el-table-column prop="orderNo" label="订单号" width="200">
          <template #default="{ row }"><span class="order-no">{{ row.orderNo?.substring(0, 12) }}...</span></template>
        </el-table-column>
        <el-table-column label="用户" width="100">
          <template #default="{ row }">
            <el-tag size="small" type="info">UID: {{ row.userId }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="金额" width="120">
          <template #default="{ row }"><span class="price-tag">¥{{ row.totalAmount }}</span></template>
        </el-table-column>
        <el-table-column label="状态" width="120">
          <template #default="{ row }">
            <el-tag :type="statusType(row.status)" size="small" effect="plain">
              {{ statusText(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="下单时间" width="170">
          <template #default="{ row }">{{ row.createTime }}</template>
        </el-table-column>
        <el-table-column label="操作" width="220">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="showDetail(row)">详情</el-button>
            <el-button v-if="row.status === 0" link type="warning" size="small" @click="handleCancel(row)">取消</el-button>
            <el-button v-if="row.status === 1" link type="success" size="small" @click="handleDeliver(row)">发货</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div class="table-footer">
        <el-pagination
          v-model:current-page="page" v-model:page-size="size"
          :page-sizes="[10,20,50]" :total="total" layout="total, sizes, prev, pager, next"
          @size-change="loadData" @current-change="loadData"
        />
      </div>
    </div>

    <!-- 订单详情抽屉 -->
    <el-drawer v-model="drawerVisible" title="订单详情" :size="isMobile ? '88%' : '480px'">
      <template v-if="detailOrder">
        <el-descriptions :column="2" border size="small">
          <el-descriptions-item label="订单号">{{ detailOrder.orderNo }}</el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag :type="statusType(detailOrder.status)" size="small">{{ statusText(detailOrder.status) }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="用户ID">{{ detailOrder.userId }}</el-descriptions-item>
          <el-descriptions-item label="总金额"><span class="price-tag">¥{{ detailOrder.totalAmount }}</span></el-descriptions-item>
          <el-descriptions-item label="下单时间" :span="2">{{ detailOrder.createTime }}</el-descriptions-item>
          <el-descriptions-item label="付款时间" v-if="detailOrder.payTime">{{ detailOrder.payTime }}</el-descriptions-item>
          <el-descriptions-item label="发货时间" v-if="detailOrder.deliveryTime">{{ detailOrder.deliveryTime }}</el-descriptions-item>
          <el-descriptions-item label="完成时间" v-if="detailOrder.finishTime">{{ detailOrder.finishTime }}</el-descriptions-item>
          <el-descriptions-item label="取消时间" v-if="detailOrder.cancelTime">{{ detailOrder.cancelTime }}</el-descriptions-item>
        </el-descriptions>
        <div style="margin-top:16px">
          <h4 style="margin-bottom:12px;color:#2d3748">商品明细</h4>
          <div v-for="item in detailItems" :key="item.id" class="order-item">
            <el-image :src="item.productImage" fit="cover" class="item-thumb">
              <template #error><div class="item-thumb-placeholder"><el-icon :size="14"><Picture /></el-icon></div></template>
            </el-image>
            <div class="item-info">
              <span class="item-name">{{ item.productName }}</span>
              <span class="item-meta">¥{{ item.price }} × {{ item.quantity }}</span>
            </div>
            <span class="item-total">¥{{ (item.price * item.quantity).toFixed(2) }}</span>
          </div>
        </div>
      </template>
    </el-drawer>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { adminAPI } from '../../api/modules/admin'
import { Search, Picture } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useBreakpoint } from '../../composables/useBreakpoint'

// 详情抽屉需要随视口收窄，故用断点状态而不是写死 480px
const { isMobile } = useBreakpoint()

const orders = ref([]), total = ref(0), page = ref(1), size = ref(10), loading = ref(false)
const searchOrderNo = ref(''), filterStatus = ref(null)

const drawerVisible = ref(false), detailOrder = ref(null), detailItems = ref([])

const statusMap = { 0: '待付款', 1: '已付款', 2: '已发货', 3: '已完成', 4: '已取消', 5: '已退款' }
const statusType = (s) => ({ 0: 'warning', 1: 'primary', 2: '', 3: 'success', 4: 'info', 5: 'info' }[s] || 'info')
const statusText = (s) => statusMap[s] || '未知'

const loadData = async () => {
  loading.value = true
  try {
    const params = { page: page.value, size: size.value }
    if (searchOrderNo.value) params.orderNo = searchOrderNo.value
    if (filterStatus.value !== null) params.status = filterStatus.value
    const res = await adminAPI.orders.getList(params)
    orders.value = res.data.data.records || []
    total.value = res.data.data.total || 0
  } finally { loading.value = false }
}

const handleSearch = () => { page.value = 1; loadData() }

const showDetail = async (row) => {
  detailOrder.value = row
  try {
    const res = await adminAPI.orders.getDetail(row.id)
    detailItems.value = res.data.data?.orderItems || []
  } catch (e) { detailItems.value = [] }
  drawerVisible.value = true
}

const handleDeliver = async (row) => {
  await ElMessageBox.confirm(`确认对订单 ${row.orderNo?.substring(0, 12)} 执行发货？`, '确认发货', { type: 'info' })
  await adminAPI.orders.updateStatus(row.id, 2)
  ElMessage.success('发货成功')
  loadData()
}

const handleCancel = async (row) => {
  await ElMessageBox.confirm(`确定取消订单 ${row.orderNo?.substring(0, 12)}？`, '取消确认', { type: 'warning' })
  await adminAPI.orders.updateStatus(row.id, 4)
  ElMessage.success('已取消')
  loadData()
}

onMounted(() => loadData())
</script>

<style scoped>
.page-toolbar {
  display: flex; justify-content: space-between; align-items: center;
  margin-bottom: 16px; flex-wrap: wrap; gap: 12px;
}
.page-toolbar h2 { margin: 0; font-size: 20px; font-weight: 600; color: #1a202c; }
.toolbar-left { display: flex; align-items: center; gap: 12px; }
.search-input { width: 220px; }
.table-card {
  background: #fff; border-radius: 12px; padding: 20px;
  box-shadow: 0 1px 3px rgba(0,0,0,0.06); border: 1px solid #edf2f7;
}
.order-no { font-family: monospace; font-size: 13px; color: #4a5568; }
.price-tag { color: #e53e3e; font-weight: 600; font-size: 14px; }
.text-muted { color: #a0aec0; }
.table-footer { display: flex; justify-content: flex-end; margin-top: 16px; }

.order-item {
  display: flex; align-items: center; gap: 12px;
  padding: 12px 0; border-bottom: 1px solid #f7fafc;
}
.item-thumb { width: 48px; height: 48px; border-radius: 6px; flex-shrink: 0; }
.item-thumb-placeholder {
  width: 48px; height: 48px; border-radius: 6px; background: #f7fafc;
  display: flex; align-items: center; justify-content: center; color: #cbd5e0;
}
.item-info { flex: 1; display: flex; flex-direction: column; gap: 4px; }
.item-name { font-size: 14px; color: #2d3748; }
.item-meta { font-size: 12px; color: #a0aec0; }
.item-total { font-weight: 600; color: #2d3748; font-size: 14px; }

/* ===== 响应式：断点取值见 composables/useBreakpoint.js ===== */

/* 平板及以下：卡片内边距收窄，把宽度还给表格本身 */
@media (max-width: 1024px) {
  .table-card {
    padding: 12px;
  }
}

/* 移动端：工具栏改为竖向堆叠（搜索框占满整行），分页居中并允许换行。
   表格列宽合计 930px，窄屏下由 el-table 自身横向滚动兜底，不做压缩。 */
@media (max-width: 640px) {
  .toolbar-left {
    flex-wrap: wrap;
  }

  .search-input {
    width: 100%;
  }

  .table-footer {
    justify-content: center;
  }

  .table-footer :deep(.el-pagination) {
    flex-wrap: wrap;
    row-gap: 8px;
    justify-content: center;
  }
}
</style>
