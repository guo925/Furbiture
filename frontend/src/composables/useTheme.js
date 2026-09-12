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
   *
   * 需要同时设置两套标记，因为它们驱动的样式体系不同：
   * - data-theme：项目自有令牌（design-tokens.css 的 --color-*）
   * - dark 类：Element Plus 组件（EP 的暗色变量挂在 html.dark 下）
   * 只设其一会出现"页面背景变深、EP 表格/弹窗仍是白色"的割裂。
   */
  function applyTheme(theme) {
    const root = document.documentElement
    // 添加过渡标记，使颜色切换平滑
    root.setAttribute('data-theme-transition', '')
    root.setAttribute('data-theme', theme)
    root.classList.toggle('dark', theme === 'dark')
    // 过渡完成后移除标记
    setTimeout(() => {
      root.removeAttribute('data-theme-transition')
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
