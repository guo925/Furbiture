import request from '../request'

export const userAPI = {
  getCurrentUser: () => request.get('/users/current'),
  updateUser: (data) => request.put('/users', data),
  changePassword: (data) => request.post('/users/password', data)
}
