import axios from 'axios'
import { useUserStore } from '../stores/user'

// 创建axios实例
const api = axios.create({
  baseURL: '/api',
  timeout: 10000
})

// 请求拦截器
api.interceptors.request.use(
  config => {
    const token = localStorage.getItem('furniture_token')
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  error => {
    return Promise.reject(error)
  }
)

// 响应拦截器
api.interceptors.response.use(
  response => {
    return response
  },
  error => {
    if (error.response?.status === 401) {
      // 直接操作localStorage，避免创建新的store实例
      localStorage.removeItem('furniture_token')
      localStorage.removeItem('furniture_user')
      window.location.href = '/login'
    }
    return Promise.reject(error)
  }
)

// API接口
export const authAPI = {
  login: (data) => api.post('/auth/login', data),
  register: (data) => api.post('/auth/register', data),
  getCurrentUser: () => api.get('/users/current')
}

export const fileAPI = {
  upload: (file) => {
    const formData = new FormData()
    // 确保传递的是真实的File对象
    const actualFile = file.raw || file
    formData.append('file', actualFile)
    return api.post('/files/upload', formData, {
      headers: {
        'Content-Type': 'multipart/form-data'
      },
      // 确保axios不会尝试序列化FormData
      transformRequest: [function (data) {
        return data
      }]
    })
  }
}

export const productAPI = {
  getList: (params) => api.get('/products', { params }),
  getDetail: (id) => api.get(`/products/${id}`),
  getCategories: () => api.get('/categories'),
  getCategoryTree: () => api.get('/categories/tree')
}

export const cartAPI = {
  getList: () => api.get('/carts'),
  add: (data) => api.post('/carts', data),
  update: (id, data) => api.put(`/carts/${id}`, data),
  remove: (id) => api.delete(`/carts/${id}`),
  clear: () => api.delete('/carts')
}

export const orderAPI = {
  create: (addressId, cartItemIds) => api.post('/orders', null, {
    params: { addressId, cartItemIds }
  }),
  pay: (orderNo) => api.post('/orders/pay', null, {
    params: { orderNo }
  }),
  getList: (params) => api.get('/orders', { params }),
  getDetail: (orderNo) => api.get(`/orders/${orderNo}`),
  cancel: (orderNo) => api.post('/orders/cancel', null, {
    params: { orderNo }
  }),
  confirmReceipt: (orderNo) => api.post('/orders/receive', null, {
    params: { orderNo }
  }),
  getOrderStats: () => api.get('/orders/stats')
}

export const addressAPI = {
  getList: () => api.get('/addresses'),
  create: (data) => api.post('/addresses', data),
  update: (id, data) => api.put(`/addresses/${id}`, data),
  remove: (id) => api.delete(`/addresses/${id}`),
  setDefault: (id) => api.put(`/addresses/${id}/default`)
}

// 用户端API
export const userAPI = {
  getCurrentUser: () => api.get('/users/current'),
  updateUser: (data) => api.put('/users', data),
  changePassword: (data) => api.post('/users/password', data)
}

// 管理端API
export const adminAPI = {
  // 仪表板
  dashboard: {
    getStats: () => api.get('/admin/dashboard'),
    getSalesTrend: () => api.get('/admin/dashboard/sales-trend'),
    getHotProducts: () => api.get('/admin/dashboard/hot-products')
  },
  // 商品管理
  products: {
    getList: (params) => api.get('/admin/products', { params }),
    create: (data) => api.post('/admin/products', data),
    update: (id, data) => api.put(`/admin/products/${id}`, data),
    delete: (id) => api.delete(`/admin/products/${id}`),
    updateStatus: (id, status) => api.put(`/admin/products/${id}/${status ? 'publish' : 'unpublish'}`)
  },
  // 分类管理
  categories: {
    getList: (params) => api.get('/admin/categories', { params }),
    getTree: () => api.get('/admin/categories/tree'),
    create: (data) => api.post('/admin/categories', data),
    update: (id, data) => api.put(`/admin/categories/${id}`, data),
    delete: (id) => api.delete(`/admin/categories/${id}`)
  },
  // 订单管理
  orders: {
    getList: (params) => api.get('/admin/orders', { params }),
    getDetail: (id) => api.get(`/admin/orders/${id}`),
    updateStatus: (id, status) => api.put(`/admin/orders/${id}/status`, { status })
  },
  // 用户管理
  users: {
    getList: (params) => api.get('/admin/users', { params }),
    create: (data) => api.post('/admin/users', data),
    update: (id, data) => api.put(`/admin/users/${id}`, data),
    delete: (id) => api.delete(`/admin/users/${id}`),
    resetPassword: (id, password) => api.post(`/admin/users/${id}/reset-password`, { password })
  }
}

// 商家管理端API
export const merchantAPI = {
  // 仪表板
  dashboard: {
    getStats: () => api.get('/merchant/dashboard')
  },
  // 商品管理
  products: {
    getList: (params) => api.get('/merchant/products', { params }),
    create: (data) => api.post('/merchant/products', data),
    update: (id, data) => api.put(`/merchant/products/${id}`, data),
    delete: (id) => api.delete(`/merchant/products/${id}`)
  },
  // 分类管理
  categories: {
    getList: (params) => api.get('/merchant/categories', { params }),
    create: (data) => api.post('/merchant/categories', data),
    update: (id, data) => api.put(`/merchant/categories/${id}`, data),
    delete: (id) => api.delete(`/merchant/categories/${id}`)
  },
  // 订单管理
  orders: {
    getList: (params) => api.get('/merchant/orders', { params }),
    getDetail: (orderNo) => api.get(`/merchant/orders/${orderNo}`),
    updateStatus: (orderNo, status) => api.put(`/merchant/orders/${orderNo}/status`, { status })
  },
  // 商家信息
  info: {
    get: () => api.get('/merchant/info'),
    updateProfile: (data) => api.put('/merchant/info', data),
    changePassword: (data) => api.post('/merchant/info/password', data)
  }
}

export default api