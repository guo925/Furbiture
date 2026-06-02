<template>
  <UserLayout>
    <section class="shop-container products-page">
      <div class="crumb">首页 / 全部商品</div>

      <div class="filter-board">
        <div class="filter-row">
          <span>分类</span>
          <button :class="{ active: !selectedCategory }" type="button" @click="setCategory('')">全部</button>
          <button
            v-for="category in categories"
            :key="category.id"
            :class="{ active: String(selectedCategory) === String(category.id) }"
            type="button"
            @click="setCategory(category.id)"
          >
            {{ category.name }}
          </button>
        </div>
        <div class="filter-row tools">
          <span>排序</span>
          <button v-for="item in sortOptions" :key="item.value" :class="{ active: sortBy === item.value }" type="button" @click="setSort(item.value)">
            {{ item.label }}
          </button>
          <el-input-number v-model="minPrice" :min="0" :controls="false" placeholder="最低价" />
          <span class="dash">-</span>
          <el-input-number v-model="maxPrice" :min="0" :controls="false" placeholder="最高价" />
          <el-button @click="applyPrice">确定</el-button>
        </div>
      </div>

      <div class="list-head">
        <div>
          <h1>{{ pageTitle }}</h1>
          <p>共 {{ total }} 件商品，支持关键词、分类、价格区间筛选。</p>
        </div>
        <el-select v-model="pageSize" @change="handleSizeChange">
          <el-option :value="12" label="每页 12 件" />
          <el-option :value="24" label="每页 24 件" />
          <el-option :value="36" label="每页 36 件" />
        </el-select>
      </div>

      <el-skeleton v-if="loading" :rows="10" animated />
      <div v-else-if="filteredProducts.length" class="product-grid">
        <ProductCard
          v-for="product in filteredProducts"
          :key="product.id"
          :product="product"
          @open="goToDetail"
          @cart="addToCart"
        />
      </div>
      <el-empty v-else description="没有找到匹配商品">
        <el-button color="#ff5000" @click="resetFilters">清空筛选</el-button>
      </el-empty>

      <div v-if="!loading && total > 0" class="pagination">
        <el-pagination
          v-model:current-page="currentPage"
          :page-size="pageSize"
          layout="prev, pager, next, jumper"
          :total="total"
          @current-change="handleCurrentChange"
        />
      </div>
    </section>
  </UserLayout>
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import UserLayout from '../../components/UserLayout.vue'
import ProductCard from '../../components/ProductCard.vue'
import { productAPI } from '../../api'
import { useUserStore } from '../../stores/user'
import { useCartStore } from '../../stores/cart'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()
const cartStore = useCartStore()

const searchQuery = ref(route.query.keyword || '')
const selectedCategory = ref(route.query.categoryId || '')
const sortBy = ref(route.query.sortBy || '')
const currentPage = ref(Number(route.query.page || 1))
const pageSize = ref(12)
const products = ref([])
const total = ref(0)
const loading = ref(false)
const categories = ref([])
const minPrice = ref(route.query.minPrice ? Number(route.query.minPrice) : undefined)
const maxPrice = ref(route.query.maxPrice ? Number(route.query.maxPrice) : undefined)

const sortOptions = [
  { label: '综合', value: '' },
  { label: '销量优先', value: 'sales_desc' },
  { label: '价格从低到高', value: 'price_asc' },
  { label: '价格从高到低', value: 'price_desc' }
]

const pageTitle = computed(() => searchQuery.value ? `搜索“${searchQuery.value}”` : '全部商品')

const filteredProducts = computed(() => {
  return products.value.filter(product => {
    const price = Number(product.price || 0)
    if (minPrice.value !== undefined && minPrice.value !== null && price < minPrice.value) return false
    if (maxPrice.value !== undefined && maxPrice.value !== null && maxPrice.value > 0 && price > maxPrice.value) return false
    return true
  })
})

onMounted(async () => {
  await loadCategories()
  await loadProducts()
})

watch(() => route.query, query => {
  searchQuery.value = query.keyword || ''
  selectedCategory.value = query.categoryId || ''
  sortBy.value = query.sortBy || ''
  currentPage.value = Number(query.page || 1)
  minPrice.value = query.minPrice ? Number(query.minPrice) : undefined
  maxPrice.value = query.maxPrice ? Number(query.maxPrice) : undefined
  loadProducts()
})

const syncQuery = (patch = {}) => {
  const query = {
    keyword: searchQuery.value || undefined,
    categoryId: selectedCategory.value || undefined,
    sortBy: sortBy.value || undefined,
    page: currentPage.value > 1 ? currentPage.value : undefined,
    minPrice: minPrice.value || undefined,
    maxPrice: maxPrice.value || undefined,
    ...patch
  }
  router.push({ path: '/products', query })
}

const loadCategories = async () => {
  try {
    const response = await productAPI.getCategories()
    categories.value = (response.data.data || []).filter(item => item.status !== 0)
  } catch (error) {
    console.error('获取分类失败:', error)
  }
}

const loadProducts = async () => {
  loading.value = true
  try {
    const params = {
      page: currentPage.value,
      size: pageSize.value,
      categoryId: selectedCategory.value || undefined,
      keyword: searchQuery.value || undefined,
      sortBy: sortBy.value || undefined
    }
    const response = await productAPI.getList(params)
    products.value = response.data.data?.records || []
    total.value = response.data.data?.total || 0
  } catch (error) {
    console.error('获取商品失败:', error)
    ElMessage.error('获取商品失败')
  } finally {
    loading.value = false
  }
}

const setCategory = (categoryId) => {
  selectedCategory.value = categoryId
  currentPage.value = 1
  syncQuery({ categoryId: categoryId || undefined, page: undefined })
}

const setSort = (value) => {
  sortBy.value = value
  currentPage.value = 1
  syncQuery({ sortBy: value || undefined, page: undefined })
}

const applyPrice = () => {
  currentPage.value = 1
  syncQuery({ page: undefined })
}

const handleSizeChange = () => {
  currentPage.value = 1
  loadProducts()
}

const handleCurrentChange = (page) => {
  currentPage.value = page
  syncQuery({ page: page > 1 ? page : undefined })
}

const resetFilters = () => {
  searchQuery.value = ''
  selectedCategory.value = ''
  sortBy.value = ''
  minPrice.value = undefined
  maxPrice.value = undefined
  currentPage.value = 1
  syncQuery({})
}

const goToDetail = (productId) => {
  router.push(`/product/${productId}`)
}

const addToCart = async (productId) => {
  if (!userStore.isAuthenticated) {
    router.push('/login')
    return
  }

  try {
    await cartStore.addToCart(productId)
    ElMessage.success('已加入购物车')
  } catch (error) {
    ElMessage.error(error.message || '添加购物车失败')
  }
}
</script>

<style scoped>
.products-page {
  padding-top: 18px;
}

.crumb {
  color: #999;
  font-size: 13px;
  margin-bottom: 12px;
}

.filter-board {
  background: #fff;
  border-radius: 8px;
  border: 1px solid #eee;
}

.filter-row {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
  padding: 14px 16px;
  border-bottom: 1px solid #f0f0f0;
}

.filter-row:last-child {
  border-bottom: 0;
}

.filter-row > span:first-child {
  color: #999;
  width: 42px;
}

.filter-row button {
  height: 30px;
  padding: 0 12px;
  border: 0;
  border-radius: 4px;
  background: transparent;
  cursor: pointer;
  color: #333;
}

.filter-row button.active,
.filter-row button:hover {
  background: #fff3ed;
  color: #ff5000;
}

.tools :deep(.el-input-number) {
  width: 100px;
}

.dash {
  color: #bbb;
}

.list-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin: 22px 0 14px;
}

.list-head h1 {
  font-size: 28px;
}

.list-head p {
  margin-top: 6px;
  color: #888;
}

.product-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 14px;
}

.pagination {
  display: flex;
  justify-content: center;
  margin-top: 26px;
}

@media (max-width: 980px) {
  .product-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 560px) {
  .list-head {
    align-items: stretch;
    flex-direction: column;
  }

  .product-grid {
    grid-template-columns: 1fr;
  }
}
</style>
