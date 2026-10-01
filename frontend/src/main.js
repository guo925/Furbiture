import { createApp } from 'vue'
import App from './App.vue'
import router from './router'
import store from './stores'
import 'element-plus/dist/index.css'
// Element Plus 暗色变量：配合 html.dark 类生效（由 useTheme 负责切换）
import 'element-plus/theme-chalk/dark/css-vars.css'
import './assets/styles/main.css'
import './assets/styles/design-tokens.css'
// 品牌令牌 → Element Plus 变量的映射，必须置于 EP 样式之后才能生效
import './assets/styles/element-theme.css'
const app = createApp(App)

// ── 图标注册说明（别再改回全量注册）────────────────────────────────────
// 这里曾有一行 `import * as ElementPlusIconsVue` + 一个 for 循环把全部 293 个
// 图标 `app.component()` 全局注册，产物里因此多出 171 kB（gzip 45 kB）的
// element-plus-icons chunk，而其中真正被用到的只有 40 来个。
//
// 它还是**净损失**：全局注册会让每个图标都成为"可能被用到"的组件，
// 构建工具无法 tree-shake；而项目里 18 个文件本来就是**显式 import** 图标的
// （如 `import { Search } from '@element-plus/icons-vue'`），
// 这些显式 import 完全能独立工作，不依赖全局注册。
//
// 何时可以再加回来：只有当某个图标需要在**模板里直接写标签名**、
// 又确实无法 import 时（例如图标名来自后端返回的字符串）。
// 那种情况请优先改为在前端映射表里转成组件引用，而不是恢复全量注册。
//
// 复核方法：`node scripts/check-icon-imports.mjs`（模板用到但未 import 的图标会被列出）

// ── 关于 Element Plus 的注册方式（别再改回全量）────────────────────────
// 这里曾是 `app.use(ElementPlus, { locale, globalConfig })`，它会把 **全部组件**
// 都注册成全局组件，于是 element-plus chunk 达 781 kB——因为全量注册让每个组件
// 都变成"可达"，tree-shaking 无从下手（尽管该包的 `sideEffects` 只声明了 CSS，
// JS 本身是可摇树的）。
//
// 现在改为**按需引入**：模板里写 <el-table>，由 vite.config.js 里的
// `unplugin-vue-components` + `ElementPlusResolver` 在编译期自动补 import，
// 打包结果只含真正用到的组件。
//
// 原来挂在 app.use 上的两项配置没有丢，移到了 App.vue 的 <el-config-provider>：
//   ① locale: zhCn —— 不指定时 EP 内置文案是英文，分页会显示 "Total / 10/page"
//   ② globalConfig.message —— 全局提示的位置/关闭按钮/时长
//
// 样式仍走下面这行**全量 CSS**：项目有 24 处 `import { ElMessage } from 'element-plus'`
// 这类 JS API 显式导入，逐个改造成本高且易漏，而 CSS 全量引入（360 kB / gzip 48 kB）
// 换来"不可能出现无样式组件"的确定性。vite.config.js 里因此设了 importStyle: false，
// 避免插件再逐个组件注入样式、与这份全量 CSS 内容重复。
app.use(router)
app.use(store)
app.mount('#app')
