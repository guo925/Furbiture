<template>
  <!--
    ElConfigProvider 承担原先 main.js 里 `app.use(ElementPlus, {...})` 的全局配置职责。
    改用按需引入后不能再全量注册组件，但语言包与提示默认值必须有个去处，就是这个组件。

    · locale  —— 不指定时 EP 内置文案为英文，分页组件会显示 "Total / 10/page"
    · message —— 全局 toast 的默认位置/关闭按钮/停留时长
    两者都必须包住所有用到 EP 组件的内容，故放在根节点。
  -->
  <el-config-provider :locale="zhCn" :message="messageConfig">
    <div class="app">
      <router-view />
    </div>
  </el-config-provider>
</template>

<script setup>
// 主应用组件
import zhCn from 'element-plus/es/locale/lang/zh-cn'

/**
 * 全局消息提示默认值。
 * 字段名与 EP 的 MessageConfigContext 一致（max / grouping / duration / offset / showClose / plain / placement）。
 */
const messageConfig = {
  offset: 100,
  showClose: true,
  duration: 3000
}
</script>

<style>
* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

.app {
  min-height: 100vh;
  /* 必须走令牌：.app 包住整个 router-view，写死字面量会把所有子元素继承的字体
     都改成 Arial，令 design-tokens.css 的 --font-family 从未生效。
     element-theme.css 已用 --el-font-family 映射同一令牌，此处保持一致。 */
  font-family: var(--font-family);
}
</style>
