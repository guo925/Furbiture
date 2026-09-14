import { ref, computed, onMounted, onUnmounted } from 'vue'

/**
 * 全项目统一断点
 *
 * 收敛为三档（见 docs/frontend-optimization.md 批次 B）：
 * - 移动端：≤ 640px
 * - 平板：  641 ~ 1024px
 * - 桌面：  > 1024px
 *
 * 约定：CSS 侧媒体查询统一写 `@media (max-width: 640px)` 与 `@media (max-width: 1024px)`，
 * 与本文件的两个数值一一对应。新增断点前先想清楚是不是真有必要——
 * 项目此前出现过 12 个各写各的断点值，正是从"就这一个页面特殊"开始的。
 */
export const BREAKPOINTS = Object.freeze({
  /** 移动端上限 */
  mobile: 640,
  /** 窄屏上限：带固定侧边栏的布局（管理端）在此宽度转为抽屉式导航 */
  narrow: 1024
})

/**
 * 响应式断点 composable
 *
 * 内部用 matchMedia 而不是 window resize 事件：
 * matchMedia 只在**跨过断点的那一刻**触发一次回调，拖拽窗口时不会产生成百上千次
 * 无意义的响应式更新（resize 每像素都触发）。这是断点判断场景下的标准做法。
 *
 * 使用：
 *   const { isNarrow, isMobile } = useBreakpoint()
 *
 * 仅客户端可用：直接依赖 window.matchMedia。本项目是纯 Vite SPA（无 SSR/预渲染），
 * 若将来引入 SSR 或 jsdom 测试环境，需要在这里补 window 缺失时的降级分支。
 *
 * @returns {{ isNarrow: import('vue').Ref<boolean>, isMobile: import('vue').Ref<boolean>, isTablet: import('vue').ComputedRef<boolean> }}
 *   isNarrow —— 宽度 ≤ 1024px（含平板与移动端）
 *   isMobile —— 宽度 ≤ 640px
 *   isTablet —— 641 ~ 1024px
 */
export function useBreakpoint() {
  const narrowMedia = window.matchMedia(`(max-width: ${BREAKPOINTS.narrow}px)`)
  const mobileMedia = window.matchMedia(`(max-width: ${BREAKPOINTS.mobile}px)`)

  const isNarrow = ref(narrowMedia.matches)
  const isMobile = ref(mobileMedia.matches)

  const handleNarrowChange = (event) => { isNarrow.value = event.matches }
  const handleMobileChange = (event) => { isMobile.value = event.matches }

  onMounted(() => {
    // addEventListener 形式在 Safari 14+ / 所有现代浏览器可用，无需兼容已废弃的 addListener
    narrowMedia.addEventListener('change', handleNarrowChange)
    mobileMedia.addEventListener('change', handleMobileChange)
  })

  onUnmounted(() => {
    // 不摘监听会让媒体查询对象一直持有组件闭包，组件卸载后仍被回调
    narrowMedia.removeEventListener('change', handleNarrowChange)
    mobileMedia.removeEventListener('change', handleMobileChange)
  })

  return {
    isNarrow,
    isMobile,
    isTablet: computed(() => isNarrow.value && !isMobile.value)
  }
}
