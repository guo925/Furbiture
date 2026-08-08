import { ref, reactive, watch } from 'vue'

/**
 * 通用分页 Composable
 * 封装分页查询的通用逻辑，减少页面中的重复代码
 * 
 * @param {Function} fetchFn - 数据获取函数，接收 { page, size } 参数
 * @param {number} defaultSize - 默认每页条数
 * @returns 分页相关的状态和方法
 */
export function usePagination(fetchFn, defaultSize = 10) {
  const list = ref([])
  const total = ref(0)
  const currentPage = ref(1)
  const pageSize = ref(defaultSize)
  const loading = ref(false)

  const loadData = async (params = {}) => {
    loading.value = true
    try {
      const response = await fetchFn({
        page: currentPage.value,
        size: pageSize.value,
        ...params
      })
      const data = response.data?.data || response.data
      list.value = data.records || []
      total.value = data.total || 0
    } catch (error) {
      console.error('加载数据失败:', error)
      list.value = []
      total.value = 0
    } finally {
      loading.value = false
    }
  }

  const handlePageChange = (page) => {
    currentPage.value = page
    loadData()
  }

  const handleSizeChange = (size) => {
    pageSize.value = size
    currentPage.value = 1
    loadData()
  }

  const refresh = () => {
    currentPage.value = 1
    loadData()
  }

  return {
    list,
    total,
    currentPage,
    pageSize,
    loading,
    loadData,
    handlePageChange,
    handleSizeChange,
    refresh
  }
}
