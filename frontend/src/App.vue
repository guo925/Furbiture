<template>
  <div class="app">
    <router-view />
  </div>
</template>

<script setup>
// 主应用组件
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