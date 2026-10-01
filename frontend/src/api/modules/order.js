import request from '../request'

export const orderAPI = {
  create: (addressId, cartItemIds) =>
    request.post('/orders', null, { params: { addressId, cartItemIds } }),
  pay: orderNo => request.post('/orders/pay', null, { params: { orderNo } }),
  getList: params => request.get('/orders', { params }),
  // 各状态订单数，供订单页签角标使用；与 getList 传同一套筛选条件（不含 status），保证两者口径一致
  getStatusCounts: params => request.get('/orders/stats', { params }),
  getDetail: orderNo => request.get(`/orders/${orderNo}`),
  cancel: orderNo => request.post('/orders/cancel', null, { params: { orderNo } }),
  confirmReceipt: orderNo => request.post('/orders/receive', null, { params: { orderNo } })
}
