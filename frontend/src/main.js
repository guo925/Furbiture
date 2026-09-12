import { createApp } from 'vue'
import App from './App.vue'
import router from './router'
import store from './stores'
import ElementPlus from 'element-plus'
import zhCn from 'element-plus/es/locale/lang/zh-cn'
import 'element-plus/dist/index.css'
// Element Plus 暗色变量：配合 html.dark 类生效（由 useTheme 负责切换）
import 'element-plus/theme-chalk/dark/css-vars.css'
import './assets/styles/main.css'
import './assets/styles/design-tokens.css'
// 品牌令牌 → Element Plus 变量的映射，必须置于 EP 样式之后才能生效
import './assets/styles/element-theme.css'
import * as ElementPlusIconsVue from '@element-plus/icons-vue'

const app = createApp(App)

// 注册Element Plus图标
for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
  app.component(key, component)
}

app.use(ElementPlus, {
  // 不指定 locale 时 EP 内置文案为英文，分页会显示 "Total / 10/page"。
  // 项目 UI 全中文，必须显式指定中文语言包。
  locale: zhCn,
  globalConfig: {
    message: {
      offset: 100,
      showClose: true,
      duration: 3000
    }
  }
})
app.use(router)
app.use(store)
app.mount('#app')