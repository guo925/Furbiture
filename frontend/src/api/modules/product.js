import request from '../request'

export const productAPI = {
  getList: (params) => request.get('/products', { params }),
  getDetail: (id) => request.get(`/products/${id}`),
  search: (keyword) => request.get('/products/search', { params: { keyword } })
}

export const categoryAPI = {
  getList: () => request.get('/categories'),
  getTree: () => request.get('/categories/tree')
}
