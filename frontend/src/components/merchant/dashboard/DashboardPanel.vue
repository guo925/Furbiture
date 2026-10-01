<template>
  <div class="work-panel">
    <div class="panel-head">
      <div>
        <h3>{{ title }}</h3>
        <p v-if="subtitle">{{ subtitle }}</p>
      </div>
      <slot name="action" />
    </div>
    <slot />
  </div>
</template>

<script setup>
/**
 * 仪表板面板外壳
 *
 * 抽出的目的：`work-panel` 卡片外观与 `panel-head` 标题区在 7 个面板里完全一致，
 * 若每个子组件各抄一份会有 ~28 行 × 7 的重复。这里作为唯一来源。
 * 右上角链接用 #action 插槽传入（其样式由调用方负责，因为插槽内容的作用域属于调用方）。
 */
defineProps({
  title: { type: String, required: true },
  subtitle: { type: String, default: '' }
})
</script>

<style scoped>
.work-panel {
  padding: 20px;
  background: rgba(255, 255, 255, 0.96);
  border: 1px solid #e8ecf3;
  border-radius: 10px;
  box-shadow: 0 12px 28px rgba(17, 24, 39, 0.04);
}

.panel-head {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 16px;
  margin-bottom: 16px;
}

.panel-head h3 {
  margin: 0 0 4px;
  color: #17202b;
  font-size: 18px;
}

.panel-head p {
  margin: 0;
  color: #6b7280;
  line-height: 1.6;
}
</style>
