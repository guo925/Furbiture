<template>
  <DashboardPanel title="常用工具" subtitle="高频经营动作快速进入">
    <div class="shortcut-grid">
      <router-link v-for="shortcut in shortcuts" :key="shortcut.label" :to="shortcut.to">
        <el-icon><component :is="shortcut.icon" /></el-icon>
        <span>{{ shortcut.label }}</span>
      </router-link>
    </div>
  </DashboardPanel>
</template>

<script setup>
import DashboardPanel from './DashboardPanel.vue'

defineProps({
  shortcuts: { type: Array, default: () => [] }
})
</script>

<style scoped>
.shortcut-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
}

.shortcut-grid a {
  min-height: 84px;
  display: grid;
  place-items: center;
  gap: 8px;
  padding: 12px;
  border-radius: 8px;
  background: #f7f9fc;
  color: #344054;
  text-decoration: none;
  font-weight: 600;
}

.shortcut-grid a:hover {
  color: #ff7a1a;
  background: #fff7ed;
}

.shortcut-grid .el-icon {
  font-size: 22px;
}

@media (max-width: 720px) {
  .shortcut-grid {
    grid-template-columns: 1fr;
  }
}
</style>
