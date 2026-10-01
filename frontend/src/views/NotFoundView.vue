<template>
  <div class="not-found">
    <div class="not-found__inner">
      <div class="not-found__code">404</div>
      <h1 class="not-found__title">页面走丢了</h1>
      <p class="not-found__desc">你访问的地址不存在，或者已经被移除了。</p>
      <div class="not-found__actions">
        <el-button type="primary" @click="goHome">返回首页</el-button>
        <el-button @click="goBack">返回上一页</el-button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { useRouter } from 'vue-router'

const router = useRouter()

const goHome = () => {
  router.push('/home')
}

const goBack = () => {
  // history.length 为 1 说明没有可回退的历史（直接打开链接），此时回首页兜底
  if (window.history.length > 1) {
    router.back()
  } else {
    router.push('/home')
  }
}
</script>

<style scoped>
.not-found {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--color-bg-primary, #f5f7fa);
  padding: 24px;
}

.not-found__inner {
  text-align: center;
}

.not-found__code {
  font-size: 96px;
  font-weight: 700;
  line-height: 1;
  color: var(--color-primary, #ff5000);
}

.not-found__title {
  margin: 16px 0 8px;
  font-size: 24px;
  color: var(--color-text-primary, #333);
}

.not-found__desc {
  margin: 0 0 28px;
  color: var(--color-text-secondary, #666);
}

.not-found__actions {
  display: flex;
  gap: 12px;
  justify-content: center;
}
</style>
