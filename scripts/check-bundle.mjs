#!/usr/bin/env node
/**
 * 校验前端构建产物没有被 Element Plus 的**未使用组件**污染。
 *
 * <p><b>为什么需要这个脚本：</b>这个问题在源码里完全看不出来——代码干净、lint 通过、
 * 页面正常，唯一的表现是产物悄悄变大。触发条件又极其隐蔽：只要有人给
 * `vite.config.js` 的 `manualChunks` 加回一条 element-plus 规则（看起来只是
 * "优化分包"，非常合理的举动），`element-plus` 桶文件（`es/index.mjs` 的
 * `export * from` 80 多个组件）的重导出就无法被 tree-shake，全部组件进入产物。
 *
 * <p>实测差异（同一份源码，仅 `manualChunks` 一行之差）：
 * <pre>
 *   归入 'element-plus' chunk       → JS 1777 kB / gzip 589 kB
 *   返回 undefined（Rollup 自动分） → JS 1439 kB / gzip 488 kB
 * </pre>
 * 这是个「改了别处、坏在这里」的缺陷，靠代码审查很难拦住，必须由产物断言。
 *
 * <p><b>为什么只查几个标志物而不是穷举：</b>该退化是**全有或全无**的——桶文件要么
 * 被完整摇掉、要么 80+ 个组件全部保留，不存在"只漏进来两个"的中间态。
 * 因此查一个确定不会被用到的组件即可判定。曾尝试动态计算"源码未引用的组件"，
 * 但那会产生大量误报：Element Plus 组件之间**互相引用**
 * （`ElPopper` 是 `el-select`/`el-tooltip` 的底座，`ElScrollbar` 被下拉框内部使用），
 * "源码没写"不等于"不需要"。下面的标志物均已实测确认：在当前配置下**不存在**于产物中。
 *
 * <p>用法（需先构建）：
 * <pre>
 *   cd frontend && npm run build
 *   node ../scripts/check-bundle.mjs
 * </pre>
 */
import { readFileSync, readdirSync, existsSync, statSync } from 'node:fs'
import { join, dirname, resolve } from 'node:path'
import { fileURLToPath } from 'node:url'

const repoRoot = resolve(dirname(fileURLToPath(import.meta.url)), '..')
const distDir = join(repoRoot, 'frontend', 'dist', 'assets')

if (!existsSync(distDir)) {
  console.error('✗ 找不到 frontend/dist/assets —— 请先执行 `cd frontend && npm run build`')
  process.exit(1)
}

/**
 * 标志物：项目**确定不会用到**的 Element Plus 组件。
 *
 * 用子组件名（如 ElCarouselItem）而非容器名：子组件的标记只会随父组件一起出现，
 * 不会因为别处引用了同名工具函数而误报。
 *
 * 若将来真的要引入其中某个组件，请把它从本列表移除，并在提交信息里说明——
 * 这是有意的决定，不是回归。
 */
const MARKERS = [
  'ElCarouselItem',      // 轮播图（电商首页常用，但本项目用的是静态 Banner）
  'ElColorPickerPanel',  // 取色器
  'ElCascaderPanel',     // 级联选择
  'ElCalendar'           // 日历
]

const bundles = readdirSync(distDir).filter(f => f.endsWith('.js'))
if (bundles.length === 0) {
  console.error('✗ dist/assets 下没有任何 .js —— 构建产物不完整。')
  process.exit(1)
}

const contents = bundles.map(f => ({ name: f, text: readFileSync(join(distDir, f), 'utf8') }))
const totalKb = Math.round(
  bundles.reduce((sum, f) => sum + statSync(join(distDir, f)).size, 0) / 1024
)

const leaked = []
for (const marker of MARKERS) {
  for (const bundle of contents) {
    if (bundle.text.includes(`"${marker}"`)) leaked.push(`${marker}（${bundle.name}）`)
  }
}

// 非空断言：万一产物是空的或读取失败，上面会"没找到标志物"从而假通过。
// 用一个必然存在的东西证明产物确实被读进来了。
const sanity = contents.some(b => b.text.length > 10_000)
if (!sanity) {
  console.error('✗ 产物内容异常（读不到任何有效代码）——结果是假通过。')
  process.exit(1)
}

console.log(`产物 JS chunk ${bundles.length} 个，合计 ${totalKb} kB`)
console.log(`检查标志物 ${MARKERS.length} 个：${MARKERS.join(', ')}`)

if (leaked.length === 0) {
  console.log('✓ 未发现未使用的 Element Plus 组件（桶文件 tree-shaking 正常）')
  process.exit(0)
}

console.error(`\n✗ 产物中混入了未使用的 Element Plus 组件：\n`)
leaked.forEach(l => console.error(`  ${l}`))
console.error(`
  最可能的原因：vite.config.js 的 manualChunks 给 element-plus 指定了 chunk 名。
  这会让它的桶文件失去跨包 tree-shaking，实测多出约 338 kB（gzip 101 kB）。

  修复：在 manualChunks 最前面提前返回 ——
      if (id.includes('node_modules/element-plus') || id.includes('node_modules/@element-plus')) return

  详见 vite.config.js 中 manualChunks 上方的注释。

  若确实是本脚本的标志物列表过时（项目开始合法使用该组件），
  请从 scripts/check-bundle.mjs 的 MARKERS 中移除对应项。
`)
process.exit(1)
