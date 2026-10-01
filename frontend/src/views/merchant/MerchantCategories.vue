<template>
  <div class="merchant-categories">
    <div class="page-header">
      <h2>商品分类</h2>
      <!-- 分类是全平台共享数据，写操作只在管理员端，这里刻意不提供新增/编辑/删除入口 -->
      <span class="readonly-hint">分类为全平台共享数据，由管理员统一维护，此处仅供查看</span>
    </div>

    <!-- 搜索 -->
    <div class="filter-bar">
      <el-input v-model="searchQuery" placeholder="搜索分类名称" style="width: 300px; margin-right: 10px">
        <template #append>
          <el-button @click="handleSearch"><el-icon><Search /></el-icon></el-button>
        </template>
      </el-input>
      <el-button @click="resetFilter">重置</el-button>
    </div>

    <!-- 分类列表（只读） -->
    <el-table :data="categories" v-loading="loading" style="width: 100%">
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column label="分类图标" width="100">
        <template #default="scope">
          <img
            v-if="scope.row.icon"
            :src="scope.row.icon"
            :alt="scope.row.name"
            class="category-icon"
          />
          <span v-else class="no-icon">-</span>
        </template>
      </el-table-column>
      <el-table-column prop="name" label="分类名称" />
      <el-table-column prop="parentName" label="父分类" width="150">
        <template #default="scope">
          {{ scope.row.parentName || '顶级分类' }}
        </template>
      </el-table-column>
      <el-table-column prop="sortOrder" label="排序" width="100" />
      <el-table-column prop="status" label="状态" width="100">
        <template #default="scope">
          <el-tag :type="scope.row.status === 1 ? 'success' : 'info'">
            {{ scope.row.status === 1 ? '启用' : '禁用' }}
          </el-tag>
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
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { merchantAPI } from '../../api'
import { Search } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'

/** 加载态：无此标记时，数据到达前表格为空会先闪一下"暂无数据" */
const loading = ref(false)
const categories = ref([])
const total = ref(0)
const currentPage = ref(1)
const pageSize = ref(10)
const searchQuery = ref('')

onMounted(async () => {
  await loadCategories()
})

const loadCategories = async () => {
  loading.value = true
  try {
    const params = {
      page: currentPage.value,
      size: pageSize.value
    }
    if (searchQuery.value) {
      params.name = searchQuery.value
    }
    const response = await merchantAPI.categories.getList(params)
    categories.value = response.data.data.records
    total.value = response.data.data.total
  } catch (error) {
    console.error('获取分类失败:', error?.message)
    ElMessage.error('获取分类失败')
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  currentPage.value = 1
  loadCategories()
}

const resetFilter = () => {
  searchQuery.value = ''
  currentPage.value = 1
  loadCategories()
}

const handleSizeChange = (size) => {
  pageSize.value = size
  loadCategories()
}

const handleCurrentChange = (page) => {
  currentPage.value = page
  loadCategories()
}
</script>

<style scoped>
.merchant-categories {
  background: #fff;
  padding: 20px;
  border-radius: 8px;
  border: 1px solid #e5e7eb;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.page-header h2 {
  margin: 0;
  font-size: 20px;
  font-weight: bold;
  color: #333;
}

.readonly-hint {
  font-size: 13px;
  color: #909399;
}

.filter-bar {
  margin-bottom: 20px;
  display: flex;
  align-items: center;
}

.category-icon {
  width: 50px;
  height: 50px;
  object-fit: cover;
  border-radius: 4px;
}

.no-icon {
  color: #999;
  font-size: 14px;
}

.pagination {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}
</style>
