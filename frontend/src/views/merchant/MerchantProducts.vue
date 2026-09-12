<template>
  <div>
    <div class="page-tools">
      <div class="summary-card">
        <span>全部商品</span>
        <strong>{{ total }}</strong>
      </div>
      <div class="summary-card">
        <span>当前页在售</span>
        <strong>{{ products.filter(item => item.status === 1).length }}</strong>
      </div>
      <div class="summary-card">
        <span>库存预警</span>
        <strong>{{ products.filter(item => Number(item.stock) <= 5).length }}</strong>
      </div>
    </div>

    <section class="seller-panel">
      <div class="panel-toolbar">
        <el-tabs v-model="statusFilter" @tab-change="loadProducts">
          <el-tab-pane label="全部宝贝" name="all" />
          <el-tab-pane label="出售中" name="1" />
          <el-tab-pane label="仓库中" name="0" />
        </el-tabs>
        <div class="search-actions">
          <el-input v-model="keyword" clearable placeholder="搜索商品名称/品牌" @keyup.enter="loadProducts" />
          <el-button @click="loadProducts">搜索</el-button>
          <el-button type="primary" @click="handleAdd">发布商品</el-button>
        </div>
      </div>

      <el-table :data="filteredProducts" v-loading="loading" row-key="id">
        <el-table-column label="商品" min-width="320">
          <template #default="{ row }">
            <div class="goods-cell">
              <img :src="row.mainImage || fallbackImage" :alt="row.name">
              <div>
                <strong>{{ row.name }}</strong>
                <span>{{ row.brand || '未填写品牌' }}</span>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="price" label="价格" width="120">
          <template #default="{ row }">¥{{ money(row.price) }}</template>
        </el-table-column>
        <el-table-column prop="stock" label="库存" width="100" />
        <el-table-column prop="sales" label="销量" width="100" />
        <el-table-column label="状态" width="110">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'">{{ row.status === 1 ? '出售中' : '仓库中' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="190" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="handleEdit(row)">编辑</el-button>
            <el-button link @click="copyProduct(row)">复制链接</el-button>
            <el-button link type="danger" @click="handleDelete(row.id)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-empty v-if="!loading && !filteredProducts.length" description="暂无商品" />

      <el-pagination
        v-if="total > 0"
        v-model:current-page="currentPage"
        :page-size="pageSize"
        :total="total"
        layout="total, prev, pager, next"
        @current-change="loadProducts"
      />
    </section>

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="720px">
      <el-form :model="productForm" :rules="productRules" ref="productFormRef" label-width="96px">
        <el-form-item label="商品名称" prop="name">
          <el-input v-model="productForm.name" maxlength="60" show-word-limit />
        </el-form-item>
        <el-form-item label="商品分类" prop="categoryId">
          <el-select v-model="productForm.categoryId" placeholder="请选择分类" filterable>
            <el-option v-for="category in flatCategories" :key="category.id" :label="category.name" :value="category.id" />
          </el-select>
        </el-form-item>
        <div class="form-grid">
          <el-form-item label="品牌" prop="brand">
            <el-input v-model="productForm.brand" />
          </el-form-item>
          <el-form-item label="价格" prop="price">
            <el-input-number v-model="productForm.price" :min="0" :precision="2" />
          </el-form-item>
          <el-form-item label="库存" prop="stock">
            <el-input-number v-model="productForm.stock" :min="0" />
          </el-form-item>
        </div>
        <el-form-item label="商品主图" prop="mainImage">
          <div class="upload-row">
            <el-upload action="" :http-request="handleImageUpload" :auto-upload="true" accept="image/*" :show-file-list="false">
              <el-button type="primary">上传主图</el-button>
            </el-upload>
            <el-input v-model="productForm.mainImage" placeholder="或输入图片 URL" />
          </div>
          <img v-if="productForm.mainImage" :src="productForm.mainImage" :alt="productForm.name" class="preview-image">
        </el-form-item>
        <el-form-item label="商品描述" prop="description">
          <el-input v-model="productForm.description" type="textarea" :rows="5" maxlength="500" show-word-limit />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSave">保存并上架</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { fileAPI, merchantAPI } from '../../api'
import { useUserStore } from '../../stores/user'

const router = useRouter()
const userStore = useUserStore()

/** 加载态：无此标记时，数据到达前表格为空会先闪一下"暂无商品" */
const loading = ref(false)
const products = ref([])
const categories = ref([])
const dialogVisible = ref(false)
const dialogTitle = ref('发布商品')
const productFormRef = ref(null)
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)
const keyword = ref('')
const statusFilter = ref('all')
const fallbackImage = 'https://images.unsplash.com/photo-1555041469-a586c61ea9bc?auto=format&fit=crop&w=300&q=80'

const productForm = ref(emptyProduct())

const productRules = {
  name: [{ required: true, message: '请输入商品名称', trigger: 'blur' }],
  categoryId: [{ required: true, message: '请选择商品分类', trigger: 'change' }],
  brand: [{ required: true, message: '请输入品牌', trigger: 'blur' }],
  price: [{ required: true, message: '请输入价格', trigger: 'blur' }],
  stock: [{ required: true, message: '请输入库存', trigger: 'blur' }],
  mainImage: [{ required: true, message: '请上传或填写商品主图', trigger: 'blur' }]
}

const flatCategories = computed(() => {
  const records = Array.isArray(categories.value) ? categories.value : (categories.value?.records || [])
  return records.map(cat => ({ id: cat.id, name: cat.parentName ? `${cat.parentName} / ${cat.name}` : cat.name }))
})

const filteredProducts = computed(() => products.value.filter(product => {
  const matchesStatus = statusFilter.value === 'all' || product.status === Number(statusFilter.value)
  const text = `${product.name} ${product.brand || ''}`.toLowerCase()
  const matchesKeyword = !keyword.value || text.includes(keyword.value.toLowerCase())
  return matchesStatus && matchesKeyword
}))

onMounted(async () => {
  if (!userStore.isAuthenticated) {
    router.push('/login')
    return
  }
  await Promise.all([loadProducts(), loadCategories()])
})

function emptyProduct() {
  return { id: null, name: '', categoryId: null, brand: '', price: 0, stock: 0, mainImage: '', description: '', status: 1 }
}

const loadProducts = async () => {
  loading.value = true
  try {
    const response = await merchantAPI.products.getList({ page: currentPage.value, size: pageSize.value })
    products.value = response.data.data.records || []
    total.value = response.data.data.total || 0
  } finally {
    loading.value = false
  }
}

const loadCategories = async () => {
  const response = await merchantAPI.categories.getList({ page: 1, size: 100 })
  categories.value = response.data.data || []
}

const handleAdd = () => {
  dialogTitle.value = '发布商品'
  productForm.value = emptyProduct()
  dialogVisible.value = true
}

const handleEdit = product => {
  dialogTitle.value = '编辑商品'
  productForm.value = { ...emptyProduct(), ...product }
  dialogVisible.value = true
}

const handleSave = async () => {
  await productFormRef.value.validate()
  if (productForm.value.id) {
    await merchantAPI.products.update(productForm.value.id, productForm.value)
    ElMessage.success('商品已更新')
  } else {
    await merchantAPI.products.create(productForm.value)
    ElMessage.success('商品已发布')
  }
  dialogVisible.value = false
  await loadProducts()
}

const handleImageUpload = async options => {
  try {
    const response = await fileAPI.upload(options.file.raw || options.file)
    productForm.value.mainImage = response.data.data
    ElMessage.success('图片上传成功')
    options.onSuccess()
  } catch (error) {
    ElMessage.error(error.response?.data?.msg || '图片上传失败')
    options.onError()
  }
}

const handleDelete = async id => {
  await ElMessageBox.confirm('删除后商品将无法恢复，确定删除吗？', '删除商品', { type: 'warning' })
  await merchantAPI.products.delete(id)
  ElMessage.success('商品已删除')
  await loadProducts()
}

const copyProduct = async product => {
  await navigator.clipboard.writeText(`${location.origin}/product/${product.id}`)
  ElMessage.success('商品链接已复制')
}

const money = value => Number(value || 0).toFixed(2)
</script>

<style scoped>
.page-tools {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 14px;
  margin-bottom: 16px;
}

.summary-card,
.seller-panel {
  background: #fff;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
}

.summary-card {
  display: grid;
  gap: 8px;
  padding: 18px;
}

.summary-card strong {
  font-size: 28px;
}

.seller-panel {
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

.goods-cell {
  display: flex;
  align-items: center;
  gap: 12px;
}

.goods-cell img,
.preview-image {
  width: 72px;
  height: 72px;
  object-fit: cover;
  border-radius: 6px;
}

.goods-cell strong,
.goods-cell span {
  display: block;
}

.goods-cell span {
  color: #909399;
  margin-top: 6px;
}

.form-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
}

.upload-row {
  width: 100%;
  display: grid;
  grid-template-columns: auto 1fr;
  gap: 10px;
}

.preview-image {
  margin-top: 12px;
}

@media (max-width: 900px) {
  .page-tools,
  .panel-toolbar,
  .form-grid,
  .upload-row {
    grid-template-columns: 1fr;
    display: grid;
  }

  .search-actions {
    flex-wrap: wrap;
  }
}
</style>
