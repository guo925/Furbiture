import { createApp } from 'vue'
import App from './App.vue'
import router from './router'
import store from './stores'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import './assets/styles/main.css'
import './assets/styles/design-tokens.css'
import * as ElementPlusIconsVue from '@element-plus/icons-vue'
import lazyLoad from './directives/lazyLoad'

const app = createApp(App)

// 注册全局指令
app.directive('lazy-load', lazyLoad)

// 注册Element Plus图标
for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
  app.component(key, component)
}

app.use(ElementPlus, {
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