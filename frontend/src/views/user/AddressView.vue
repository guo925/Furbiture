<template>
  <div class="address">
    <!-- 导航栏 -->
    <header class="navbar">
      <div class="container">
        <div class="logo">
          <router-link to="/">家具商城</router-link>
        </div>
        <nav class="nav">
          <router-link to="/home" class="nav-item">首页</router-link>
          <router-link to="/products" class="nav-item">商品列表</router-link>
          <router-link to="/cart" class="nav-item">
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

    <!-- 地址管理 -->
    <div class="address-section">
      <div class="container">
        <h2 class="page-title">地址管理</h2>
        
        <el-button type="primary" @click="handleAdd" class="add-btn">添加收货地址</el-button>
        
        <div class="address-list" v-if="addresses.length > 0">
          <div v-for="address in addresses" :key="address.id" class="address-item">
            <div class="address-content">
              <div class="address-header">
                <span class="address-name">{{ address.receiver }}</span>
                <span class="address-phone">{{ address.phone }}</span>
                <span v-if="address.isDefault" class="default-tag">默认</span>
              </div>
              <div class="address-detail">{{ address.province }} {{ address.city }} {{ address.district }} {{ address.detail }}</div>
            </div>
            <div class="address-actions">
              <el-button @click="handleEdit(address)">编辑</el-button>
              <el-button v-if="!address.isDefault" @click="setDefault(address.id)">设为默认</el-button>
              <el-button type="danger" @click="handleDelete(address.id)">删除</el-button>
            </div>
          </div>
        </div>
        
        <div v-else class="empty-address">
          <el-empty description="暂无收货地址" />
          <el-button type="primary" @click="handleAdd">添加收货地址</el-button>
        </div>
      </div>
    </div>

    <!-- 添加/编辑地址对话框 -->
    <el-dialog v-model="dialogVisible" :title="dialogTitle">
      <el-form :model="addressForm" :rules="addressRules" ref="addressFormRef" label-width="100px">
        <el-form-item label="收货人" prop="receiver">
          <el-input v-model="addressForm.receiver" />
        </el-form-item>
        <el-form-item label="手机号" prop="phone">
          <el-input v-model="addressForm.phone" />
        </el-form-item>
        <el-form-item label="省份" prop="province">
          <el-input v-model="addressForm.province" />
        </el-form-item>
        <el-form-item label="城市" prop="city">
          <el-input v-model="addressForm.city" />
        </el-form-item>
        <el-form-item label="区县" prop="district">
          <el-input v-model="addressForm.district" />
        </el-form-item>
        <el-form-item label="详细地址" prop="detail">
          <el-input v-model="addressForm.detail" type="textarea" />
        </el-form-item>
        <el-form-item label="设为默认">
          <el-switch v-model="addressForm.isDefault" />
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
import { ref, onMounted, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '../../stores/user'
import { useCartStore } from '../../stores/cart'
import { addressAPI } from '../../api'
import { ElMessage, ElMessageBox } from 'element-plus'

const router = useRouter()
const userStore = useUserStore()
const cartStore = useCartStore()

const addresses = ref([])
const dialogVisible = ref(false)
const dialogTitle = ref('添加收货地址')
const addressFormRef = ref(null)

const addressForm = reactive({
  id: null,
  receiver: '',
  phone: '',
  province: '',
  city: '',
  district: '',
  detail: '',
  isDefault: false
})

const addressRules = {
  receiver: [{ required: true, message: '请输入收货人', trigger: 'blur' }],
  phone: [{ required: true, message: '请输入手机号', trigger: 'blur' }],
  province: [{ required: true, message: '请输入省份', trigger: 'blur' }],
  city: [{ required: true, message: '请输入城市', trigger: 'blur' }],
  district: [{ required: true, message: '请输入区县', trigger: 'blur' }],
  detail: [{ required: true, message: '请输入详细地址', trigger: 'blur' }]
}

onMounted(async () => {
  if (!userStore.isAuthenticated) {
    router.push('/login')
    return
  }
  await loadAddresses()
  await cartStore.getCartList()
})

const loadAddresses = async () => {
  try {
    const response = await addressAPI.getList()
    addresses.value = (response.data.data || []).map(addr => ({
      id: addr.id,
      receiver: addr.name,
      phone: addr.phone,
      province: addr.province,
      city: addr.city,
      district: addr.district,
      detail: addr.detailAddress,
      isDefault: addr.isDefault === 1
    }))
  } catch (error) {
    console.error('获取地址失败:', error)
    ElMessage.error('获取地址失败')
  }
}



const handleAdd = () => {
  dialogTitle.value = '添加收货地址'
  Object.assign(addressForm, {
    id: null,
    receiver: '',
    phone: '',
    province: '',
    city: '',
    district: '',
    detail: '',
    isDefault: false
  })
  dialogVisible.value = true
}

const handleEdit = (address) => {
  dialogTitle.value = '编辑收货地址'
  Object.assign(addressForm, address)
  dialogVisible.value = true
}

const handleSave = async () => {
  if (!addressFormRef.value) return

  try {
    await addressFormRef.value.validate()

    const submitData = {
      name: addressForm.receiver,
      phone: addressForm.phone,
      province: addressForm.province,
      city: addressForm.city,
      district: addressForm.district,
      detailAddress: addressForm.detail,
      isDefault: addressForm.isDefault ? 1 : 0
    }

    if (addressForm.id) {
      await addressAPI.update(addressForm.id, submitData)
      ElMessage.success('更新成功')
    } else {
      await addressAPI.create(submitData)
      ElMessage.success('添加成功')
    }

    dialogVisible.value = false
    await loadAddresses()
  } catch (error) {
    // 显示错误信息，但不关闭对话框，也不重新加载地址列表
    ElMessage.error(error.response?.data?.msg || error.message || '操作失败')
  }
}

const setDefault = async (id) => {
  try {
    await addressAPI.setDefault(id)
    ElMessage.success('设置默认地址成功')
    await loadAddresses()
  } catch (error) {
    ElMessage.error(error.message || '设置失败')
  }
}

const handleDelete = async (id) => {
  try {
    await ElMessageBox.confirm('确定要删除这个地址吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })

    await addressAPI.remove(id)
    ElMessage.success('删除成功')
    await loadAddresses()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(error.message || '删除失败')
    }
  }
}

const logout = () => {
  userStore.logout()
  ElMessage.success('退出成功')
  router.push('/home')
}
</script>

<style scoped>
.address {
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

.nav-item:hover {
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

.address-section {
  flex: 1;
  padding: 40px 0;
}

.page-title {
  font-size: 24px;
  font-weight: bold;
  color: #333;
  margin-bottom: 30px;
}

.add-btn {
  margin-bottom: 30px;
}

.address-list {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.address-item {
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 2px 12px rgba(0,0,0,0.1);
  padding: 20px;
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
}

.address-content {
  flex: 1;
}

.address-header {
  display: flex;
  align-items: center;
  gap: 20px;
  margin-bottom: 10px;
}

.address-name {
  font-weight: bold;
  color: #333;
}

.address-phone {
  color: #666;
}

.default-tag {
  background: #409eff;
  color: #fff;
  padding: 2px 8px;
  border-radius: 4px;
  font-size: 12px;
}

.address-detail {
  color: #666;
  line-height: 1.5;
}

.address-actions {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.empty-address {
  text-align: center;
  padding: 60px 0;
  background: #f5f7fa;
  border-radius: 8px;
}

.empty-address .el-button {
  margin-top: 20px;
}

.dialog-footer {
  text-align: right;
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