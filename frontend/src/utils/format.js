/**
 * 全项目统一的展示格式化工具
 *
 * 此前 `money` / 日期格式化在 8+ 个视图里各复制了一份，行为靠人工保持一致，
 * 一旦某处改了精度就会漂移。这里收敛为单一来源。
 */

/**
 * 金额格式化：保留两位小数
 *
 * 兼容 null / undefined / 空字符串 / 非数字（统归为 0.00），与各页面原有实现逐字一致：
 * 原实现均为 `Number(value || 0).toFixed(2)`，故此处行为不变、显示结果不变。
 *
 * @param {number|string|null|undefined} value
 * @returns {string} 例如 128 -> '128.00'
 */
export const money = value => Number(value || 0).toFixed(2)

/**
 * 日期时间格式化：精确到分钟（'YYYY-MM-DD HH:mm'）
 *
 * 后端返回的是 ISO 字符串（含 'T'），这里仅做「把 T 换成空格 + 截断」的轻量处理，
 * 不引入 dayjs 依赖（与项目现状一致）。
 *
 * @param {string|null|undefined} time
 * @returns {string} 空值返回 '-'
 */
export const formatDateTime = time => (time ? String(time).replace('T', ' ').slice(0, 16) : '-')

/**
 * 日期时间格式化：精确到秒（'YYYY-MM-DD HH:mm:ss'）
 *
 * 与 {@link formatDateTime} 的唯一区别是截断长度。订单页需要到秒，列表页到分钟即可，
 * 保留两个函数以免改动既有页面的显示粒度。
 *
 * @param {string|null|undefined} value
 * @returns {string} 空值返回 '-'
 */
export const formatDate = value => (value ? String(value).replace('T', ' ').slice(0, 19) : '-')
