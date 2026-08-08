import { ref, watch } from 'vue'

const THEME_KEY = 'furniture_theme'
const currentTheme = ref(localStorage.getItem(THEME_KEY) || 'light')

/**
 * 主题管理 composable
 *
 * 功能：
 * - 读取/写入 localStorage 持久化主题偏好
 * - 切换 <html> 上的 data-theme 属性
 * - 切换时启用过渡动画
 */
export function useTheme() {
  const isDark = ref(currentTheme.value === 'dark')

  /**
   * 应用主题到 DOM
   */
  function applyTheme(theme) {
    // 添加过渡标记，使颜色切换平滑
    document.documentElement.setAttribute('data-theme-transition', '')
    document.documentElement.setAttribute('data-theme', theme)
    // 过渡完成后移除标记
    setTimeout(() => {
      document.documentElement.removeAttribute('data-theme-transition')
    }, 400)
  }

  /**
   * 切换主题（亮色 ⇄ 深色）
   */
  function toggleTheme() {
    const next = isDark.value ? 'light' : 'dark'
    isDark.value = !isDark.value
    currentTheme.value = next
    localStorage.setItem(THEME_KEY, next)
    applyTheme(next)
  }

  /**
   * 设置指定主题
   */
  function setTheme(theme) {
    if (theme !== 'light' && theme !== 'dark') return
    isDark.value = theme === 'dark'
    currentTheme.value = theme
    localStorage.setItem(THEME_KEY, theme)
    applyTheme(theme)
  }

  // 初始化：页面加载时立即应用主题，避免闪烁
  applyTheme(currentTheme.value)

  return {
    isDark,
    currentTheme,
    toggleTheme,
    setTheme
  }
}
