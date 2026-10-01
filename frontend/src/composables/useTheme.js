import { ref, computed } from 'vue'

const THEME_KEY = 'furniture_theme'

// 模块级单例状态：所有组件共享同一份主题。
// currentTheme 与 isDark 都必须是模块级的，否则各组件各自持有一份，
// A 组件切主题时 B 组件的 isDark 不会更新（三个布局互斥挂载时看不出问题，
// 一旦同屏出现两个消费方就是 bug）。
const currentTheme = ref(localStorage.getItem(THEME_KEY) || 'light')
const isDark = computed(() => currentTheme.value === 'dark')

// 过渡定时器句柄：模块级，重设前先清掉，避免多个 400ms 定时器叠加、
// 也避免旧定时器把新切换刚加上的过渡标记提前移除。
let transitionTimer = null

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
  if (transitionTimer) {
    clearTimeout(transitionTimer)
  }
  // 添加过渡标记，使颜色切换平滑
  root.setAttribute('data-theme-transition', '')
  root.setAttribute('data-theme', theme)
  root.classList.toggle('dark', theme === 'dark')
  // 过渡完成后移除标记
  transitionTimer = setTimeout(() => {
    root.removeAttribute('data-theme-transition')
    transitionTimer = null
  }, 400)
}

// 初始化：页面加载时立即应用一次，避免闪烁。
// 模块级执行——原先放在 useTheme() 内部，导致每个调用方都重复设 DOM + 起定时器。
applyTheme(currentTheme.value)

/**
 * 主题管理 composable
 *
 * 功能：
 * - 读取/写入 localStorage 持久化主题偏好
 * - 切换 <html> 上的 data-theme 属性
 * - 切换时启用过渡动画
 */
export function useTheme() {
  /**
   * 切换主题（亮色 ⇄ 深色）
   */
  function toggleTheme() {
    const next = isDark.value ? 'light' : 'dark'
    currentTheme.value = next
    localStorage.setItem(THEME_KEY, next)
    applyTheme(next)
  }

  /**
   * 设置指定主题
   */
  function setTheme(theme) {
    if (theme !== 'light' && theme !== 'dark') return
    currentTheme.value = theme
    localStorage.setItem(THEME_KEY, theme)
    applyTheme(theme)
  }

  return {
    isDark,
    currentTheme,
    toggleTheme,
    setTheme
  }
}
