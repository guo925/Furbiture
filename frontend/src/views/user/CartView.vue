<template>
  <div class="cart">
    <!-- 导航栏 -->
    <header class="navbar">
      <div class="container">
        <div class="logo">
          <router-link to="/">家具商城</router-link>
        </div>
        <nav class="nav">
          <router-link to="/home" class="nav-item">首页</router-link>
          <router-link to="/products" class="nav-item">商品列表</router-link>
          <router-link to="/cart" class="nav-item active">
            购物车
            <span v-if="cartStore.totalQuantity > 0" class="cart-badge">{{ cartStore.totalQuantity }}</span>
          </router-link>
        </nav>
        <div class="user">
          <template v-if="userStore.isAuthenticated">
            <span class="welcome">欢迎, {{ userStore.user.username }}</span>
            <router-link to="/profile" class="nav-item">个人中心</router-link>
            <router-link to="/orders" class="nav-item">我的订单</router-link>
            <button @click="logout" class="btn">退出</button>
          </template>
          <template v-else>
            <router-link to="/login" class="btn">登录</router-link>
            <router-link to="/register" class="btn btn-primary">注册</router-link>
          </template>
        </div>
      </div>
    </header>

    <!-- 购物车内容 -->
    <div class="cart-section">
      <div class="container">
        <h2 class="section-title">我的购物车</h2>
        
        <div v-if="cartStore.loading" class="loading">
          <el-skeleton :rows="4" animated />
        </div>
        
        <div v-else-if="cartStore.cartItems.length === 0" class="empty-cart">
          <el-empty description="购物车是空的" />
          <router-link to="/products" class="btn btn-primary">去购物</router-link>
        </div>
        
        <div v-else class="cart-content">
          <el-table :data="cartStore.cartItems" style="width: 100%" row-key="id">
            <el-table-column prop="productId" label="商品ID" width="100" />
            <el-table-column label="商品名称" min-width="200">
              <template #default="scope">
                {{ scope.row.product?.name || '商品已下架' }}
              </template>
            </el-table-column>
            <el-table-column label="单价" width="120">
              <template #default="scope">
                ¥{{ scope.row.product?.price || 0 }}
              </template>
            </el-table-column>
            <el-table-column label="数量" width="200">
              <template #default="scope">
                <el-input-number
                  v-model="scope.row.quantity"
                  @change="updateQuantity(scope.row.id, scope.row.quantity)"
                  :min="1"
                  :max="99"
                  size="small"
                />
              </template>
            </el-table-column>
            <el-table-column label="小计" width="120">
              <template #default="scope">
                ¥{{ (scope.row.product?.price || 0) * (scope.row.quantity || 0) }}
              </template>
            </el-table-column>
            <el-table-column label="操作" width="120">
              <template #default="scope">
                <el-button
                  type="danger"
                  plain
                  @click="removeItem(scope.row.id)"
                  :icon="Delete"
                  size="small"
                >
                  删除
                </el-button>
              </template>
            </el-table-column>
          </el-table>
          
          <div class="cart-footer">
            <div class="total">
              <span>总计：</span>
              <span class="total-price">¥{{ isNaN(cartStore.totalPrice) ? '0.00' : cartStore.totalPrice.toFixed(2) }}</span>
            </div>
            <div class="actions">
              <button @click="clearCart" class="btn">清空购物车</button>
              <router-link to="/checkout" class="btn btn-primary">去结算</router-link>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 页脚 -->
    <footer class="footer">
      <div class="container">
        <p>&copy; 2026 家具商城. 保留所有权利.</p>
      </div>
    </footer>
  </div>
</template>

<script setup>
import { onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '../../stores/user'
import { useCartStore } from '../../stores/cart'
import { Delete } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'

const router = useRouter()
const userStore = useUserStore()
const cartStore = useCartStore()

onMounted(async () => {
  if (!userStore.isAuthenticated) {
    router.push('/login')
    return
  }
  await cartStore.getCartList()
})

const updateQuantity = async (id, quantity) => {
  try {
    await cartStore.updateCartItem(id, quantity)
    ElMessage.success('更新成功')
  } catch (error) {
    ElMessage.error(error.msg || error.message || '更新失败')
  }
}

const removeItem = async (id) => {
  try {
    await cartStore.removeCartItem(id)
    ElMessage.success('删除成功')
  } catch (error) {
    ElMessage.error(error.message || '删除失败')
  }
}

const clearCart = async () => {
  try {
    await cartStore.clearCart()
    ElMessage.success('购物车已清空')
  } catch (error) {
    ElMessage.error(error.message || '清空失败')
  }
}

const logout = () => {
  userStore.logout()
  ElMessage.success('退出成功')
  router.push('/home')
}
</script>

<style scoped>
.cart {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
}

.navbar {
  background: #fff;
  box-shadow: 0 2px 8px rgba(0,0,0,0.1);
  position: sticky;
  top: 0;
  z-index: 100;
}

.container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 20px;
}

.navbar .container {
  display: flex;
  justify-content: space-between;
  align-items: center;
  height: 60px;
}

.logo a {
  font-size: 24px;
  font-weight: bold;
  color: #333;
  text-decoration: none;
}

.nav {
  display: flex;
  gap: 30px;
}

.nav-item {
  color: #666;
  text-decoration: none;
  font-size: 16px;
  transition: color 0.3s;
}

.nav-item:hover,
.nav-item.active {
  color: #409eff;
}

.user {
  display: flex;
  align-items: center;
  gap: 15px;
}

.welcome {
  color: #666;
}

.btn {
  padding: 6px 16px;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  background: #fff;
  color: #666;
  cursor: pointer;
  transition: all 0.3s;
  text-decoration: none;
}

.btn:hover {
  border-color: #409eff;
  color: #409eff;
}

.btn-primary {
  background: #409eff;
  color: #fff;
  border-color: #409eff;
}

.btn-primary:hover {
  background: #66b1ff;
  border-color: #66b1ff;
  color: #fff;
}

.cart-badge {
  background: #f56c6c;
  color: #fff;
  border-radius: 50%;
  font-size: 12px;
  padding: 2px 6px;
  margin-left: 5px;
}

.cart-section {
  flex: 1;
  padding: 30px 0;
}

.section-title {
  font-size: 24px;
  font-weight: bold;
  color: #333;
  margin-bottom: 30px;
  text-align: center;
}

.loading {
  padding: 40px 0;
}

.empty-cart {
  text-align: center;
  padding: 60px 0;
}

.empty-cart .btn {
  margin-top: 20px;
}

.cart-content {
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.1);
  overflow: hidden;
}

.cart-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px;
  border-top: 1px solid #e4e7ed;
  background: #f5f7fa;
}

.total {
  font-size: 18px;
  font-weight: bold;
  color: #333;
}

.total-price {
  color: #f56c6c;
  margin-left: 10px;
}

.actions {
  display: flex;
  gap: 15px;
}

.footer {
  background: #f5f7fa;
  padding: 30px 0;
  margin-top: auto;
}

.footer p {
  text-align: center;
  color: #666;
  font-size: 14px;
}
</style>