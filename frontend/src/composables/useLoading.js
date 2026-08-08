import { ref } from 'vue'

/**
 * 全局加载状态管理
 * 用于 API 请求时的 loading 状态
 */
const globalLoading = ref(false)
const loadingCount = ref(0)

export function useLoading() {
  const startLoading = () => {
    loadingCount.value++
    globalLoading.value = true
  }

  const stopLoading = () => {
    loadingCount.value = Math.max(0, loadingCount.value - 1)
    if (loadingCount.value === 0) {
      globalLoading.value = false
    }
  }

  return {
    globalLoading,
    startLoading,
    stopLoading
  }
}
