<template>
  <div>
  <div class="merchant-categories">
    <div class="page-header">
      <h2>商品分类</h2>
      <el-button type="primary" @click="handleAdd">添加分类</el-button>
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

    <!-- 分类列表 -->
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
      <el-table-column prop="sort" label="排序" width="100" />
      <el-table-column prop="status" label="状态" width="100">
        <template #default="scope">
          <el-switch
            v-model="scope.row.status"
            :active-value="1"
            :inactive-value="0"
            @change="handleStatusChange(scope.row.id, scope.row.status)"
          />
        </template>
      </el-table-column>
      <el-table-column label="操作" width="200">
        <template #default="scope">
          <el-button type="primary" @click="handleEdit(scope.row)">
            <el-icon><Edit /></el-icon>
          </el-button>
          <el-button type="danger" @click="handleDelete(scope.row.id)">
            <el-icon><Delete /></el-icon>
          </el-button>
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

    <!-- 添加/编辑分类对话框 -->
    <el-dialog v-model="dialogVisible" :title="dialogTitle">
      <el-form :model="categoryForm" :rules="categoryRules" ref="categoryFormRef" label-width="100px">
        <el-form-item label="分类名称" prop="name">
          <el-input v-model="categoryForm.name" />
        </el-form-item>
        <el-form-item label="父分类">
          <el-select v-model="categoryForm.parentId" placeholder="选择父分类">
            <el-option label="顶级分类" value="0" />
            <el-option 
              v-for="category in parentCategories" 
              :key="category.id" 
              :label="category.name" 
              :value="category.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="排序" prop="sort">
          <el-input v-model.number="categoryForm.sort" type="number" />
        </el-form-item>
        <el-form-item label="分类图标">
          <el-upload
            class="upload-demo"
            action=""
            :http-request="handleUpload"
            :auto-upload="true"
            accept="image/*"
            :show-file-list="false"
          >
            <el-button type="primary">点击上传</el-button>
            <template #tip>
              <div class="el-upload__tip">
                只能上传图片文件，且不超过10MB
              </div>
            </template>
          </el-upload>
          <div v-if="categoryForm.icon" style="margin-top: 10px">
            <img :src="categoryForm.icon" :alt="categoryForm.name" style="width: 100px; height: 100px; object-fit: cover; border-radius: 4px">
          </div>
        </el-form-item>
        <el-form-item label="分类状态" prop="status">
          <el-switch v-model="categoryForm.status" />
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" @click="handleSave">保存</el-button>
        </span>
      </template>
    </el-dialog>
  </div>
  </div>
</template>

<script setup>
import { ref, onMounted, reactive } from 'vue'
import { merchantAPI, fileAPI } from '../../api'
import { Search, Edit, Delete } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'

/** 加载态：无此标记时，数据到达前表格为空会先闪一下"暂无数据" */
const loading = ref(false)
const categories = ref([])
const total = ref(0)
const currentPage = ref(1)
const pageSize = ref(10)
const searchQuery = ref('')
const parentCategories = ref([])
const updatingStatus = ref(new Set())

const dialogVisible = ref(false)
const dialogTitle = ref('添加分类')
const categoryFormRef = ref(null)

const categoryForm = reactive({
  id: null,
  name: '',
  parentId: '0',
  sort: 0,
  icon: '',
  status: true
})

const categoryRules = {
  name: [{ required: true, message: '请输入分类名称', trigger: 'blur' }],
  sort: [{ required: true, message: '请输入排序号', trigger: 'blur' }]
}

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

    // 获取所有顶级分类用于下拉选择
    const allCategories = await merchantAPI.categories.getList()
    parentCategories.value = allCategories.data.data.records.filter(c => c.parentId === 0 || !c.parentId)
  } catch (error) {
    console.error('获取分类失败:', error)
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

const handleAdd = () => {
  dialogTitle.value = '添加分类'
  Object.assign(categoryForm, {
    id: null,
    name: '',
    parentId: '0',
    sort: 0,
    icon: '',
    status: true
  })
  loadParentCategories()
  dialogVisible.value = true
}

const loadParentCategories = async () => {
  try {
    const allCategories = await merchantAPI.categories.getList()
    parentCategories.value = allCategories.data.data.records.filter(c => c.parentId === 0 || !c.parentId)
  } catch (error) {
    console.error('获取分类失败:', error)
  }
}

const handleUpload = async (options) => {
  const file = options.file
  try {
    const actualFile = file.raw || file
    const response = await fileAPI.upload(actualFile)
    if (response.data && response.data.data) {
      categoryForm.icon = response.data.data
      ElMessage.success('图片上传成功')
      options.onSuccess()
    } else {
      ElMessage.error('上传失败：无效的响应')
      options.onError()
    }
  } catch (error) {
    console.error('上传失败:', error)
    const errorMsg = error.response?.data?.msg || error.message || '图片上传失败'
    ElMessage.error(errorMsg)
    options.onError()
  }
}

const handleEdit = async (row) => {
  dialogTitle.value = '编辑分类'
  Object.assign(categoryForm, {
    id: row.id,
    name: row.name,
    parentId: row.parentId ? row.parentId.toString() : '0',
    sort: row.sort || 0,
    icon: row.icon || '',
    status: row.status === 1
  })
  await loadParentCategories()
  dialogVisible.value = true
}

const handleSave = async () => {
  if (!categoryFormRef.value) return

  try {
    await categoryFormRef.value.validate()

    const categoryData = {
      ...categoryForm,
      parentId: parseInt(categoryForm.parentId),
      sort: parseInt(categoryForm.sort),
      status: categoryForm.status ? 1 : 0
    }

    if (categoryForm.id) {
      await merchantAPI.categories.update(categoryForm.id, categoryData)
      ElMessage.success('更新成功')
    } else {
      await merchantAPI.categories.create(categoryData)
      ElMessage.success('添加成功')
    }

    dialogVisible.value = false
    loadCategories()
  } catch (error) {
    ElMessage.error(error.message || '操作失败')
  }
}

const handleDelete = async (id) => {
  try {
    await ElMessageBox.confirm('确定要删除这个分类吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })

    await merchantAPI.categories.delete(id)
    ElMessage.success('删除成功')
    loadCategories()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(error.message || '删除失败')
    }
  }
}

const handleStatusChange = async (id, status) => {
  if (updatingStatus.value.has(id)) {
    return
  }
  
  try {
    updatingStatus.value.add(id)
    const categoryData = {
      status: status ? 1 : 0
    }
    await merchantAPI.categories.update(id, categoryData)
    ElMessage.success('状态更新成功')
  } catch (error) {
    ElMessage.error(error.message || '状态更新失败')
    loadCategories()
  } finally {
    updatingStatus.value.delete(id)
  }
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

.dialog-footer {
  text-align: right;
}
</style>
