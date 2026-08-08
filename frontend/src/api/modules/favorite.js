import request from '../request'

export const favoriteAPI = {
  /** 切换收藏状态 */
  toggle(productId) {
    return request.post(`/favorites/${productId}`)
  },

  /** 检查是否已收藏 */
  check(productId) {
    return request.get(`/favorites/check/${productId}`)
  },

  /** 获取收藏列表 */
  getList() {
    return request.get('/favorites')
  }
}
