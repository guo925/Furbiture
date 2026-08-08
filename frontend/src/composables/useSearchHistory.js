const HISTORY_KEY = 'furniture_search_history'
const MAX_HISTORY = 10

/**
 * 搜索历史 composable
 *
 * 基于 localStorage 存储用户搜索关键词，支持去重和数量限制。
 */
export function useSearchHistory() {
  function getHistory() {
    try {
      const raw = localStorage.getItem(HISTORY_KEY)
      return raw ? JSON.parse(raw) : []
    } catch {
      return []
    }
  }

  function saveHistory(list) {
    localStorage.setItem(HISTORY_KEY, JSON.stringify(list))
  }

  /** 添加关键词到搜索历史（去重，最新的排在最前） */
  function addSearch(keyword) {
    if (!keyword || !keyword.trim()) return
    const kw = keyword.trim()
    const list = getHistory().filter(item => item !== kw)
    list.unshift(kw)
    if (list.length > MAX_HISTORY) list.pop()
    saveHistory(list)
  }

  /** 清除全部搜索历史 */
  function clearHistory() {
    localStorage.removeItem(HISTORY_KEY)
  }

  /** 删除单条搜索历史 */
  function removeSearch(keyword) {
    saveHistory(getHistory().filter(item => item !== keyword))
  }

  return { getHistory, addSearch, clearHistory, removeSearch }
}
