import request from '../request'

export const merchantAPI = {
  dashboard: {
    getStats: () => request.get('/merchant/dashboard'),
    getRevenueTrend: () => request.get('/merchant/dashboard/revenue-trend'),
    getCategoryDistribution: () => request.get('/merchant/dashboard/category-distribution'),
    getOrderFunnel: () => request.get('/merchant/dashboard/order-funnel'),
    getTopProducts: () => request.get('/merchant/dashboard/top-products')
  },
  products: {
    getList: (params) => request.get('/merchant/products', { params }),
    create: (data) => request.post('/merchant/products', data),
    update: (id, data) => request.put(`/merchant/products/${id}`, data),
    delete: (id) => request.delete(`/merchant/products/${id}`)
  },
  categories: {
    getList: (params) => request.get('/merchant/categories', { params }),
    create: (data) => request.post('/merchant/categories', data),
    update: (id, data) => request.put(`/merchant/categories/${id}`, data),
    delete: (id) => request.delete(`/merchant/categories/${id}`)
  },
  orders: {
    getList: (params) => request.get('/merchant/orders', { params }),
    getDetail: (orderNo) => request.get(`/merchant/orders/${orderNo}`),
    updateStatus: (orderNo, status) => request.put(`/merchant/orders/${orderNo}/status`, { status })
  },
  info: {
    get: () => request.get('/merchant/info'),
    updateProfile: (data) => request.put('/merchant/info', data),
    changePassword: (data) => request.post('/merchant/info/password', data)
  }
}
