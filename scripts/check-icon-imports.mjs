#!/usr/bin/env node
/**
 * 校验前端模板里用到的 Element Plus 图标，是否都在同一个文件里显式 import 了。
 *
 * <p><b>为什么需要这个脚本：</b>`main.js` 曾经把全部 293 个图标全局注册
 * （`app.component(key, component)`）。全局注册让所有图标都变成"可能被用到"，
 * 构建工具无法 tree-shake，产物里因此多出 171 kB。删掉全局注册能显著瘦身，
 * 但**风险是静默的**：某个模板里 `<Search />` 没有对应 import 时，
 * Vue 只会在**运行时**往控制台打一条 "Failed to resolve component" 警告，
 * 页面照常渲染，只是那个图标不见了——构建不会失败，lint 也不会报错。
 *
 * <p>所以用这个脚本在**构建期**把这类遗漏变成可见的失败。
 *
 * <p>用法：
 * <pre>
 *   node scripts/check-icon-imports.mjs
 * </pre>
 *
 * <p><b>注意两种合法豁免</b>（脚本不会报，但值得知道）：
 * <ul>
 *   <li>图标通过 `:icon="Search"` 之类的 **prop** 传入（如 `el-input` 的前缀图标）——
 *       这类引用在 `<script>` 里，本来就必须 import。</li>
 *   <li>图标通过 `<component :is="item.icon" />` 动态渲染，而 `item.icon` 的值是
 *       在 script 里 import 进来的组件引用（本项目全部属于这一类）。
 *       **但若哪天有人把 `icon` 的值改成字符串 `'Search'`**，本脚本检测不到，
 *       那种写法又会变回"需要全局注册"。改数据时请留意。</li>
 * </ul>
 */
import { readFileSync, readdirSync, statSync, existsSync } from 'node:fs'
import { join, dirname, resolve } from 'node:path'
import { fileURLToPath } from 'node:url'
import { createRequire } from 'node:module'

const repoRoot = resolve(dirname(fileURLToPath(import.meta.url)), '..')
const frontendDir = join(repoRoot, 'frontend')
const srcDir = join(frontendDir, 'src')

if (!existsSync(srcDir)) {
  console.error(`✗ 找不到前端源码目录：${srcDir}`)
  process.exit(1)
}

// ── 1. 取图标库的真实导出名 ────────────────────────────────────────────
// 不能去解析 .d.ts：icons-vue 的 index.d.ts 只有一行 `export * from './components'`，
// 用正则去匹配会得到 0 个导出，于是整个检查循环一次都不执行、直接打印"全部通过"
// —— 一个永远为真的检查比没有检查更危险。这里直接问模块运行时要真实导出。
const require = createRequire(join(frontendDir, 'package.json'))
let iconNames
try {
  iconNames = Object.keys(require('@element-plus/icons-vue'))
} catch (e) {
  console.error('✗ 无法加载 @element-plus/icons-vue，请先在 frontend/ 下执行 npm ci')
  console.error(`  原因：${e.message}`)
  process.exit(1)
}

if (iconNames.length === 0) {
  console.error('✗ 图标库导出数为 0，检查会空转。请确认依赖安装是否完整。')
  process.exit(1)
}

// ── 2. 遍历所有 .vue，比对模板用法与 script 里的 import ─────────────────
function walk(dir, out = []) {
  for (const entry of readdirSync(dir)) {
    const full = join(dir, entry)
    if (statSync(full).isDirectory()) walk(full, out)
    else if (full.endsWith('.vue')) out.push(full)
  }
  return out
}

/**
 * 收集某个文件**所有 import 语句**绑定的本地标识符名。
 *
 * <p>这里刻意不检查"标识符在 script 里出现过"——那太弱：
 * 删掉 `import { Goods }` 之后，导航数据里的 `icon: Goods` 仍然会让它"出现过"，
 * 检查照样通过（本脚本第一版就是这么写的，负向对照一测就露馅）。
 * 只认 import 语句真正绑定进来的名字。
 */
function collectImportedBindings(source) {
  const bound = new Set()
  const importRe = /import\s+(?:type\s+)?([\s\S]*?)\s+from\s*['"][^'"]+['"]/g
  let match
  while ((match = importRe.exec(source))) {
    const clause = match[1]
    // 具名导入：{ A, B as C }
    const braced = clause.match(/\{([\s\S]*)\}/)
    if (braced) {
      for (const part of braced[1].split(',')) {
        const t = part.trim()
        if (!t) continue
        const aliased = t.match(/\bas\s+([A-Za-z_$][\w$]*)$/)
        bound.add(aliased ? aliased[1] : t.replace(/^type\s+/, ''))
      }
    }
    // 默认导入 / 命名空间导入：import Foo from ...  /  import * as Foo from ...
    const withoutBraces = clause.replace(/\{[\s\S]*\}/, '').replace(/,\s*$/, '').trim()
    if (withoutBraces) {
      const ns = withoutBraces.match(/^\*\s+as\s+([A-Za-z_$][\w$]*)$/)
      bound.add(ns ? ns[1] : withoutBraces.replace(/^type\s+/, ''))
    }
  }
  return bound
}

const problems = []
let templateIconRefs = 0

for (const file of walk(srcDir)) {
  const source = readFileSync(file, 'utf8')
  const templateMatch = source.match(/<template>([\s\S]*?)\n<\/template>/)
  if (!templateMatch) continue

  const template = templateMatch[1]
  const script = source.replace(template, '')
  const rel = file.replace(`${repoRoot}/`, '')
  const bound = collectImportedBindings(script)

  for (const name of iconNames) {
    // 模板中作为标签出现：<Search> / <Search /> / <Search ...>
    if (!new RegExp(`<${name}(\\s|/|>)`).test(template)) continue
    templateIconRefs++
    if (!bound.has(name)) {
      problems.push(`${rel}  →  <${name}> 在模板中使用，但该文件没有 import 它`)
    }
  }
}

// ── 3. 非空断言 ────────────────────────────────────────────────────────
// 与上一条同理：若模板解析规则失效导致一处都没匹配到，下面会打印"通过"，
// 但那是假通过。这里显式要求至少匹配到若干处已知存在的引用。
if (templateIconRefs === 0) {
  console.error('✗ 模板中一处图标引用都没匹配到——检查规则已失效，结果是假通过。')
  console.error('  请确认 .vue 文件的 <template> 结构与本脚本的解析假设一致。')
  process.exit(1)
}

// ── 4. 输出 ────────────────────────────────────────────────────────────
console.log(`图标库导出 ${iconNames.length} 个；模板中以标签形式引用 ${templateIconRefs} 处`)
console.log('（作用域：只检查模板标签 <Xxx />。script 里的 `icon: Goods` / `markRaw(Van)`')
console.log('  这类标识符引用不在此检查内——它们若未 import，lint 与编译本身就会报错。）')

if (problems.length === 0) {
  console.log('✓ 所有模板图标都有显式 import（main.js 的全局注册可以安全移除）')
  process.exit(0)
}

console.error(`\n✗ 发现 ${problems.length} 处模板图标缺少 import：\n`)
problems.forEach(p => console.error(`  ${p}`))
console.error('\n修复：在该文件的 <script setup> 中加上')
console.error("  import { 图标名 } from '@element-plus/icons-vue'\n")
process.exit(1)
