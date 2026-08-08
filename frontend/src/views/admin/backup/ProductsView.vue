<template>
  <div class="admin-products">
    <div class="page-header">
      <h2>商品管理</h2>
      <el-button type="primary" @click="handleAdd">添加商品</el-button>
    </div>

    <!-- 搜索和筛选 -->
    <div class="filter-bar">
      <el-input v-model="searchQuery" placeholder="搜索商品名称" style="width: 300px; margin-right: 10px">
        <template #append>
          <el-button @click="handleSearch"><el-icon><Search /></el-icon></el-button>
        </template>
      </el-input>
      <el-select v-model="statusFilter" placeholder="选择状态" style="width: 120px; margin-right: 10px">
        <el-option label="全部" value=""></el-option>
        <el-option label="上架" value="1"></el-option>
        <el-option label="下架" value="0"></el-option>
      </el-select>
      <el-button @click="resetFilter">重置</el-button>
    </div>

    <!-- 商品列表 -->
    <el-table :data="products" style="width: 100%">
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column label="商品图片" width="100">
        <template #default="scope">
          <div>
            <img :src="scope.row.mainImage || 'https://picsum.photos/200/200'" :alt="scope.row.name" class="product-image">
          </div>
        </template>
      </el-table-column>
      <el-table-column prop="name" label="商品名称" />
      <el-table-column prop="price" label="价格" width="100">
        <template #default="scope">
          ¥{{ scope.row.price }}
        </template>
      </el-table-column>
      <el-table-column prop="stock" label="库存" width="100" />
      <el-table-column prop="categoryName" label="分类" width="150" />
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
      <el-table-column label="操作" width="150">
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

    <!-- 添加/编辑商品对话框 -->
    <el-dialog v-model="dialogVisible" :title="dialogTitle">
      <el-form :model="productForm" :rules="productRules" ref="productFormRef" label-width="100px">
        <el-form-item label="商品名称" prop="name">
          <el-input v-model="productForm.name" />
        </el-form-item>
        <el-form-item label="商品分类" prop="categoryId">
          <el-select v-model="productForm.categoryId" placeholder="选择分类">
            <el-option
              v-for="category in categories"
              :key="category.id"
              :label="category.name"
              :value="category.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="商品价格" prop="price">
          <el-input v-model.number="productForm.price" type="number" />
        </el-form-item>
        <el-form-item label="商品库存" prop="stock">
          <el-input v-model.number="productForm.stock" type="number" />
        </el-form-item>
        <el-form-item label="商品图片" prop="mainImage">
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
          <div v-if="productForm.mainImage" style="margin-top: 10px">
            <img :src="productForm.mainImage" :alt="productForm.name" style="width: 100px; height: 100px; object-fit: cover; border-radius: 4px">
          </div>
        </el-form-item>
        <el-form-item label="商品描述" prop="description">
          <el-input v-model="productForm.description" type="textarea" />
        </el-form-item>
        <el-form-item label="商品状态" prop="status">
          <el-switch v-model="productForm.status" />
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
</template>

<script setup>
import { ref, onMounted, reactive } from 'vue'
import api, { adminAPI, fileAPI } from '../../api'
import { Search, Edit, Delete } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'

const products = ref([])
const total = ref(0)
const currentPage = ref(1)
const pageSize = ref(10)
const searchQuery = ref('')
const statusFilter = ref('')
const categories = ref([])
const updatingStatus = ref(new Set())

const dialogVisible = ref(false)
const dialogTitle = ref('添加商品')
const productFormRef = ref(null)
const fileList = ref([])

const productForm = reactive({
  id: null,
  name: '',
  categoryId: '',
  price: 0,
  stock: 0,
  mainImage: '',
  description: '',
  status: true
})

const productRules = {
  name: [{ required: true, message: '请输入商品名称', trigger: 'blur' }],
  categoryId: [{ required: true, message: '请选择商品分类', trigger: 'blur' }],
  price: [{ required: true, message: '请输入商品价格', trigger: 'blur' }],
  stock: [{ required: true, message: '请输入商品库存', trigger: 'blur' }],
  mainImage: [{ required: true, message: '请输入商品图片', trigger: 'blur' }]
}

onMounted(async () => {
  await loadCategories()
  await loadProducts()
})

// 扁平化分类树结构
const flattenCategories = (categories) => {
  let result = []
  const traverse = (category) => {
    result.push(category)
    if (category.children && category.children.length > 0) {
      category.children.forEach(child => traverse(child))
    }
  }
  categories.forEach(category => traverse(category))
  return result
}

const loadCategories = async () => {
  try {
    const response = await adminAPI.categories.getTree()
    // 将树形结构扁平化，以便在下拉选择框中显示所有分类
    categories.value = flattenCategories(response.data.data)
  } catch (error) {
    console.error('获取分类失败:', error)
  }
}

const loadProducts = async () => {
  try {
    const params = {
      page: currentPage.value,
      size: pageSize.value
    }
    if (searchQuery.value) {
      params.name = searchQuery.value
    }
    if (statusFilter.value) {
      params.status = statusFilter.value
    }
    const response = await adminAPI.products.getList(params)
    console.log('商品列表响应:', response)
    console.log('商品列表数据:', response.data.data.records)
    products.value = response.data.data.records
    total.value = response.data.data.total
  } catch (error) {
    console.error('获取商品失败:', error)
  }
}

const handleSearch = () => {
  currentPage.value = 1
  loadProducts()
}

const resetFilter = () => {
  searchQuery.value = ''
  statusFilter.value = ''
  currentPage.value = 1
  loadProducts()
}

const handleSizeChange = (size) => {
  pageSize.value = size
  loadProducts()
}

const handleCurrentChange = (page) => {
  currentPage.value = page
  loadProducts()
}

const handleAdd = async () => {
  dialogTitle.value = '添加商品'
  Object.assign(productForm, {
    id: null,
    name: '',
    categoryId: '',
    price: 0,
    stock: 0,
    mainImage: '',
    description: '',
    status: true
  })
  fileList.value = []
  // 重新加载分类列表，确保能看到新添加的分类
  await loadCategories()
  dialogVisible.value = true
}

const handleFileChange = (file) => {
  fileList.value = [file]
}

const handleUpload = async (options) => {
  const file = options.file
  console.log('上传的文件信息:', file)
  console.log('文件名称:', file.name)
  console.log('文件大小:', file.size)
  console.log('文件类型:', file.type)
  console.log('file.raw:', file.raw)
  try {
    const actualFile = file.raw || file
    console.log('实际上传的文件:', actualFile)
    const response = await fileAPI.upload(actualFile)
    console.log('上传响应:', response)
    if (response.data && response.data.data) {
      productForm.mainImage = response.data.data
      ElMessage.success('图片上传成功')
      options.onSuccess()
    } else {
      ElMessage.error('上传失败：无效的响应')
      options.onError()
    }
  } catch (error) {
    console.error('上传失败:', error)
    console.error('错误类型:', typeof error)
    console.error('错误消息:', error.message)
    console.error('错误响应:', error.response)
    const errorMsg = error.response?.data?.msg || error.message || '图片上传失败'
    ElMessage.error(errorMsg)
    options.onError()
  }
}

const handleEdit = async (row) => {
  dialogTitle.value = '编辑商品'
  Object.assign(productForm, row)
  // 重新加载分类列表，确保能看到新添加的分类
  await loadCategories()
  dialogVisible.value = true
}

const handleSave = async () => {
  if (!productFormRef.value) return

  try {
    await productFormRef.value.validate()

    // 转换状态为整数
    const productData = {
      ...productForm,
      status: productForm.status ? 1 : 0
    }

    if (productForm.id) {
      await adminAPI.products.update(productForm.id, productData)
      ElMessage.success('更新成功')
    } else {
      await adminAPI.products.create(productData)
      ElMessage.success('添加成功')
    }

    dialogVisible.value = false
    loadProducts()
  } catch (error) {
    ElMessage.error(error.message || '操作失败')
  }
}

const handleDelete = async (id) => {
  try {
    await ElMessageBox.confirm('确定要删除这个商品吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })

    await adminAPI.products.delete(id)
    ElMessage.success('删除成功')
    loadProducts()
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
    await adminAPI.products.updateStatus(id, status ? 1 : 0)
    ElMessage.success('状态更新成功')
  } catch (error) {
    ElMessage.error(error.message || '状态更新失败')
    loadProducts()
  } finally {
    updatingStatus.value.delete(id)
  }
}
</script>

<style scoped>
.admin-products {
  background: #fff;
  padding: 20px;
  border-radius: 8px;
  box-shadow: 0 2px 12px rgba(0,0,0,0.1);
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

.product-image {
  width: 50px;
  height: 50px;
  object-fit: cover;
  border-radius: 4px;
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