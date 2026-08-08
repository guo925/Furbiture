import request from '../request'

export const authAPI = {
  login: (data) => request.post('/auth/login', data),
  register: (data) => request.post('/auth/register', data),
  getCurrentUser: () => request.get('/users/current')
}
