#!/usr/bin/env node
/**
 * check-entity-schema.mjs — 实体字段 vs 数据库表结构 漂移校验
 *
 * 解决什么问题
 * ------------
 * MyBatis-Plus 默认把实体的**所有非 @TableField(exist=false) 字段**拼进 SELECT/INSERT/UPDATE。
 * 所以只要「实体声明了字段、库里没有这一列」，这条链路的所有查询都会直接报
 *   `Unknown column 'xxx' in 'field list'`
 * —— 不是少一个字段那么简单，是**整个模块不可用**。
 *
 * 本项目真实发生过：`Category` 实体有 `icon` / `status`，但建表脚本里没有这两列。
 * 现有开发库是**手工补过列**的，所以一直没暴露；照 `schema.sql` 全新建库则会缺列，
 * 分类相关接口全部挂掉。这类问题在开发机上永远看不见，只在全新部署时爆。
 *
 * 为什么是脚本
 * ----------
 * 完全可机械判定（字段名集合比对），无需人工判断，就该自动化。
 *
 * 用法
 * ----
 *   node scripts/check-entity-schema.mjs                       # 默认连 furniture_db
 *   DB_NAME=furbiture_verify node scripts/check-entity-schema.mjs
 *   MYSQL_CMD="mysql -h127.0.0.1 -uroot -p123456" node scripts/check-entity-schema.mjs
 *
 * 退出码：0 = 一致，1 = 存在漂移
 *
 * 注意：本脚本**只读** information_schema，不会修改任何数据。
 */

import { readFileSync, readdirSync } from 'node:fs'
import { execFileSync } from 'node:child_process'
import { join } from 'node:path'
import { fileURLToPath } from 'node:url'

const ROOT = fileURLToPath(new URL('..', import.meta.url))
const ENTITY_DIR = join(ROOT, 'src/main/java/com/gjx/entity')

const DB_NAME = process.env.DB_NAME || 'furniture_db'
// 默认走 docker 里的 seckill-mysql（本机标准中间件）；可用 MYSQL_CMD 覆盖
const MYSQL_CMD = process.env.MYSQL_CMD || 'docker exec -i seckill-mysql mysql -uroot -p123456'

const RED = '\x1b[31m', GREEN = '\x1b[32m', YELLOW = '\x1b[33m', DIM = '\x1b[2m', NC = '\x1b[0m'

/** 用 execFileSync + 参数数组执行（不走 shell，避免命令注入） */
function queryColumns() {
  const [cmd, ...baseArgs] = MYSQL_CMD.split(/\s+/)
  const sql = `SELECT TABLE_NAME, COLUMN_NAME FROM information_schema.COLUMNS WHERE TABLE_SCHEMA='${DB_NAME}'`
  let out
  try {
    out = execFileSync(cmd, [...baseArgs, '-N', '-B', '-e', sql], {
      encoding: 'utf8', stdio: ['ignore', 'pipe', 'pipe'],
    })
  } catch (e) {
    console.error(`${RED}✗ 无法连接数据库（${DB_NAME}）${NC}`)
    console.error(`${DIM}  ${e.stderr?.toString().trim() || e.message}${NC}`)
    console.error(`${DIM}  提示：确认容器在跑 → docker ps；或用 DB_NAME=/MYSQL_CMD= 覆盖${NC}`)
    process.exit(2)
  }

  const cols = {}
  for (const line of out.trim().split('\n')) {
    const [t, c] = line.split('\t')
    if (!t || !c) continue
    ;(cols[t] ??= new Set()).add(c)
  }
  return cols
}

const camelToSnake = s => s.replace(/([a-z0-9])([A-Z])/g, '$1_$2').toLowerCase()

function parseEntity(file) {
  const src = readFileSync(join(ENTITY_DIR, file), 'utf8')
  const table = /@TableName\(\s*[`"]?([A-Za-z_]+)[`"]?\s*\)/.exec(src)?.[1]
  if (!table) return null

  // 非数据库字段：@TableField(exist = false) 修饰的，两遍扫描都要排除
  const transientFields = new Set()
  for (const m of src.matchAll(
    /@TableField\s*\(\s*exist\s*=\s*false\s*\)[\s\S]{0,120}?private\s+[\w<>,\s[\]]+?\s+(\w+)\s*;/g)) {
    transientFields.add(m[1])
  }

  const fields = []
  for (const m of src.matchAll(/@TableField\s*\(([^)]*)\)\s*(?:private|protected)\s+[\w<>,\s]+\s+(\w+)\s*;/g)) {
    if (!transientFields.has(m[2])) fields.push(m[2])
  }
  const body = src.slice(src.indexOf('{'))
  for (const m of body.matchAll(/(?:^|\n)\s*private\s+(?!static)([\w<>,\s[\]]+?)\s+(\w+)\s*;/g)) {
    if (!transientFields.has(m[2]) && !fields.includes(m[2])) fields.push(m[2])
  }
  return { table, fields }
}

// ---- main ---------------------------------------------------------------
const cols = queryColumns()
const entityFiles = readdirSync(ENTITY_DIR).filter(f => f.endsWith('.java'))
const problems = []

for (const f of entityFiles) {
  const parsed = parseEntity(f)
  if (!parsed) continue
  const { table, fields } = parsed

  if (!cols[table]) {
    problems.push({ file: f, table, kind: 'table-missing', items: ['表不存在'] })
    continue
  }
  const missing = fields
    .map(name => ({ name, col: camelToSnake(name) }))
    .filter(x => !cols[table].has(x.col))
    .map(x => `${x.name} → ${x.col}`)

  if (missing.length) problems.push({ file: f, table, kind: 'column-missing', items: missing })
}

console.log('\n' + '─'.repeat(64))
console.log('  实体字段 vs 数据库表结构 漂移校验')
console.log('─'.repeat(64))
console.log(`  库: ${DB_NAME}   实体: ${entityFiles.length} 个   表: ${Object.keys(cols).length} 张`)

if (problems.length) {
  console.log(`\n${RED}✗ ${problems.length} 个实体存在漂移${NC}`)
  console.log(`${DIM}  实体字段会被 MyBatis-Plus 拼进 SQL；库里没有该列 → 整条链路 Unknown column${NC}\n`)
  for (const p of problems) {
    console.log(`  ${RED}✗${NC} ${p.file}  (表 ${p.table})`)
    for (const it of p.items) console.log(`     - ${it}`)
  }
  console.log(`\n${YELLOW}  修复方向：补一条幂等迁移脚本添加缺列，并同步更新 schema.sql${NC}`)
} else {
  console.log(`\n${GREEN}✓ 全部实体与表结构一致${NC}`)
}

console.log('─'.repeat(64) + '\n')
process.exit(problems.length ? 1 : 0)
