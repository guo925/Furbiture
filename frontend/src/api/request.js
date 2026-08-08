import axios from 'axios'
import { ElMessage } from 'element-plus'

// 创建axios实例
const request = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api',
  timeout: 15000
})

// 请求拦截器
request.interceptors.request.use(
  config => {
    const token = localStorage.getItem('furniture_token')
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  error => Promise.reject(error)
)

// 响应拦截器
request.interceptors.response.use(
  response => {
    // 兼容部分接口仍返回 HTTP 200 + code != 200 的旧模式
    const data = response.data
    if (data && typeof data.code === 'number' && data.code !== 200) {
      ElMessage.error(data.msg || '请求失败')
      return Promise.reject(new Error(data.msg || '请求失败'))
    }
    return response
  },
  error => {
    if (!error.response) {
      ElMessage.error('网络连接异常，请检查网络')
      return Promise.reject(error)
    }
    const { status, data } = error.response
    switch (status) {
      case 400:
        ElMessage.error(data?.msg || '请求参数有误')
        break
      case 401:
        localStorage.removeItem('furniture_token')
        localStorage.removeItem('furniture_user')
        window.location.href = '/login'
        break
      case 403:
        ElMessage.error('权限不足，无法执行此操作')
        break
      case 404:
        ElMessage.error(data?.msg || '请求的资源不存在')
        break
      case 409:
        ElMessage.error(data?.msg || '业务操作冲突')
        break
      case 500:
        ElMessage.error(data?.msg || '服务器内部错误，请稍后重试')
        break
      default:
        ElMessage.error(data?.msg || `请求失败 (${status})`)
    }
    return Promise.reject(error)
  }
)

export default request
