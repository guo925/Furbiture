import request from '../request'

export const orderAPI = {
  create: (addressId, cartItemIds) => request.post('/orders', null, { params: { addressId, cartItemIds } }),
  pay: (orderNo) => request.post('/orders/pay', null, { params: { orderNo } }),
  getList: (params) => request.get('/orders', { params }),
  getDetail: (orderNo) => request.get(`/orders/${orderNo}`),
  cancel: (orderNo) => request.post('/orders/cancel', null, { params: { orderNo } }),
  confirmReceipt: (orderNo) => request.post('/orders/receive', null, { params: { orderNo } })
}
