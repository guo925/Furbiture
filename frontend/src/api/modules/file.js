import request from '../request'

export const fileAPI = {
  upload: (file) => {
    const formData = new FormData()
    formData.append('file', file.raw || file)
    return request.post('/files/upload', formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
      transformRequest: [data => data]
    })
  }
}
