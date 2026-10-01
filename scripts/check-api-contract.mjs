#!/usr/bin/env node
/**
 * check-api-contract.mjs — 前端 API 契约校验
 *
 * 解决什么问题
 * ------------
 * 前端 `api/modules/*.js` 定义了一组 API 对象（productAPI / orderAPI / ...），
 * 视图层通过 `xxxAPI.method(...)` 调用。调用了**不存在的方法**时，JS 不会编译报错，
 * 运行时抛 TypeError —— 而这个异常几乎总被 catch 静默吞掉，于是**功能悄无声息地全废**。
 *
 * 本项目已真实发生 2 次：
 *   - ProductsView.vue / HomeView.vue 调用 productAPI.getCategories() → 方法其实在 categoryAPI 上
 *   - ProfileView.vue                 调用 orderAPI.getOrderStats()   → 方法从未存在
 * 两处的后果都是「页面看起来正常、功能实际不工作」，常规代码审查极难发现。
 *
 * 为什么是脚本而不是文档
 * --------------------
 * 这是**完全可机械判定**的约束（方法名集合比对），不存在需要判断的灰区。
 * 能被校验器判定的东西就该自动化，不要写成人靠自觉遵守的规范。
 *
 * 用法
 * ----
 *   node scripts/check-api-contract.mjs          # 报告问题；退出码 1 表示有错误
 *   node scripts/check-api-contract.mjs --quiet  # 只输出错误，不列未使用方法
 *
 * 退出码：0 = 通过，1 = 存在错误（可用于 CI / pre-commit 门禁）
 */

import { readFileSync, readdirSync, statSync } from 'node:fs'
import { join, relative } from 'node:path'
import { fileURLToPath } from 'node:url'

const ROOT = fileURLToPath(new URL('..', import.meta.url))
const SRC = join(ROOT, 'frontend/src')
const MODULES_DIR = join(SRC, 'api/modules')
const QUIET = process.argv.includes('--quiet')

const RED = '\x1b[31m', YELLOW = '\x1b[33m', GREEN = '\x1b[32m', DIM = '\x1b[2m', NC = '\x1b[0m'

/**
 * 从 openIdx（必须指向 '{'）开始，返回**不含最外层大括号**的内容。
 * 返回不含外层括号是关键：调用方拿到的是对象"内部"，
 * 内部的第一层（即 API 方法定义那一层）深度为 0，才能被正确识别。
 */
function readObjectBody(src, openIdx) {
  if (src[openIdx] !== '{') return null
  let depth = 0
  for (let i = openIdx; i < src.length; i++) {
    const c = src[i]
    if (c === '{') depth++
    else if (c === '}') {
      depth--
      if (depth === 0) return src.slice(openIdx + 1, i)
    }
  }
  return null
}

/**
 * 解析 API 对象内部的一层，返回 { methods:Set, nested:{组名:Set} }。
 *
 * 按行解析而非逐字符：本项目的 API 模块是规整格式化的，
 * 且同时存在两种写法——
 *   方法：`getList: (params) => ...`（admin.js）与 `toggle(id) { ... }`（favorite.js）
 *   分组：`products: { ... }`  ← adminAPI / merchantAPI 用
 * 逐字符扫描要在两种写法 + 嵌套之间同时正确，太脆弱；按缩进层级判断更可靠。
 */
function parseLevel(content) {
  const methods = new Set()
  const nested = {}
  let depth = 0
  let group = null

  // 花括号计数：模板串里的 `${id}` 是配对的 { }，净增 0，不影响层级
  const braceDelta = s => (s.match(/\{/g) || []).length - (s.match(/\}/g) || []).length

  for (const raw of content.split('\n')) {
    const line = raw.trim()
    if (!line) continue

    if (depth === 0) {
      const asGroup = /^([A-Za-z_$][\w$]*)\s*:\s*\{/.exec(line)
      if (asGroup) {
        group = asGroup[1]
        nested[group] = new Set()
      } else {
        const asMethod = /^(?:async\s+)?([A-Za-z_$][\w$]*)\s*[:(]/.exec(line)
        if (asMethod) methods.add(asMethod[1])
      }
    } else if (depth === 1 && group) {
      const asMethod = /^(?:async\s+)?([A-Za-z_$][\w$]*)\s*[:(]/.exec(line)
      if (asMethod) nested[group].add(asMethod[1])
    }

    depth += braceDelta(line)
    if (depth <= 0) { depth = 0; group = null }   // 分组结束，回到顶层
  }

  // 分组名也可能与方法名重名（如 products 组），此时以分组为准，避免误报
  for (const g of Object.keys(nested)) methods.delete(g)
  return { methods, nested }
}

/**
 * 把注释替换成等长空白（保留换行以维持行号），字符串原样保留。
 * 必须做这一步：本项目 HomeView.vue 的注释里就写着
 * 「此前误用了 productAPI.getCategories（该方法不存在）」——
 * 不剔除注释就会把这条正确的修复记录误报成错误。
 * 同时必须跳过字符串（如 'https://...' 里的 //）否则会误判为行注释、吞掉后面的代码。
 */
function stripComments(src) {
  const out = []
  let i = 0
  while (i < src.length) {
    const c = src[i]

    if (c === '"' || c === "'" || c === '`') {          // 字符串字面量：原样复制
      const q = c
      out.push(c); i++
      while (i < src.length) {
        if (src[i] === '\\') { out.push(src[i], src[i + 1] ?? ''); i += 2; continue }
        out.push(src[i])
        if (src[i] === q) { i++; break }
        i++
      }
      continue
    }
    if (c === '/' && src[i + 1] === '/') {              // 行注释：吞到行尾（换行留给下一轮输出）
      while (i < src.length && src[i] !== '\n') i++
      continue
    }
    if (c === '/' && src[i + 1] === '*') {              // 块注释：保留其中的换行
      i += 2
      while (i < src.length && !(src[i] === '*' && src[i + 1] === '/')) {
        if (src[i] === '\n') out.push('\n')
        i++
      }
      i += 2
      continue
    }
    if (src.startsWith('<!--', i)) {                     // Vue 模板注释
      i += 4
      while (i < src.length && !src.startsWith('-->', i)) {
        if (src[i] === '\n') out.push('\n')
        i++
      }
      i += 3
      continue
    }
    out.push(c); i++
  }
  return out.join('')
}

function extractApiSurface(rawSource, fileName) {
  const source = stripComments(rawSource)
  const surface = {}
  for (const m of source.matchAll(/export\s+const\s+([A-Za-z_$][\w$]*)\s*=\s*\{/g)) {
    const openIdx = m.index + m[0].length - 1
    const body = readObjectBody(source, openIdx)
    if (body === null) continue
    surface[m[1]] = { ...parseLevel(body), file: fileName }
  }
  return surface
}

function walk(dir, out = []) {
  for (const entry of readdirSync(dir)) {
    if (entry === 'node_modules' || entry === 'dist') continue
    const p = join(dir, entry)
    if (statSync(p).isDirectory()) walk(p, out)
    else if (/\.(js|vue|mjs)$/.test(entry)) out.push(p)
  }
  return out
}

// ---- 1. 建立契约 --------------------------------------------------------
const surface = {}
for (const f of readdirSync(MODULES_DIR)) {
  if (f.endsWith('.js')) Object.assign(surface, extractApiSurface(readFileSync(join(MODULES_DIR, f), 'utf8'), f))
}

const knownObjects = Object.keys(surface)
const used = new Map(knownObjects.map(n => [n, new Set()]))
const errors = []

// ---- 2. 扫描调用点 ------------------------------------------------------
for (const file of walk(SRC)) {
  if (file.startsWith(MODULES_DIR)) continue
  const src = stripComments(readFileSync(file, 'utf8'))   // 剔除注释，行号保持不变
  const rel = relative(ROOT, file)

  for (const apiName of knownObjects) {
    const api = surface[apiName]
    for (const m of src.matchAll(new RegExp(`\\b${apiName}\\s*\\.\\s*([A-Za-z_$][\\w$]*)`, 'g'))) {
      const member = m[1]
      const line = src.slice(0, m.index).split('\n').length

      if (api.methods.has(member)) { used.get(apiName).add(member); continue }

      // 嵌套分组：adminAPI.products.getList
      if (member in api.nested) {
        const sub = /^\s*\.\s*([A-Za-z_$][\w$]*)/.exec(src.slice(m.index + m[0].length))
        if (sub) {
          const path = `${member}.${sub[1]}`
          used.get(apiName).add(path)
          if (!api.nested[member].has(sub[1])) {
            errors.push({ rel, line, call: `${apiName}.${path}`,
              hint: `${apiName}.${member} 上不存在 ${sub[1]}（可用: ${[...api.nested[member]].join(', ') || '无'}）` })
          }
          continue
        }
      }
      const avail = [...api.methods, ...Object.keys(api.nested).map(k => `${k}.*`)].sort()
      errors.push({ rel, line, call: `${apiName}.${member}`,
        hint: `${apiName} 只导出: ${avail.join(', ') || '(无)'}` })
    }
  }
}

// ---- 3. 报告 ------------------------------------------------------------
console.log('\n' + '─'.repeat(64))
console.log('  前端 API 契约校验')
console.log('─'.repeat(64))
console.log(`  扫描到 ${knownObjects.length} 个 API 对象，方法定义于 frontend/src/api/modules/`)

if (errors.length) {
  console.log(`\n${RED}✗ 发现 ${errors.length} 处调用了不存在的 API 方法${NC}`)
  console.log(`${DIM}  这类错误运行时抛 TypeError，且通常被 catch 静默吞掉 → 功能全废但看不出来${NC}\n`)
  for (const e of errors) {
    console.log(`  ${RED}✗${NC} ${e.rel}:${e.line}`)
    console.log(`     调用: ${e.call}`)
    console.log(`     ${DIM}${e.hint}${NC}`)
  }
} else {
  console.log(`\n${GREEN}✓ 所有 API 调用都有对应定义${NC}`)
}

if (!QUIET) {
  const unused = Object.entries(surface)
    .map(([name, api]) => {
      const flat = [...api.methods].filter(m => !used.get(name).has(m))
      return flat.length ? `${name}: ${flat.join(', ')}` : null
    })
    .filter(Boolean)
  if (unused.length) {
    console.log(`\n${YELLOW}○ 定义了但全项目零调用的方法（死代码，确认后删除）${NC}`)
    for (const u of unused) console.log(`  ${DIM}·${NC} ${u}`)
    console.log(`${DIM}  注：可能是给未来预留的，也可能是「写了没接线」——后者是真缺陷${NC}`)
  }
}

console.log('─'.repeat(64) + '\n')
process.exit(errors.length ? 1 : 0)
