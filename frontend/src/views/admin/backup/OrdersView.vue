<template>
  <div class="admin-orders">
    <div class="page-header">
      <h2>订单管理</h2>
    </div>

    <!-- 搜索和筛选 -->
    <div class="filter-bar">
      <el-input v-model="searchQuery" placeholder="搜索订单号" style="width: 300px; margin-right: 10px">
        <template #append>
          <el-button @click="handleSearch"><el-icon><Search /></el-icon></el-button>
        </template>
      </el-input>
      <el-select v-model="statusFilter" placeholder="选择状态" style="width: 120px; margin-right: 10px">
        <el-option label="全部" value=""></el-option>
        <el-option label="待支付" value="0"></el-option>
        <el-option label="已支付" value="1"></el-option>
        <el-option label="已发货" value="2"></el-option>
        <el-option label="已完成" value="3"></el-option>
        <el-option label="已取消" value="4"></el-option>
        <el-option label="已退款" value="5"></el-option>
      </el-select>
      <el-date-picker
        v-model="dateRange"
        type="daterange"
        range-separator="至"
        start-placeholder="开始日期"
        end-placeholder="结束日期"
        value-format="YYYY-MM-DD"
        @change="handleDateChange"
        style="margin-right: 10px"
      />
      <el-button @click="resetFilter">重置</el-button>
    </div>

    <!-- 订单列表 -->
    <el-table :data="orders" style="width: 100%">
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column prop="orderNo" label="订单号" />
      <el-table-column prop="userId" label="用户ID" width="100" />
      <el-table-column prop="totalAmount" label="总金额" width="100">
        <template #default="scope">
          ¥{{ scope.row.totalAmount }}
        </template>
      </el-table-column>
      <el-table-column prop="status" label="状态" width="120">
        <template #default="scope">
          <el-select v-model="scope.row.status" @change="handleStatusChange(scope.row.id, scope.row.status)">
            <el-option label="待支付" :value="0" />
            <el-option label="已支付" :value="1" />
            <el-option label="已发货" :value="2" />
            <el-option label="已完成" :value="3" />
            <el-option label="已取消" :value="4" />
            <el-option label="已退款" :value="5" />
          </el-select>
        </template>
      </el-table-column>
      <el-table-column prop="createTime" label="创建时间" width="180" />
      <el-table-column label="操作" width="150">
        <template #default="scope">
          <el-button @click="viewOrderDetail(scope.row.id)">查看详情</el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 分页 -->
    <div class="pagination">
      <el-pagination
        v-model:current-page="currentPage"
        v-model:page-size="pageSize"
        :page-sizes="[10, 20, 50]"
        layout="total, sizes, prev, pager, next, jumper"
        :total="total"
        @size-change="handleSizeChange"
        @current-change="handleCurrentChange"
      />
    </div>

    <!-- 订单详情对话框 -->
    <el-dialog
      v-model="dialogVisible"
      title="订单详情"
      width="800px"
    >
      <div v-if="orderDetail" class="order-detail">
        <el-descriptions title="基本信息" :column="2">
          <el-descriptions-item label="订单ID">{{ orderDetail.id }}</el-descriptions-item>
          <el-descriptions-item label="订单号">{{ orderDetail.orderNo }}</el-descriptions-item>
          <el-descriptions-item label="用户ID">{{ orderDetail.userId }}</el-descriptions-item>
          <el-descriptions-item label="总金额">¥{{ orderDetail.totalAmount }}</el-descriptions-item>
          <el-descriptions-item label="状态">{{ getStatusText(orderDetail.status) }}</el-descriptions-item>
          <el-descriptions-item label="创建时间">{{ orderDetail.createTime }}</el-descriptions-item>
          <el-descriptions-item label="支付时间">{{ orderDetail.payTime || '未支付' }}</el-descriptions-item>
          <el-descriptions-item label="发货时间">{{ orderDetail.deliveryTime || '未发货' }}</el-descriptions-item>
          <el-descriptions-item label="完成时间">{{ orderDetail.finishTime || '未完成' }}</el-descriptions-item>
          <el-descriptions-item label="取消时间">{{ orderDetail.cancelTime || '未取消' }}</el-descriptions-item>
        </el-descriptions>

        <el-divider />

        <h3>订单商品</h3>
        <el-table :data="orderItems" style="width: 100%">
          <el-table-column prop="productId" label="商品ID" width="100" />
          <el-table-column prop="productName" label="商品名称" />
          <el-table-column prop="quantity" label="数量" width="80" />
          <el-table-column prop="price" label="单价" width="100">
            <template #default="scope">
              ¥{{ scope.row.price }}
            </template>
          </el-table-column>
          <el-table-column prop="totalPrice" label="小计" width="100">
            <template #default="scope">
              ¥{{ scope.row.totalPrice }}
            </template>
          </el-table-column>
        </el-table>

        <el-divider />

        <h3>收货地址</h3>
        <el-descriptions v-if="address" :column="1">
          <el-descriptions-item label="收货人">{{ address.name }}</el-descriptions-item>
          <el-descriptions-item label="联系电话">{{ address.phone }}</el-descriptions-item>
          <el-descriptions-item label="详细地址">{{ address.province }} {{ address.city }} {{ address.district }} {{ address.detail }}</el-descriptions-item>
        </el-descriptions>
        <div v-else>无地址信息</div>
      </div>
      <div v-else class="loading">
        <el-icon class="is-loading"><Loading /></el-icon>
        <span>加载中...</span>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { adminAPI } from '../../api'
import { Search, Loading } from '@element-plus/icons-vue'
import { ElMessage, ElIcon } from 'element-plus'

const route = useRoute()
const router = useRouter()
const orders = ref([])
const total = ref(0)
const currentPage = ref(1)
const pageSize = ref(10)
const searchQuery = ref('')
const statusFilter = ref('')
const dateRange = ref([])
const startDate = ref('')
const endDate = ref('')

// 订单详情对话框相关
const dialogVisible = ref(false)
const orderDetail = ref(null)
const orderItems = ref([])
const address = ref(null)

onMounted(async () => {
  // 从路由参数中获取日期范围
  if (route.query.startDate) {
    startDate.value = route.query.startDate
    dateRange.value = [route.query.startDate, route.query.endDate || route.query.startDate]
  }
  if (route.query.endDate) {
    endDate.value = route.query.endDate
  }
  await loadOrders()
})

// 监听路由变化，更新日期参数
watch(() => route.query, (newQuery) => {
  if (newQuery.startDate) {
    startDate.value = newQuery.startDate
    dateRange.value = [newQuery.startDate, newQuery.endDate || newQuery.startDate]
  }
  if (newQuery.endDate) {
    endDate.value = newQuery.endDate
  }
  // 重新加载订单
  loadOrders()
}, { deep: true })

const loadOrders = async () => {
  try {
    const params = {
      page: currentPage.value,
      size: pageSize.value
    }
    if (searchQuery.value) {
      params.orderNo = searchQuery.value
    }
    if (statusFilter.value) {
      params.status = statusFilter.value
    }
    if (startDate.value) {
      params.startDate = startDate.value
    }
    if (endDate.value) {
      params.endDate = endDate.value
    }
    const response = await adminAPI.orders.getList(params)
    orders.value = response.data.data.records
    total.value = response.data.data.total
  } catch (error) {
    console.error('获取订单失败:', error)
  }
}

const handleSearch = () => {
  currentPage.value = 1
  loadOrders()
}

const handleDateChange = (val) => {
  if (val && val.length === 2) {
    startDate.value = val[0]
    endDate.value = val[1]
  } else {
    startDate.value = ''
    endDate.value = ''
  }
  currentPage.value = 1
  loadOrders()
}

const resetFilter = () => {
  searchQuery.value = ''
  statusFilter.value = ''
  dateRange.value = []
  startDate.value = ''
  endDate.value = ''
  currentPage.value = 1
  // 移除路由参数中的日期范围
  router.push({
    path: '/admin/orders',
    query: {}
  })
  loadOrders()
}

const handleSizeChange = (size) => {
  pageSize.value = size
  loadOrders()
}

const handleCurrentChange = (page) => {
  currentPage.value = page
  loadOrders()
}

const handleStatusChange = async (id, status) => {
  try {
    await adminAPI.orders.updateStatus(id, status)
    ElMessage.success('状态更新成功')
  } catch (error) {
    ElMessage.error(error.message || '状态更新失败')
    // 恢复原来的状态
    loadOrders()
  }
}

const viewOrderDetail = async (id) => {
  try {
    // 显示加载状态
    orderDetail.value = null
    orderItems.value = []
    address.value = null
    dialogVisible.value = true

    // 获取订单基本信息
    const response = await adminAPI.orders.getDetail(id)
    orderDetail.value = response.data.data

    // 模拟获取订单商品（实际应该调用真实API）
    // 这里使用模拟数据，实际项目中需要调用后端接口
    orderItems.value = [
      {
        productId: 1,
        productName: '现代简约沙发',
        quantity: 1,
        price: 2999.00,
        totalPrice: 2999.00
      },
      {
        productId: 2,
        productName: '北欧风格茶几',
        quantity: 1,
        price: 899.00,
        totalPrice: 899.00
      }
    ]

    // 模拟获取收货地址（实际应该调用真实API）
    // 这里使用模拟数据，实际项目中需要调用后端接口
    address.value = {
      name: '张三',
      phone: '13800138000',
      province: '北京市',
      city: '北京市',
      district: '朝阳区',
      detail: '建国路88号SOHO现代城'
    }
  } catch (error) {
    console.error('获取订单详情失败:', error)
    ElMessage.error('获取订单详情失败')
    dialogVisible.value = false
  }
}

const getStatusText = (status) => {
  const statusMap = {
    0: '待支付',
    1: '已支付',
    2: '已发货',
    3: '已完成',
    4: '已取消',
    5: '已退款'
  }
  return statusMap[status] || '未知状态'
}
</script>

<style scoped>
.admin-orders {
  background: #fff;
  padding: 20px;
  border-radius: 8px;
  box-shadow: 0 2px 12px rgba(0,0,0,0.1);
}

.page-header {
  margin-bottom: 20px;
}

.page-header h2 {
  margin: 0;
  font-size: 20px;
  font-weight: bold;
  color: #333;
}

.filter-bar {
  margin-bottom: 20px;
  display: flex;
  align-items: center;
}

.pagination {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}

.order-detail {
  padding: 10px 0;
}

.order-detail h3 {
  margin: 0 0 15px 0;
  font-size: 16px;
  font-weight: bold;
  color: #333;
}

.loading {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 40px 0;
  color: #999;
}

.loading .is-loading {
  margin-right: 10px;
  animation: rotating 2s linear infinite;
}

@keyframes rotating {
  from {
    transform: rotate(0deg);
  }
  to {
    transform: rotate(360deg);
  }
}
</style>