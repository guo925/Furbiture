import axios from 'axios'
import { ElMessage } from 'element-plus'

// 创建axios实例
const request = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api',
  timeout: 15000
})

/**
 * 401 处理的并发闸门。
 *
 * 首屏常同时发出多个请求（例如仪表板一次拉 5 个统计接口），若 token 失效，
 * 多个响应会在极短时间内先后返回 401。没有闸门时这段清理 + 跳转逻辑会被执行
 * N 次：弹 N 条提示、跳转 N 次，且相互打断。
 *
 * 闸门语义：第一个 401 把标志置为 true 并开始处理，同一批里后续的 401 看到
 * 标志后直接跳过（但**仍向下 reject**，避免调用方拿到 undefined 当成功处理）。
 * 跳转完成后在 finally 中复位，保证下一次会话过期时仍能再次生效。
 */
let isHandling401 = false

/**
 * 会话过期时的统一处理：清理登录态 → 带 redirect 跳登录页。
 *
 * 用动态 import 获取 router / store，而不是顶层静态 import：
 * request.js 被 stores/user.js（经 api 模块）间接引用，静态 import router 会形成
 * request → router → stores/user → api → request 的循环。动态 import 在调用时
 * 才求值，此时各模块均已初始化完毕，规避循环依赖。
 */
async function handleUnauthorized() {
  try {
    const [{ default: router }, { useUserStore }] = await Promise.all([
      import('../router'),
      import('../stores/user')
    ])

    // 走 store 的 logout，同步清空 Pinia 内存态（token / user）与 localStorage，
    // 避免"localStorage 清了但内存里还是旧用户"的脏状态。
    useUserStore().logout()

    const current = router.currentRoute.value
    // 已在登录页则不再重复跳转（吞掉同批 401 的尾随响应）
    if (current.name === 'Login') return

    // 带上原始地址（fullPath 含 path + query + hash），登录成功后由 LoginView 跳回。
    // 交给 vue-router 序列化 query，它会自动做 URL 编码，不会截断带参数的地址。
    await router.replace({
      name: 'Login',
      query: { redirect: current.fullPath }
    })
  } catch (err) {
    console.error('401 处理失败:', err?.message)
  } finally {
    isHandling401 = false
  }
}

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
        // 并发去重：同批 401 只处理一次
        if (isHandling401) break
        isHandling401 = true
        ElMessage.warning('登录已过期，请重新登录')
        // 不 await：拦截器保持同步返回 reject，跳转在后台完成
        handleUnauthorized()
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
