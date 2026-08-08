// 向后兼容：从新的模块化 API 重新导出
// 建议在新代码中直接从 api/modules/* 导入
export {
  authAPI,
  fileAPI,
  productAPI,
  categoryAPI,
  cartAPI,
  orderAPI,
  addressAPI,
  userAPI,
  adminAPI,
  merchantAPI,
  request as default
} from './modules/index'
