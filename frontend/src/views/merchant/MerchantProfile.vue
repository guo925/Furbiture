<template>
  <div class="merchant-profile">
    <div class="page-header">
      <h2>个人中心</h2>
    </div>

    <!-- 个人中心内容 -->
    <div class="profile-content">
      <div class="profile-form">
        <h3>商家信息</h3>
        <el-form :model="userInfo" :rules="rules" ref="formRef" label-width="100px">
          <el-form-item label="用户名">
            <el-input v-model="userInfo.username" />
          </el-form-item>
          <el-form-item label="邮箱" prop="email">
            <el-input v-model="userInfo.email" />
          </el-form-item>
          <el-form-item label="手机号" prop="phone">
            <el-input v-model="userInfo.phone" />
          </el-form-item>
          <el-form-item label="姓名" prop="name">
            <el-input v-model="userInfo.name" />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" @click="handleUpdate">保存修改</el-button>
          </el-form-item>
        </el-form>

        <h3 class="password-title">修改密码</h3>
        <el-form :model="passwordForm" :rules="passwordRules" ref="passwordFormRef" label-width="100px">
          <el-form-item label="当前密码" prop="oldPassword">
            <el-input type="password" v-model="passwordForm.oldPassword" />
          </el-form-item>
          <el-form-item label="新密码" prop="newPassword">
            <el-input type="password" v-model="passwordForm.newPassword" />
          </el-form-item>
          <el-form-item label="确认密码" prop="confirmPassword">
            <el-input type="password" v-model="passwordForm.confirmPassword" />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" @click="handleChangePassword">修改密码</el-button>
          </el-form-item>
        </el-form>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { merchantAPI } from '../../api'
import { ElMessage } from 'element-plus'

const formRef = ref(null)
const passwordFormRef = ref(null)

const userInfo = reactive({
  username: '',
  email: '',
  phone: '',
  name: ''
})

const passwordForm = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})

const validateConfirmPassword = (rule, value, callback) => {
  if (value !== passwordForm.newPassword) {
    callback(new Error('两次输入的密码不一致'))
  } else {
    callback()
  }
}

const rules = {
  email: [
    { type: 'email', message: '请输入正确的邮箱格式', trigger: 'blur' }
  ],
  phone: [
    { pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号', trigger: 'blur' }
  ]
}

const passwordRules = {
  oldPassword: [
    { required: true, message: '请输入当前密码', trigger: 'blur' }
  ],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, message: '密码长度不能少于6位', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请确认新密码', trigger: 'blur' },
    { validator: validateConfirmPassword, trigger: 'blur' }
  ]
}

onMounted(async () => {
  await loadUserInfo()
})

const loadUserInfo = async () => {
  try {
    const response = await merchantAPI.info.get()
    const data = response.data.data
    Object.assign(userInfo, {
      username: data.username || '',
      email: data.email || '',
      phone: data.phone || '',
      name: data.name || ''
    })
  } catch (error) {
    console.error('获取商家信息失败:', error)
    ElMessage.error(error.message || '获取商家信息失败')
  }
}

const handleUpdate = async () => {
  try {
    await formRef.value.validate()
    await merchantAPI.updateProfile(userInfo)
    ElMessage.success('信息更新成功')
  } catch (error) {
    if (error.errorFields) return
    ElMessage.error(error.message || '更新失败')
  }
}

const handleChangePassword = async () => {
  try {
    await passwordFormRef.value.validate()
    
    await merchantAPI.changePassword({
      oldPassword: passwordForm.oldPassword,
      newPassword: passwordForm.newPassword
    })
    
    ElMessage.success('密码修改成功')
    passwordForm.oldPassword = ''
    passwordForm.newPassword = ''
    passwordForm.confirmPassword = ''
    passwordFormRef.value.resetFields()
  } catch (error) {
    if (error.errorFields) return
    ElMessage.error(error.message || '修改密码失败')
  }
}
</script>

<style scoped>
.merchant-profile {
  background: #fff;
  padding: 20px;
  border-radius: 8px;
  box-shadow: 0 2px 12px rgba(0,0,0,0.1);
  min-height: 600px;
}

.page-header {
  margin-bottom: 30px;
  padding-bottom: 15px;
  border-bottom: 1px solid #eaeaea;
}

.page-header h2 {
  margin: 0;
  font-size: 24px;
  font-weight: bold;
  color: #333;
  display: flex;
  align-items: center;
}

.page-header h2::before {
  content: '';
  display: inline-block;
  width: 4px;
  height: 20px;
  background: #67c23a;
  margin-right: 10px;
  border-radius: 2px;
}

.profile-form {
  max-width: 600px;
}

.profile-form h3 {
  margin-bottom: 20px;
  color: #333;
  font-size: 18px;
  font-weight: bold;
  padding-bottom: 10px;
  border-bottom: 1px solid #f0f0f0;
}

.password-title {
  margin-top: 40px;
}

:deep(.el-form-item__label) {
  font-weight: 500;
  color: #666;
}

:deep(.el-input) {
  max-width: 300px;
}

:deep(.el-button--primary) {
  background: #67c23a;
  border-color: #67c23a;
}

:deep(.el-button--primary:hover) {
  background: #85ce61;
  border-color: #85ce61;
}
</style>