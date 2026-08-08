import request from '../request'

export const cartAPI = {
  getList: () => request.get('/carts'),
  add: (data) => request.post('/carts', data),
  update: (id, data) => request.put(`/carts/${id}`, data),
  remove: (id) => request.delete(`/carts/${id}`),
  clear: () => request.delete('/carts')
}
