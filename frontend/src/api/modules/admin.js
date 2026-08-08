import request from '../request'

export const adminAPI = {
  dashboard: {
    getStats: () => request.get('/admin/dashboard'),
    getSalesTrend: () => request.get('/admin/dashboard/sales-trend'),
    getHotProducts: () => request.get('/admin/dashboard/hot-products')
  },
  products: {
    getList: (params) => request.get('/admin/products', { params }),
    create: (data) => request.post('/admin/products', data),
    update: (id, data) => request.put(`/admin/products/${id}`, data),
    delete: (id) => request.delete(`/admin/products/${id}`),
    updateStatus: (id, status) => request.put(`/admin/products/${id}/${status ? 'publish' : 'unpublish'}`)
  },
  categories: {
    getList: (params) => request.get('/admin/categories', { params }),
    getTree: () => request.get('/admin/categories/tree'),
    create: (data) => request.post('/admin/categories', data),
    update: (id, data) => request.put(`/admin/categories/${id}`, data),
    delete: (id) => request.delete(`/admin/categories/${id}`)
  },
  orders: {
    getList: (params) => request.get('/admin/orders', { params }),
    getDetail: (id) => request.get(`/admin/orders/${id}`),
    updateStatus: (id, status) => request.put(`/admin/orders/${id}/status`, { status })
  },
  users: {
    getList: (params) => request.get('/admin/users', { params }),
    create: (data) => request.post('/admin/users', data),
    update: (id, data) => request.put(`/admin/users/${id}`, data),
    delete: (id) => request.delete(`/admin/users/${id}`),
    resetPassword: (id, password) => request.post(`/admin/users/${id}/reset-password`, { password })
  }
}
