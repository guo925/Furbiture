<template>
  <div class="login">
    <div class="login-container">
      <h2 class="login-title">用户登录</h2>
      <el-form :model="loginForm" :rules="loginRules" ref="loginFormRef" label-width="80px">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="loginForm.username" placeholder="请输入用户名" />
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input v-model="loginForm.password" type="password" placeholder="请输入密码" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleLogin" :loading="loading">登录</el-button>
          <el-button @click="resetForm">重置</el-button>
        </el-form-item>
        <el-form-item>
          <span>还没有账号？</span>
          <router-link to="/register">立即注册</router-link>
        </el-form-item>
      </el-form>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '../../stores/user'
import { ElMessage } from 'element-plus'

const router = useRouter()
const userStore = useUserStore()

const loginFormRef = ref(null)
const loading = ref(false)

const loginForm = reactive({
  username: '',
  password: ''
})

const loginRules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' }
  ]
}

const handleLogin = async () => {
  if (!loginFormRef.value) return
  
  try {
    await loginFormRef.value.validate()
    loading.value = true
    
    const result = await userStore.login(loginForm.username, loginForm.password)
    ElMessage.success('登录成功')
    
    // 根据用户角色跳转到不同页面
    if (userStore.user.role === 'ADMIN') {
      router.push('/admin/dashboard')
    } else if (userStore.user.role === 'MERCHANT') {
      router.push('/merchant')
    } else {
      router.push('/home')
    }
  } catch (error) {
    ElMessage.error(error.message || '登录失败')
  } finally {
    loading.value = false
  }
}

const resetForm = () => {
  if (loginFormRef.value) {
    loginFormRef.value.resetFields()
  }
}
</script>

<style scoped>
.login {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f5f7fa;
}

.login-container {
  background: #fff;
  padding: 40px;
  border-radius: 8px;
  box-shadow: 0 4px 16px rgba(0,0,0,0.1);
  width: 400px;
}

.login-title {
  font-size: 24px;
  font-weight: bold;
  color: #333;
  text-align: center;
  margin-bottom: 30px;
}

.el-form-item:last-child {
  text-align: center;
}

.el-form-item span {
  color: #666;
}

.el-form-item a {
  color: var(--color-primary);
  text-decoration: none;
  margin-left: 5px;
}

.el-form-item a:hover {
  text-decoration: underline;
}
</style>