import { ElMessageBox } from 'element-plus'

/**
 * 统一的确认弹窗封装
 *
 * 为什么需要它：
 * `ElMessageBox.confirm()` 在用户点「取消」或关闭弹窗时会 **reject**（拒绝值是字符串
 * `'cancel'` / `'close'`）。若调用方没有 catch，就会在控制台抛
 * `Uncaught (in promise)` —— 这不是业务错误，只是用户点了「取消」。
 * 模板里 `@click="handleDelete(row)"` 直接调用 async 方法时尤其容易漏接。
 *
 * 于是把「确认 → 布尔值」收敛到一处：调用方只需
 *
 *   if (!await confirm('确定删除吗？', '删除确认')) return
 *   await api.delete(id)
 *
 * 取消被静默吞掉（返回 false），永不再抛未捕获的 Promise 拒绝。
 *
 * @param {string} message 弹窗正文
 * @param {string} [title='提示'] 弹窗标题
 * @returns {Promise<boolean>} 用户确认返回 true，取消/关闭返回 false
 */
export async function confirm(message, title = '提示') {
  try {
    await ElMessageBox.confirm(message, title, { type: 'warning' })
    return true
  } catch {
    // 用户取消：EP 以 'cancel' / 'close' 拒绝，属正常交互，静默处理
    return false
  }
}
