<template>
  <div class="admin-page">
    <div class="page-toolbar">
      <div class="toolbar-left">
        <h2>商品管理</h2>
        <el-input
          v-model="searchKeyword" placeholder="搜索商品名称"
          clearable class="search-input" @keyup.enter="handleSearch" @clear="handleSearch"
        >
          <template #prefix><el-icon><Search /></el-icon></template>
        </el-input>
        <el-select v-model="filterStatus" placeholder="商品状态" clearable style="width:120px" @change="handleSearch">
          <el-option label="全部" :value="null" />
          <el-option label="已上架" :value="1" />
          <el-option label="已下架" :value="0" />
        </el-select>
        <el-button type="primary" @click="handleSearch">查询</el-button>
      </div>
      <el-button type="primary" @click="handleAdd"><el-icon><Plus /></el-icon>新增商品</el-button>
    </div>

    <!-- 商品表格 -->
    <div class="table-card">
      <el-table :data="products" v-loading="loading" stripe class="product-table">
        <el-table-column label="商品信息" min-width="280">
          <template #default="{ row }">
            <div class="product-cell">
              <el-image
                :src="row.mainImage"
                fit="cover"
                class="product-thumb"
                :preview-src-list="[row.mainImage]"
                :hide-on-click-modal="true"
              >
                <template #error>
                  <div class="img-placeholder"><el-icon :size="24"><Picture /></el-icon></div>
                </template>
              </el-image>
              <div class="product-info">
                <span class="product-name">{{ row.name }}</span>
                <span class="product-meta">{{ row.brand || '无品牌' }} · {{ row.categoryName || '未分类' }}</span>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="price" label="价格" width="100" sortable>
          <template #default="{ row }">
            <span class="price-tag">¥{{ row.price }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="stock" label="库存" width="80" sortable>
          <template #default="{ row }">
            <span :class="row.stock < 10 ? 'text-danger' : ''">{{ row.stock }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="sales" label="销量" width="80" sortable />
        <el-table-column prop="status" label="状态" width="90">
          <template #default="{ row }">
            <el-switch
              :model-value="row.status === 1"
              @change="(val) => handleToggleStatus(row, val)"
              active-text="上架"
              inactive-text="下架"
              inline-prompt
            />
          </template>
        </el-table-column>
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="handleEdit(row)">编辑</el-button>
            <el-button link type="danger" size="small" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="table-footer">
        <el-pagination
          v-model:current-page="page" v-model:page-size="size"
          :page-sizes="[10, 20, 50]"
          :total="total" layout="total, sizes, prev, pager, next"
          @size-change="loadData" @current-change="loadData"
        />
      </div>
    </div>

    <!-- 新增/编辑对话框 -->
    <el-dialog
      v-model="dialogVisible" :title="isEdit ? '编辑商品' : '新增商品'"
      :width="isMobile ? '92%' : '640px'" :close-on-click-modal="false" @closed="resetForm"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="商品名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入商品名称" />
        </el-form-item>
        <el-form-item label="品牌" prop="brand">
          <el-input v-model="form.brand" placeholder="如：宜家、全友" />
        </el-form-item>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="分类" prop="categoryId">
              <el-select v-model="form.categoryId" placeholder="选择分类" style="width:100%">
                <el-option
                  v-for="cat in categories" :key="cat.id"
                  :label="cat.name" :value="cat.id"
                />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="价格" prop="price">
              <el-input-number v-model="form.price" :min="0" :precision="2" :step="100" style="width:100%" placeholder="商品价格" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="库存" prop="stock">
              <el-input-number v-model="form.stock" :min="0" :step="1" style="width:100%" placeholder="库存数量" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="状态">
              <el-switch v-model="form.statusBool" active-text="上架" inactive-text="下架" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="主图" prop="mainImage">
          <div class="upload-area">
            <el-upload
              :show-file-list="false" :http-request="handleUpload"
              accept="image/*" class="upload-trigger"
            >
              <img v-if="form.mainImage" :src="form.mainImage" class="upload-preview" />
              <div v-else class="upload-placeholder">
                <el-icon :size="32"><Plus /></el-icon>
                <span>上传图片</span>
              </div>
            </el-upload>
            <el-input v-model="form.mainImage" placeholder="或输入图片URL" class="url-input" />
          </div>
        </el-form-item>
        <el-form-item label="描述" prop="description">
          <el-input v-model="form.description" type="textarea" :rows="3" placeholder="商品描述" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSave" :loading="saving">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { adminAPI, categoryAPI } from '../../api/modules'
import { fileAPI } from '../../api/modules/file'
import { Search, Plus, Picture } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useBreakpoint } from '../../composables/useBreakpoint'

// 弹窗宽度需要随视口变化，故用断点状态而不是写死 640px
const { isMobile } = useBreakpoint()

const products = ref([])
const total = ref(0)
const page = ref(1)
const size = ref(10)
const loading = ref(false)
const searchKeyword = ref('')
const filterStatus = ref(null)

const categories = ref([])
const dialogVisible = ref(false)
const isEdit = ref(false)
const saving = ref(false)
const formRef = ref(null)

const defaultForm = { name: '', brand: '', categoryId: null, price: 0, stock: 0, statusBool: true, mainImage: '', description: '' }
const form = reactive({ ...defaultForm })
const rules = {
  name: [{ required: true, message: '请输入商品名称', trigger: 'blur' }],
  price: [{ required: true, message: '请输入价格', trigger: 'blur' }],
  categoryId: [{ required: true, message: '请选择分类', trigger: 'change' }]
}

const loadData = async () => {
  loading.value = true
  try {
    const params = { page: page.value, size: size.value }
    if (searchKeyword.value) params.name = searchKeyword.value
    if (filterStatus.value !== null) params.status = filterStatus.value
    const res = await adminAPI.products.getList(params)
    products.value = res.data.data.records || []
    total.value = res.data.data.total || 0
  } finally { loading.value = false }
}

const loadCategories = async () => {
  try {
    const res = await categoryAPI.getList()
    categories.value = res.data.data || []
  } catch (e) { /* ignore */ }
}

const handleSearch = () => { page.value = 1; loadData() }

const handleAdd = () => {
  isEdit.value = false
  Object.assign(form, { ...defaultForm })
  dialogVisible.value = true
}

const handleEdit = (row) => {
  isEdit.value = true
  Object.assign(form, {
    ...row,
    statusBool: row.status === 1,
    categoryId: row.categoryId ? Number(row.categoryId) : null
  })
  dialogVisible.value = true
}

const handleSave = async () => {
  if (!formRef.value) return
  try {
    await formRef.value.validate()
    saving.value = true
    const data = { ...form, status: form.statusBool ? 1 : 0 }
    delete data.statusBool
    if (isEdit.value) {
      await adminAPI.products.update(form.id, data)
      ElMessage.success('更新成功')
    } else {
      await adminAPI.products.create(data)
      ElMessage.success('添加成功')
    }
    dialogVisible.value = false
    loadData()
  } catch (e) { if (e?.errorFields) return } finally { saving.value = false }
}

const handleDelete = async (row) => {
  await ElMessageBox.confirm(`确定删除商品「${row.name}」吗？此操作不可恢复。`, '删除确认', { type: 'warning' })
  await adminAPI.products.delete(row.id)
  ElMessage.success('删除成功')
  loadData()
}

const handleToggleStatus = async (row, val) => {
  await adminAPI.products.updateStatus(row.id, val)
  ElMessage.success(val ? '已上架' : '已下架')
  loadData()
}

const handleUpload = async (options) => {
  try {
    const res = await fileAPI.upload(options.file)
    if (res.data?.data) {
      form.mainImage = res.data.data
      ElMessage.success('上传成功')
    }
  } catch (e) { ElMessage.error('上传失败') }
}

const resetForm = () => {
  formRef.value?.resetFields()
  Object.assign(form, { ...defaultForm })
}

onMounted(() => {
  loadData()
  loadCategories()
})
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
.product-cell { display: flex; align-items: center; gap: 12px; }
.product-thumb { width: 56px; height: 56px; border-radius: 8px; flex-shrink: 0; }
.img-placeholder {
  width: 56px; height: 56px; border-radius: 8px; background: #f7fafc;
  display: flex; align-items: center; justify-content: center; color: #cbd5e0;
}
.product-info { display: flex; flex-direction: column; gap: 4px; }
.product-name { font-size: 14px; font-weight: 500; color: #2d3748; }
.product-meta { font-size: 12px; color: #a0aec0; }
.price-tag { color: #e53e3e; font-weight: 600; }
.text-danger { color: #e53e3e; font-weight: 600; }
.table-footer { display: flex; justify-content: flex-end; margin-top: 16px; }

.upload-area { display: flex; gap: 12px; align-items: flex-start; width: 100%; }
.upload-trigger { flex-shrink: 0; }
.upload-preview { width: 80px; height: 80px; border-radius: 8px; object-fit: cover; border: 1px solid #edf2f7; }
.upload-placeholder {
  width: 80px; height: 80px; border-radius: 8px; border: 1px dashed #cbd5e0;
  display: flex; flex-direction: column; align-items: center; justify-content: center;
  gap: 4px; color: #a0aec0; font-size: 12px; cursor: pointer; transition: all 0.2s;
}
.upload-placeholder:hover { border-color: #667eea; color: #667eea; }
.url-input { flex: 1; }

/* ===== 响应式：断点取值见 composables/useBreakpoint.js ===== */

/* 平板及以下：卡片内边距收窄，把宽度还给表格本身 */
@media (max-width: 1024px) {
  .table-card {
    padding: 12px;
  }
}

/* 移动端：工具栏改为竖向堆叠（搜索框占满整行），分页居中并允许换行。
   表格列宽合计 750px，窄屏下由 el-table 自身横向滚动兜底，不做压缩。 */
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
