<template>
  <div class="admin-users">
    <div class="page-header">
      <h2>用户管理</h2>
      <el-button type="primary" @click="handleAdd">添加用户</el-button>
    </div>

    <!-- 搜索和筛选 -->
    <div class="filter-bar">
      <el-input v-model="searchQuery" placeholder="搜索用户名或手机号" class="search-input">
        <template #append>
          <el-button @click="handleSearch"><el-icon><Search /></el-icon></el-button>
        </template>
      </el-input>
      <el-button @click="resetFilter">重置</el-button>
    </div>

    <!-- 用户列表 -->
    <el-table :data="users" style="width: 100%">
      <el-table-column prop="id" label="ID" width="80" />
      <!-- 三个自适应列补 min-width：容器不够宽时让表格横向滚动，而不是把列压成省略号。
           代价：表格最小宽度由 800px 抬到 990px，视口 <1300px 时本页会出现横向滚动条
           （改动前是压缩列宽硬塞）。这是为消除窄屏"用户名只剩省略号"有意做的取舍。 -->
      <el-table-column prop="username" label="用户名" min-width="120" />
      <el-table-column prop="phone" label="手机号" min-width="130" />
      <el-table-column prop="email" label="邮箱" min-width="180" />
      <el-table-column prop="role" label="角色" width="100">
        <template #default="scope">
          <el-tag v-if="scope.row.role === 'ADMIN'" type="danger">管理员</el-tag>
          <el-tag v-else-if="scope.row.role === 'MERCHANT'" type="warning">商家</el-tag>
          <el-tag v-else type="success">用户</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createTime" label="创建时间" width="180" />
      <el-table-column label="操作" width="200">
        <template #default="scope">
          <el-button type="primary" @click="handleEdit(scope.row)">
            <el-icon><Edit /></el-icon>
          </el-button>
          <el-button type="warning" @click="handleResetPassword(scope.row)">
            <el-icon><Lock /></el-icon>
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

    <!-- 添加/编辑用户对话框 -->
    <el-dialog v-model="dialogVisible" :title="dialogTitle" :width="isMobile ? '92%' : '50%'">
      <el-form :model="userForm" :rules="userRules" ref="userFormRef" label-width="100px">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="userForm.username" />
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input v-model="userForm.password" type="password" show-password placeholder="请输入密码" />
        </el-form-item>
        <el-form-item label="手机号" prop="phone">
          <el-input v-model="userForm.phone" />
        </el-form-item>
        <el-form-item label="邮箱" prop="email">
          <el-input v-model="userForm.email" />
        </el-form-item>
        <el-form-item label="角色" prop="role">
          <el-select v-model="userForm.role" placeholder="选择角色">
            <el-option label="用户" value="USER" />
            <el-option label="管理员" value="ADMIN" />
            <el-option label="商家" value="MERCHANT" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" @click="handleSave">保存</el-button>
        </span>
      </template>
    </el-dialog>

    <!-- 重置密码对话框 -->
    <el-dialog v-model="passwordDialogVisible" title="重置密码" :width="isMobile ? '92%' : '400px'">
      <el-form :model="passwordForm" :rules="passwordRules" ref="passwordFormRef" label-width="100px">
        <el-form-item label="新密码" prop="password">
          <el-input v-model="passwordForm.password" type="password" show-password placeholder="请输入新密码" />
        </el-form-item>
        <el-form-item label="确认密码" prop="confirmPassword">
          <el-input v-model="passwordForm.confirmPassword" type="password" show-password placeholder="请确认新密码" />
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="passwordDialogVisible = false">取消</el-button>
          <el-button type="primary" @click="handlePasswordSave">确定</el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted, reactive } from 'vue'
import { adminAPI } from '../../api'
import { Search, Edit, Delete, Lock } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useBreakpoint } from '../../composables/useBreakpoint'

// 弹窗宽度需要随视口变化，故用断点状态而不是写死固定宽度
const { isMobile } = useBreakpoint()

const users = ref([])
const total = ref(0)
const currentPage = ref(1)
const pageSize = ref(10)
const searchQuery = ref('')

const dialogVisible = ref(false)
const dialogTitle = ref('添加用户')
const userFormRef = ref(null)
const passwordDialogVisible = ref(false)
const passwordFormRef = ref(null)
const resetPasswordUserId = ref(null)

const userForm = reactive({
  id: null,
  username: '',
  password: '',
  phone: '',
  email: '',
  role: 'USER'
})

const passwordForm = reactive({
  password: '',
  confirmPassword: ''
})

const validatePassword = (rule, value, callback) => {
  if (!userForm.id && !value) {
    callback(new Error('请输入密码'))
  } else {
    callback()
  }
}

const validateConfirmPassword = (rule, value, callback) => {
  if (value !== passwordForm.password) {
    callback(new Error('两次输入的密码不一致'))
  } else {
    callback()
  }
}

const userRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ validator: validatePassword, trigger: 'blur' }],
  role: [{ required: true, message: '请选择角色', trigger: 'blur' }]
}

const passwordRules = {
  password: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, message: '密码长度不能少于6位', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请确认新密码', trigger: 'blur' },
    { validator: validateConfirmPassword, trigger: 'blur' }
  ]
}

onMounted(async () => {
  await loadUsers()
})

const loadUsers = async () => {
  try {
    const params = {
      page: currentPage.value,
      size: pageSize.value
    }
    if (searchQuery.value) {
      params.username = searchQuery.value
    }
    const response = await adminAPI.users.getList(params)
    users.value = response.data.data.records
    total.value = response.data.data.total
  } catch (error) {
    console.error('获取用户失败:', error)
  }
}

const handleSearch = () => {
  currentPage.value = 1
  loadUsers()
}

const resetFilter = () => {
  searchQuery.value = ''
  currentPage.value = 1
  loadUsers()
}

const handleSizeChange = (size) => {
  pageSize.value = size
  loadUsers()
}

const handleCurrentChange = (page) => {
  currentPage.value = page
  loadUsers()
}

const handleAdd = () => {
  dialogTitle.value = '添加用户'
  Object.assign(userForm, {
    id: null,
    username: '',
    password: '',
    phone: '',
    email: '',
    role: 'USER'
  })
  dialogVisible.value = true
}

const handleEdit = (row) => {
  dialogTitle.value = '编辑用户'
  Object.assign(userForm, {
    id: row.id,
    username: row.username,
    password: '',
    phone: row.phone || '',
    email: row.email || '',
    role: row.role || 'USER'
  })
  dialogVisible.value = true
}

const handleSave = async () => {
  if (!userFormRef.value) return

  try {
    await userFormRef.value.validate()

    const data = {
      username: userForm.username,
      phone: userForm.phone,
      email: userForm.email,
      role: userForm.role
    }

    if (userForm.password) {
      data.password = userForm.password
    }

    if (userForm.id) {
      await adminAPI.users.update(userForm.id, data)
      ElMessage.success('更新成功')
    } else {
      await adminAPI.users.create(data)
      ElMessage.success('添加成功')
    }

    dialogVisible.value = false
    loadUsers()
  } catch (error) {
    ElMessage.error(error.message || '操作失败')
  }
}

const handleDelete = async (id) => {
  try {
    await ElMessageBox.confirm('确定要删除这个用户吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })

    await adminAPI.users.delete(id)
    ElMessage.success('删除成功')
    loadUsers()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(error.message || '删除失败')
    }
  }
}

const handleResetPassword = (row) => {
  resetPasswordUserId.value = row.id
  passwordForm.password = ''
  passwordForm.confirmPassword = ''
  passwordDialogVisible.value = true
}

const handlePasswordSave = async () => {
  if (!passwordFormRef.value) return

  try {
    await passwordFormRef.value.validate()

    await adminAPI.users.resetPassword(resetPasswordUserId.value, passwordForm.password)
    ElMessage.success('密码重置成功')
    passwordDialogVisible.value = false
  } catch (error) {
    ElMessage.error(error.message || '密码重置失败')
  }
}
</script>

<style scoped>
.admin-users {
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
  /* 原先靠 el-input 的内联 margin-right 撑开间距，内联样式媒体查询覆盖不了，改用 flex gap */
  gap: 10px;
}

.search-input {
  width: 300px;
}

.pagination {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}

.dialog-footer {
  text-align: right;
}

/* ===== 响应式：断点取值见 composables/useBreakpoint.js ===== */

/* 平板及以下：容器内边距收窄，把宽度还给表格本身 */
@media (max-width: 1024px) {
  .admin-users {
    padding: 12px;
  }
}

/* 移动端：筛选栏竖向堆叠（搜索框占满整行），分页居中并允许换行。
   表格列宽合计 990px，窄屏下由 el-table 自身横向滚动兜底，不做压缩。 */
@media (max-width: 640px) {
  .page-header {
    flex-wrap: wrap;
    gap: 12px;
  }

  .filter-bar {
    flex-wrap: wrap;
  }

  .search-input {
    width: 100%;
  }

  .pagination {
    justify-content: center;
  }

  .pagination :deep(.el-pagination) {
    flex-wrap: wrap;
    row-gap: 8px;
    justify-content: center;
  }
}
</style>
