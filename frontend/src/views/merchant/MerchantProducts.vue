<template>
  <div class="merchant-products">
    <!-- 导航栏 -->
    <header class="navbar">
      <div class="container">
        <div class="logo">
          <router-link to="/merchant">家具商城-商家管理</router-link>
        </div>
        <nav class="nav">
          <router-link to="/merchant" class="nav-item">首页</router-link>
          <router-link to="/merchant/products" class="nav-item">商品管理</router-link>
          <router-link to="/merchant/categories" class="nav-item">商品分类</router-link>
          <router-link to="/merchant/orders" class="nav-item">订单管理</router-link>
        </nav>
        <div class="user">
          <template v-if="userStore.isAuthenticated">
            <span class="welcome">欢迎, {{ userStore.user.username }}</span>
            <button @click="logout" class="btn">退出</button>
          </template>
        </div>
      </div>
    </header>

    <!-- 内容区域 -->
    <div class="content">
      <div class="container">
        <div class="page-header">
          <h1 class="page-title">商品管理</h1>
          <el-button type="primary" @click="handleAdd">添加商品</el-button>
        </div>

        <!-- 商品列表 -->
        <div class="product-list" v-if="products.length > 0">
          <div v-for="product in products" :key="product.id" class="product-item">
            <div class="product-image">
              <img :src="product.mainImage || '/placeholder.png'" :alt="product.name" />
            </div>
            <div class="product-info">
              <div class="product-name">{{ product.name }}</div>
              <div class="product-brand">{{ product.brand }}</div>
              <div class="product-price">¥{{ product.price }}</div>
              <div class="product-stock">库存: {{ product.stock }}</div>
              <div class="product-status">
                <el-tag :type="product.status === 1 ? 'success' : 'info'">
                  {{ product.status === 1 ? '上架' : '下架' }}
                </el-tag>
              </div>
            </div>
            <div class="product-actions">
              <el-button @click="handleEdit(product)">编辑</el-button>
              <el-button type="danger" @click="handleDelete(product.id)">删除</el-button>
            </div>
          </div>
        </div>

        <el-empty v-else description="暂无商品" />

        <!-- 分页 -->
        <el-pagination
          v-if="total > 0"
          v-model:current-page="currentPage"
          :page-size="pageSize"
          :total="total"
          layout="prev, pager, next"
          @current-change="loadProducts"
          class="pagination"
        />
      </div>
    </div>

    <!-- 添加/编辑商品对话框 -->
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="600px">
      <el-form :model="productForm" :rules="productRules" ref="productFormRef" label-width="100px">
        <el-form-item label="商品名称" prop="name">
          <el-input v-model="productForm.name" />
        </el-form-item>
        <el-form-item label="商品分类" prop="categoryId">
          <el-select v-model="productForm.categoryId" placeholder="请选择分类">
            <el-option
              v-for="category in flatCategories"
              :key="category.id"
              :label="category.name"
              :value="category.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="品牌" prop="brand">
          <el-input v-model="productForm.brand" />
        </el-form-item>
        <el-form-item label="价格" prop="price">
          <el-input-number v-model="productForm.price" :min="0" :precision="2" />
        </el-form-item>
        <el-form-item label="库存" prop="stock">
          <el-input-number v-model="productForm.stock" :min="0" />
        </el-form-item>
        <el-form-item label="主图" prop="mainImage">
          <el-upload
            class="upload-demo"
            action=""
            :http-request="handleImageUpload"
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
          <el-input v-model="productForm.mainImage" placeholder="或输入图片URL" style="margin-top: 10px;" />
        </el-form-item>
        <el-form-item label="商品描述" prop="description">
          <el-input v-model="productForm.description" type="textarea" rows="4" />
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" @click="handleSave">保存</el-button>
        </span>
      </template>
    </el-dialog>

    <!-- 页脚 -->
    <footer class="footer">
      <div class="container">
        <p>&copy; 2026 家具商城. 保留所有权利.</p>
      </div>
    </footer>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '../../stores/user'
import { merchantAPI, fileAPI } from '../../api'
import { ElMessage, ElMessageBox } from 'element-plus'

const router = useRouter()
const userStore = useUserStore()

const products = ref([])
const categories = ref([])
const dialogVisible = ref(false)
const dialogTitle = ref('添加商品')
const productFormRef = ref(null)
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)

const productForm = ref({
  id: null,
  name: '',
  categoryId: null,
  brand: '',
  price: 0,
  stock: 0,
  mainImage: '',
  description: ''
})

const productRules = {
  name: [{ required: true, message: '请输入商品名称', trigger: 'blur' }],
  categoryId: [{ required: true, message: '请选择商品分类', trigger: 'change' }],
  brand: [{ required: true, message: '请输入品牌', trigger: 'blur' }],
  price: [{ required: true, message: '请输入价格', trigger: 'blur' }],
  stock: [{ required: true, message: '请输入库存', trigger: 'blur' }]
}

const flatCategories = computed(() => {
  // 分类数据现在是分页格式，直接从 records 中获取
  const records = Array.isArray(categories.value) ? categories.value : (categories.value?.records || [])
  return records.map(cat => ({
    id: cat.id,
    name: cat.name
  }))
})

onMounted(async () => {
  if (!userStore.isAuthenticated) {
    router.push('/login')
    return
  }
  await Promise.all([loadProducts(), loadCategories()])
})

const loadProducts = async () => {
  try {
    const response = await merchantAPI.products.getList({
      page: currentPage.value,
      size: pageSize.value
    })
    products.value = response.data.data.records || []
    total.value = response.data.data.total || 0
  } catch (error) {
    console.error('获取商品列表失败:', error)
    ElMessage.error('获取商品列表失败')
  }
}

const loadCategories = async () => {
  try {
    const response = await merchantAPI.categories.getList()
    categories.value = response.data.data || []
  } catch (error) {
    console.error('获取分类失败:', error)
    ElMessage.error('获取分类失败')
  }
}

const handleAdd = () => {
  dialogTitle.value = '添加商品'
  productForm.value = {
    id: null,
    name: '',
    categoryId: null,
    brand: '',
    price: 0,
    stock: 0,
    mainImage: '',
    description: ''
  }
  dialogVisible.value = true
}

const handleEdit = (product) => {
  dialogTitle.value = '编辑商品'
  productForm.value = {
    id: product.id,
    name: product.name,
    categoryId: product.categoryId,
    brand: product.brand,
    price: product.price,
    stock: product.stock,
    mainImage: product.mainImage,
    description: product.description
  }
  dialogVisible.value = true
}

const handleSave = async () => {
  if (!productFormRef.value) return

  try {
    await productFormRef.value.validate()

    if (productForm.value.id) {
      await merchantAPI.products.update(productForm.value.id, productForm.value)
      ElMessage.success('更新成功')
    } else {
      await merchantAPI.products.create(productForm.value)
      ElMessage.success('添加成功')
    }

    dialogVisible.value = false
    await loadProducts()
  } catch (error) {
    ElMessage.error(error.response?.data?.msg || error.message || '操作失败')
  }
}

const handleImageUpload = async (options) => {
  const file = options.file
  try {
    const actualFile = file.raw || file
    const response = await fileAPI.upload(actualFile)
    if (response.data && response.data.data) {
      productForm.value.mainImage = response.data.data
      ElMessage.success('图片上传成功')
      options.onSuccess()
    } else {
      ElMessage.error('上传失败：无效的响应')
      options.onError()
    }
  } catch (error) {
    const errorMsg = error.response?.data?.msg || error.message || '图片上传失败'
    ElMessage.error(errorMsg)
    options.onError()
  }
}

const handleDelete = async (id) => {
  try {
    await ElMessageBox.confirm('确定要删除这个商品吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })

    await merchantAPI.products.delete(id)
    ElMessage.success('删除成功')
    await loadProducts()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(error.message || '删除失败')
    }
  }
}

const logout = () => {
  localStorage.removeItem('furniture_token')
  localStorage.removeItem('furniture_user')
  router.push('/login')
}
</script>

<style scoped>
.merchant-products {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
}

.navbar {
  background: #fff;
  box-shadow: 0 2px 8px rgba(0,0,0,0.1);
  padding: 0;
  position: sticky;
  top: 0;
  z-index: 100;
}

.navbar .container {
  display: flex;
  align-items: center;
  justify-content: space-between;
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 20px;
  height: 60px;
}

.logo a {
  font-size: 20px;
  font-weight: bold;
  color: #409eff;
  text-decoration: none;
}

.nav {
  display: flex;
  gap: 20px;
}

.nav-item {
  color: #666;
  text-decoration: none;
  padding: 8px 16px;
  border-radius: 4px;
  transition: all 0.3s;
}

.nav-item:hover,
.nav-item.router-link-active {
  background: #409eff;
  color: #fff;
}

.user {
  display: flex;
  align-items: center;
  gap: 10px;
}

.welcome {
  color: #666;
}

.btn {
  padding: 8px 16px;
  border: 1px solid #409eff;
  background: #fff;
  color: #409eff;
  border-radius: 4px;
  cursor: pointer;
  transition: all 0.3s;
}

.btn:hover {
  background: #409eff;
  color: #fff;
}

.content {
  flex: 1;
  padding: 40px 0;
  background: #f5f5f5;
}

.container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 20px;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 30px;
}

.page-title {
  font-size: 28px;
  color: #333;
}

.product-list {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 20px;
  margin-bottom: 20px;
}

.product-item {
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.1);
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

.product-image {
  width: 100%;
  height: 200px;
  overflow: hidden;
  background: #f5f5f5;
}

.product-image img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.product-info {
  padding: 15px;
  flex: 1;
}

.product-name {
  font-size: 16px;
  font-weight: bold;
  margin-bottom: 8px;
  color: #333;
}

.product-brand,
.product-price,
.product-stock {
  font-size: 14px;
  color: #666;
  margin-bottom: 4px;
}

.product-price {
  color: #f56c6c;
  font-size: 18px;
  font-weight: bold;
}

.product-status {
  margin-top: 8px;
}

.product-actions {
  padding: 15px;
  display: flex;
  gap: 10px;
  border-top: 1px solid #eee;
}

.pagination {
  margin-top: 20px;
  display: flex;
  justify-content: center;
}

.footer {
  background: #fff;
  padding: 20px 0;
  text-align: center;
  color: #666;
  margin-top: auto;
}
</style>