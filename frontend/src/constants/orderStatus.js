/**
 * 订单状态单一数据源
 *
 * 此前 admin/OrdersView.vue 与 merchant/MerchantOrders.vue 各维护一份状态映射，
 * 且文案/index 不一致（status 1 在 admin 叫「已付款」、在商家端叫「待发货」），
 * admin 的筛选下拉还漏了「已退款(5)」。这里统一收敛，两个视图都改为引用本文件。
 *
 * 状态码语义（与后端 OrderStatusEnum 对齐）：
 *   0 待付款 · 1 已付款（待发货）· 2 已发货 · 3 已完成 · 4 已取消 · 5 已退款
 *
 * 取值说明：
 * - label：中文文案，三端统一采用「动作视角」措辞（status 1 = 待发货），
 *   因为商家端与用户端都需要看到"还需我做什么"。
 * - type：Element Plus `el-tag` 的 type（'primary' | 'success' | 'warning' | 'info' | 'danger'）。
 */
export const ORDER_STATUS = Object.freeze({
  0: { label: '待付款', type: 'warning' },
  1: { label: '待发货', type: 'primary' },
  2: { label: '已发货', type: 'success' },
  3: { label: '已完成', type: 'success' },
  4: { label: '已取消', type: 'info' },
  5: { label: '已退款', type: 'danger' }
})

/** 未知状态兜底（仅当后端返回 0~5 之外的状态码时出现） */
export const UNKNOWN_ORDER_STATUS = Object.freeze({ label: '未知', type: 'info' })

/**
 * 状态元信息查询：文案 + tag 颜色
 * @param {number|string} status
 * @returns {{ label: string, type: string }}
 */
export function getOrderStatusMeta(status) {
  return ORDER_STATUS[status] || UNKNOWN_ORDER_STATUS
}

/**
 * 下拉框 / 页签可选项（不含「全部」，由各视图自行追加）
 * 顺序按状态码升序，覆盖 0~5 全部状态（含此前遗漏的 5 已退款）。
 * @type {{ value: number, label: string }[]}
 */
export const ORDER_STATUS_OPTIONS = Object.keys(ORDER_STATUS).map(key => ({
  value: Number(key),
  label: ORDER_STATUS[key].label
}))
