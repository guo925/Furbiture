<template>
  <div class="file-upload-test">
    <h2>文件上传测试</h2>
    <el-upload
      class="upload-demo"
      action=""
      :http-request="handleUpload"
      :auto-upload="true"
      accept="image/*"
      :show-file-list="false"
    >
      <el-button type="primary">点击上传</el-button>
      <template #tip>
        <div class="el-upload__tip">
          只能上传图片文件，且不超过10MB
        </div>
      </template>
    </el-upload>
    <div v-if="fileUrl" style="margin-top: 20px">
      <h3>上传成功！</h3>
      <img :src="fileUrl" alt="上传的图片" style="max-width: 300px; margin-top: 10px">
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { fileAPI } from '../../api'
import { ElMessage } from 'element-plus'

const fileUrl = ref('')

const handleUpload = async (options) => {
  const file = options.file
  try {
    console.log('上传的文件:', file)
    const response = await fileAPI.upload(file)
    console.log('上传响应:', response)
    if (response.data && response.data.data) {
      fileUrl.value = response.data.data
      ElMessage.success('图片上传成功')
      options.onSuccess()
    } else {
      ElMessage.error('上传失败：无效的响应')
      options.onError()
    }
  } catch (error) {
    ElMessage.error('图片上传失败')
    console.error('上传失败:', error)
    options.onError()
  }
}
</script>

<style scoped>
.file-upload-test {
  background: #fff;
  padding: 20px;
  border-radius: 8px;
  box-shadow: 0 2px 12px rgba(0,0,0,0.1);
  margin: 20px;
}

.file-upload-test h2 {
  margin-top: 0;
  font-size: 20px;
  font-weight: bold;
  color: #333;
  margin-bottom: 20px;
}
</style>