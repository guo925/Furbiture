import { ref, reactive } from 'vue'

/**
 * 通用表单 Composable
 * 封装表单对话框的通用逻辑（新增/编辑）
 * 
 * @param {Object} defaultForm - 表单默认值
 * @param {Function} saveFn - 保存函数，接收 formData 参数
 * @returns 表单相关的状态和方法
 */
export function useForm(defaultForm = {}, saveFn) {
  const dialogVisible = ref(false)
  const dialogTitle = ref('')
  const formRef = ref(null)
  const formData = reactive({ ...defaultForm })
  const isEdit = ref(false)
  const saving = ref(false)

  const handleAdd = () => {
    isEdit.value = false
    dialogTitle.value = '新增'
    Object.assign(formData, { ...defaultForm })
    dialogVisible.value = true
  }

  const handleEdit = (row) => {
    isEdit.value = true
    dialogTitle.value = '编辑'
    Object.assign(formData, { ...defaultForm, ...row })
    dialogVisible.value = true
  }

  const handleSave = async () => {
    if (!formRef.value) return
    try {
      await formRef.value.validate()
      saving.value = true
      await saveFn({ ...formData }, isEdit.value)
      dialogVisible.value = false
      return true
    } catch (error) {
      if (error !== false && error?.errorFields) return false
      throw error
    } finally {
      saving.value = false
    }
  }

  const resetForm = () => {
    Object.assign(formData, { ...defaultForm })
    formRef.value?.resetFields()
  }

  return {
    dialogVisible,
    dialogTitle,
    formRef,
    formData,
    isEdit,
    saving,
    handleAdd,
    handleEdit,
    handleSave,
    resetForm
  }
}
