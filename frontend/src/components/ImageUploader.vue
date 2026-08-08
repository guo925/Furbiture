<template>
  <div class="image-uploader">
    <div class="upload-grid">
      <!-- 已上传图片 -->
      <div v-for="(img, idx) in modelValue" :key="idx" class="image-item">
        <img :src="img" :alt="`图片 ${idx + 1}`">
        <div class="image-actions">
          <el-button v-if="idx > 0" size="small" circle @click="moveUp(idx)">
            <el-icon><ArrowLeft /></el-icon>
          </el-button>
          <el-button v-if="idx < modelValue.length - 1" size="small" circle @click="moveDown(idx)">
            <el-icon><ArrowRight /></el-icon>
          </el-button>
          <el-button size="small" circle type="danger" @click="remove(idx)">
            <el-icon><Delete /></el-icon>
          </el-button>
        </div>
      </div>

      <!-- 上传区域 -->
      <div v-if="modelValue.length < max" class="upload-trigger" @click="triggerUpload">
        <el-icon :size="28"><Plus /></el-icon>
        <span>上传图片</span>
      </div>
    </div>

    <input ref="fileInput" type="file" accept="image/*" multiple hidden @change="handleFileChange">
    <el-progress v-if="uploading" :percentage="uploadProgress" />
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { ArrowLeft, ArrowRight, Delete, Plus } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { fileAPI } from '../api'

const props = defineProps({
  modelValue: { type: Array, default: () => [] },
  max: { type: Number, default: 9 }
})

const emit = defineEmits(['update:modelValue'])

const fileInput = ref(null)
const uploading = ref(false)
const uploadProgress = ref(0)

function triggerUpload() {
  fileInput.value?.click()
}

async function handleFileChange(e) {
  const files = Array.from(e.target.files || [])
  if (!files.length) return
  if (props.modelValue.length + files.length > props.max) {
    ElMessage.warning(`最多上传 ${props.max} 张图片`)
    return
  }
  uploading.value = true
  uploadProgress.value = 0
  const urls = [...props.modelValue]
  for (let i = 0; i < files.length; i++) {
    try {
      const res = await fileAPI.upload(files[i])
      urls.push(res.data.data || res.data)
      uploadProgress.value = Math.round(((i + 1) / files.length) * 100)
    } catch {
      ElMessage.error(`上传 ${files[i].name} 失败`)
    }
  }
  emit('update:modelValue', urls)
  uploading.value = false
  fileInput.value.value = ''
}

function remove(idx) {
  const urls = [...props.modelValue]
  urls.splice(idx, 1)
  emit('update:modelValue', urls)
}

function moveUp(idx) {
  if (idx <= 0) return
  const urls = [...props.modelValue]
  ;[urls[idx], urls[idx - 1]] = [urls[idx - 1], urls[idx]]
  emit('update:modelValue', urls)
}

function moveDown(idx) {
  if (idx >= props.modelValue.length - 1) return
  const urls = [...props.modelValue]
  ;[urls[idx], urls[idx + 1]] = [urls[idx + 1], urls[idx]]
  emit('update:modelValue', urls)
}
</script>

<style scoped>
.upload-grid {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.image-item {
  position: relative;
  width: 100px;
  height: 100px;
  border-radius: var(--radius-sm);
  overflow: hidden;
}

.image-item img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.image-actions {
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  display: flex;
  justify-content: center;
  gap: 4px;
  padding: 4px;
  background: rgba(0, 0, 0, 0.5);
  opacity: 0;
  transition: opacity var(--transition-fast);
}

.image-item:hover .image-actions { opacity: 1; }

.upload-trigger {
  width: 100px;
  height: 100px;
  border: 2px dashed var(--color-border);
  border-radius: var(--radius-sm);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 4px;
  color: var(--color-text-muted);
  cursor: pointer;
  transition: all var(--transition-fast);
}

.upload-trigger:hover {
  border-color: var(--color-primary);
  color: var(--color-primary);
}
</style>
