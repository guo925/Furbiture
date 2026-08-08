<template>
  <el-dialog
    v-model="visible"
    :title="title"
    :width="width"
    :close-on-click-modal="false"
    @closed="handleClosed"
  >
    <el-form
      ref="formRef"
      :model="formData"
      :rules="rules"
      :label-width="labelWidth"
    >
      <slot :formData="formData"></slot>
    </el-form>
    <template #footer>
      <span class="dialog-footer">
        <el-button @click="handleCancel">取消</el-button>
        <el-button type="primary" @click="handleConfirm" :loading="loading">
          {{ confirmText }}
        </el-button>
      </span>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref } from 'vue'

const props = defineProps({
  title: { type: String, default: '对话框' },
  width: { type: String, default: '600px' },
  labelWidth: { type: String, default: '100px' },
  confirmText: { type: String, default: '确定' },
  loading: { type: Boolean, default: false },
  formData: { type: Object, required: true },
  rules: { type: Object, default: () => ({}) }
})

const emit = defineEmits(['confirm', 'cancel', 'closed'])

const visible = defineModel('visible', { type: Boolean, default: false })
const formRef = ref(null)

const handleConfirm = async () => {
  if (!formRef.value) {
    emit('confirm')
    return
  }
  try {
    await formRef.value.validate()
    emit('confirm')
  } catch {
    // validation failed
  }
}

const handleCancel = () => {
  visible.value = false
  emit('cancel')
}

const handleClosed = () => {
  formRef.value?.resetFields()
  emit('closed')
}

const validate = () => formRef.value?.validate()
const resetFields = () => formRef.value?.resetFields()

defineExpose({ validate, resetFields })
</script>

<style scoped>
.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
}
</style>
