import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import Components from 'unplugin-vue-components/vite'
import { ElementPlusResolver } from 'unplugin-vue-components/resolvers'

/**
 * 依赖分包：把体积大、变动少的第三方库拆成独立 chunk，让应用代码改动时
 * 浏览器可以复用缓存的 vendor chunk。
 *
 * ⚠️ <b>不要给 element-plus 指定 chunk 名</b>（本文件踩过的坑，实测数据在下）。
 *
 * `manualChunks` 只要给某个模块返回了 chunk 名，**该模块就会失去跨包 tree-shaking**。
 * Element Plus 是通过**桶文件**（`element-plus/es/index.mjs`，`export * from` 80 多个组件）
 * 被引入的，桶文件的重导出能否被消除，直接决定未使用组件会不会被打进产物。
 * 一旦把 element-plus 归到固定 chunk，桶里那些**从未被使用的组件**（ElCarousel、
 * ElColorPicker、ElCascader、ElTransfer、ElMention、ElTour、ElWatermark…）会全量保留。
 *
 * 实测（同一份源码）：
 *   · element-plus 归入 'element-plus' chunk  → JS 1777 kB / gzip 589 kB（未使用组件全部保留）
 *   · element-plus 返回 undefined（交回 Rollup 自动分块）→ JS 1439 kB / gzip 488 kB
 *   **差 338 kB（gzip 101 kB），且后者的 vendor 分包收益一分不少。**
 *
 * 为什么 echarts 不受影响：它走**子路径**按需引入（`echarts/core`、`echarts/charts`），
 * 没有桶文件重导出需要消除，因此照常分包即可。
 */
function manualChunks(id) {
  if (!id.includes('node_modules')) return

  // —— Element Plus 必须放在最前面并返回 undefined ——
  // 顺序敏感：下面 `@vue/`、`vue-router` 等宽泛规则会把它吞掉，
  // 而且 `@element-plus/icons-vue` 的路径里也含 "element-plus"，必须一并覆盖。
  if (id.includes('node_modules/element-plus') || id.includes('node_modules/@element-plus')) return

  if (id.includes('node_modules/echarts') || id.includes('node_modules/zrender')) return 'echarts'
  if (id.includes('node_modules/vue-router')) return 'vue-router'
  if (id.includes('node_modules/pinia')) return 'pinia'
  if (id.includes('node_modules/axios')) return 'axios'
  if (id.includes('node_modules/@vue/') || id.includes('node_modules/vue/')) return 'vue-vendor'
  return 'vendor'
}

export default defineConfig({
  plugins: [
    vue(),
    // Element Plus 按需引入：模板里写 <el-table> 时，由本插件自动补上
    // `import { ElTable } from 'element-plus'` 与指令（v-loading → ElLoadingDirective），
    // 不再需要 main.js 里的 `app.use(ElementPlus)` 全量注册。
    //
    // 为什么这一步能显著瘦身：`element-plus/package.json` 的 `sideEffects` 只声明了
    // CSS 与 style 目录，`es/**` 的 JS 是**可 tree-shake 的**——但全量注册会让
    // 每一个组件都变成"可达"，tree-shaking 就无从下手。去掉全量注册后，
    // 打包结果只包含模板中真正出现过的组件。
    //
    // importStyle: false —— 样式仍走 main.js 的 `element-plus/dist/index.css` 全量引入。
    // 若这里改成默认值，插件会再逐个组件注入 `es/components/*/style/css`，
    // 与全量 CSS **内容重复**、体积翻倍。两者只能取其一，本项目取全量 CSS
    // （原因见 main.js 注释：ElMessage 等 24 处 JS API 是显式 import 的）。
    Components({
      resolvers: [ElementPlusResolver({ importStyle: false })],
      dts: false // 项目是 JS 不是 TS，不需要生成 components.d.ts
    })
  ],
  esbuild: {
    // 只剥离 debugger。
    // 不 drop console：ESLint 已禁止 console.log（batch 1 已清零），
    // 剩余 20+ 处 console.error/warn 是项目**刻意保留**的生产错误可观测性
    // （见 docs/AI-CONTEXT.md §九）。drop: ['console'] 会把它们一并删掉，
    // 收益只是几 KB，却让线上错误变成静默，得不偿失。
    drop: ['debugger']
  },
  build: {
    // 分包后应用自身代码仅 ~13kB，但 echarts 单个 chunk 约 1.1MB（两个仪表板
    // 视图用 `import * as echarts` 全量引入）。阈值设在它之上以消除已知噪音，
    // echarts 的真正瘦身留待按需引入（见报告"待办"）。
    chunkSizeWarningLimit: 1200,
    rollupOptions: {
      output: {
        manualChunks
      }
    }
  },
  server: {
    port: 3003,
    proxy: {
      '/api': {
        target: 'http://localhost:9090',
        changeOrigin: true
      },
      '/uploads': {
        target: 'http://localhost:9090',
        changeOrigin: true
      }
    }
  }
})
