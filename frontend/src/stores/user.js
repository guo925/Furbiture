import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { authAPI } from '../api'

const USER_KEY = 'furniture_user'
const TOKEN_KEY = 'furniture_token'

function safeParseJSON(str, defaultValue = null) {
  if (!str || str === 'undefined' || str === 'null') {
    return defaultValue
  }
  try {
    return JSON.parse(str)
  } catch {
    return defaultValue
  }
}

export const useUserStore = defineStore('user', () => {
  const token = ref(localStorage.getItem(TOKEN_KEY) || '')
  const user = ref(safeParseJSON(localStorage.getItem(USER_KEY)))

  const isAuthenticated = computed(() => !!token.value && !!user.value)
  const isAdmin = computed(() => user.value && user.value.role === 'ADMIN')
  const isMerchant = computed(() => user.value && user.value.role === 'MERCHANT')

  // 登录
  const login = async (username, password) => {
    try {
      const response = await authAPI.login({ username, password })

      // 检查业务响应码（后端成功返回code=200）
      if (response.data && response.data.code !== 200) {
        throw new Error(response.data.msg || '登录失败')
      }
      
      // 处理响应数据（支持不同格式）
      let resultData = null
      if (response.data && response.data.data) {
        resultData = response.data.data
      } else if (response.data && response.data.token) {
        resultData = response.data
      }

      // 检查返回数据
      if (!resultData) {
        throw new Error('登录返回数据为空')
      }
      if (!resultData.token) {
        throw new Error('登录返回数据缺少token')
      }
      if (!resultData.user) {
        throw new Error('登录返回数据缺少user')
      }

      token.value = resultData.token
      user.value = resultData.user
      localStorage.setItem(TOKEN_KEY, resultData.token)
      localStorage.setItem(USER_KEY, JSON.stringify(resultData.user))
      return resultData
    } catch (error) {
      console.error('登录错误:', error?.message)
      if (error.response?.data) {
        throw new Error(error.response.data.msg || error.response.data.message || '登录失败')
      } else {
        throw new Error(error.message || '登录失败')
      }
    }
  }

  // 注册
  const register = async (userData) => {
    try {
      const response = await authAPI.register(userData)
      return response.data
    } catch (error) {
      if (error.response?.data) {
        throw new Error(error.response.data.msg || '注册失败')
      } else {
        throw new Error(error.message || '注册失败')
      }
    }
  }

  // 登出
  const logout = () => {
    token.value = ''
    user.value = null
    localStorage.removeItem(TOKEN_KEY)
    localStorage.removeItem(USER_KEY)
  }

  // 获取当前用户信息
  const getCurrentUser = async () => {
    try {
      const response = await authAPI.getCurrentUser()
      user.value = response.data.data
      localStorage.setItem(USER_KEY, JSON.stringify(user.value))
      return user.value
    } catch (error) {
      if (error.response?.data) {
        throw new Error(error.response.data.msg || '获取用户信息失败')
      } else {
        throw new Error(error.message || '获取用户信息失败')
      }
    }
  }

  // 更新用户信息
  const updateUserInfo = (updatedInfo) => {
    if (user.value) {
      user.value = { ...user.value, ...updatedInfo }
      localStorage.setItem(USER_KEY, JSON.stringify(user.value))
    }
  }

  return {
    token,
    user,
    isAuthenticated,
    isAdmin,
    isMerchant,
    login,
    register,
    logout,
    getCurrentUser,
    updateUserInfo
  }
})