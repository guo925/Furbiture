<template>
  <div class="admin-page">
    <div class="page-toolbar">
      <div class="toolbar-left">
        <h2>分类管理</h2>
        <el-input v-model="searchKeyword" placeholder="搜索分类名称" clearable class="search-input" @keyup.enter="handleSearch" @clear="handleSearch">
          <template #prefix><el-icon><Search /></el-icon></template>
        </el-input>
        <el-button type="primary" @click="handleSearch">查询</el-button>
      </div>
      <el-button type="primary" @click="handleAdd"><el-icon><Plus /></el-icon>新增分类</el-button>
    </div>

    <div class="table-card">
      <el-table :data="categories" v-loading="loading" stripe row-key="id" default-expand-all :tree-props="{ children: 'children', hasChildren: 'hasChildren' }">
        <el-table-column label="分类名称" min-width="220">
          <template #default="{ row }">
            <div class="category-cell">
              <el-image v-if="row.icon" :src="row.icon" fit="cover" class="cat-thumb">
                <template #error><div class="cat-thumb-placeholder"><el-icon :size="16"><Folder /></el-icon></div></template>
              </el-image>
              <span v-else class="cat-thumb cat-thumb-placeholder"><el-icon :size="16"><Folder /></el-icon></span>
              <span class="cat-name">{{ row.name }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="parentName" label="父级分类" width="120">
          <template #default="{ row }"><span v-if="row.parentId > 0">{{ row.parentName || '—' }}</span><span v-else class="text-muted">顶级分类</span></template>
        </el-table-column>
        <el-table-column prop="sortOrder" label="排序" width="80" sortable />
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-switch :model-value="row.status === 1" @change="(v) => handleToggleStatus(row, v)" active-text="启用" inactive-text="禁用" inline-prompt />
          </template>
        </el-table-column>
        <el-table-column label="操作" width="160" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="handleEdit(row)">编辑</el-button>
            <el-button link type="danger" size="small" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div class="table-footer">
        <el-pagination
          v-model:current-page="page" v-model:page-size="size"
          :page-sizes="[10,20,50]" :total="total" layout="total, sizes, prev, pager, next"
          @size-change="loadData" @current-change="loadData"
        />
      </div>
    </div>

    <!-- 对话框 -->
    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑分类' : '新增分类'" :width="isMobile ? '92%' : '500px'" :close-on-click-modal="false" @closed="resetForm">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="分类名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入分类名称" />
        </el-form-item>
        <el-form-item label="父级分类">
          <el-select v-model="form.parentId" placeholder="选择父级（留空为顶级）" clearable style="width:100%">
            <el-option label="无（顶级分类）" :value="0" />
            <el-option v-for="cat in parentOptions" :key="cat.id" :label="cat.name" :value="cat.id" />
          </el-select>
        </el-form-item>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="排序号" prop="sortOrder">
              <el-input-number v-model="form.sortOrder" :min="0" style="width:100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="状态">
              <el-switch v-model="form.statusBool" active-text="启用" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="图标URL">
          <el-input v-model="form.icon" placeholder="图标URL（可选）" />
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
import { adminAPI } from '../../api/modules/admin'
import { Search, Plus, Folder } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { useBreakpoint } from '../../composables/useBreakpoint'
import { confirm } from '../../composables/useConfirm'

// 弹窗宽度需要随视口变化，故用断点状态而不是写死 500px
const { isMobile } = useBreakpoint()

const categories = ref([])
const total = ref(0)
const page = ref(1)
const size = ref(10)
const loading = ref(false)
const searchKeyword = ref('')
const parentOptions = ref([])

const dialogVisible = ref(false)
const isEdit = ref(false)
const saving = ref(false)
const formRef = ref(null)

const defaultForm = { name: '', parentId: 0, sortOrder: 0, statusBool: true, icon: '' }
const form = reactive({ ...defaultForm })
const rules = { name: [{ required: true, message: '请输入分类名称', trigger: 'blur' }] }

const loadData = async () => {
  loading.value = true
  try {
    const params = { page: page.value, size: size.value }
    if (searchKeyword.value) params.name = searchKeyword.value
    const res = await adminAPI.categories.getList(params)
    categories.value = res.data.data.records || []
    total.value = res.data.data.total || 0
    parentOptions.value = categories.value.filter(c => c.parentId === 0)
  } finally { loading.value = false }
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
    parentId: row.parentId ? Number(row.parentId) : 0,
    statusBool: row.status === 1
  })
  dialogVisible.value = true
}

const handleSave = async () => {
  if (!formRef.value) return
  try {
    await formRef.value.validate()
    saving.value = true
    const data = { ...form, status: form.statusBool ? 1 : 0, parentId: Number(form.parentId) || 0 }
    delete data.statusBool; delete data.children; delete data.parentName
    if (isEdit.value) {
      await adminAPI.categories.update(form.id, data)
      ElMessage.success('更新成功')
    } else {
      await adminAPI.categories.create(data)
      ElMessage.success('添加成功')
    }
    dialogVisible.value = false
    loadData()
  } catch (e) { if (e?.errorFields) return } finally { saving.value = false }
}

const handleDelete = async (row) => {
  if (!await confirm(`确定删除分类「${row.name}」吗？`, '删除确认')) return
  await adminAPI.categories.delete(row.id)
  ElMessage.success('删除成功')
  loadData()
}

const handleToggleStatus = async (row, val) => {
  await adminAPI.categories.update(row.id, { ...row, status: val ? 1 : 0, children: undefined, parentName: undefined })
  ElMessage.success(val ? '已启用' : '已禁用')
  loadData()
}

const resetForm = () => {
  formRef.value?.resetFields()
  Object.assign(form, { ...defaultForm })
}

onMounted(() => loadData())
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
.category-cell { display: flex; align-items: center; gap: 10px; }
.cat-thumb { width: 36px; height: 36px; border-radius: 6px; flex-shrink: 0; }
.cat-thumb-placeholder {
  background: #f7fafc; display: flex; align-items: center; justify-content: center; color: #cbd5e0;
}
.cat-name { font-size: 14px; font-weight: 500; color: #2d3748; }
.text-muted { color: #a0aec0; font-size: 13px; }
.table-footer { display: flex; justify-content: flex-end; margin-top: 16px; }

/* ===== 响应式：断点取值见 composables/useBreakpoint.js ===== */

/* 平板及以下：卡片内边距收窄，把宽度还给表格本身 */
@media (max-width: 1024px) {
  .table-card {
    padding: 12px;
  }
}

/* 移动端：工具栏改为竖向堆叠（搜索框占满整行），分页居中并允许换行。
   树形表格列宽合计 680px，窄屏下由 el-table 自身横向滚动兜底，不做压缩。 */
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
