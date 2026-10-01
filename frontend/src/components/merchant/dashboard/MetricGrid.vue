<template>
  <section class="metric-grid">
    <router-link v-for="item in metrics" :key="item.label" :to="item.to" class="metric-card">
      <span class="metric-icon" :class="item.tone">
        <el-icon><component :is="item.icon" /></el-icon>
      </span>
      <span class="metric-label">{{ item.label }}</span>
      <strong>{{ item.value }}</strong>
      <small>{{ item.hint }}</small>
    </router-link>
  </section>
</template>

<script setup>
defineProps({
  metrics: { type: Array, default: () => [] }
})
</script>

<style scoped>
.metric-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 14px;
}

.metric-card {
  position: relative;
  display: grid;
  gap: 8px;
  min-height: 142px;
  padding: 18px;
  color: inherit;
  text-decoration: none;
  overflow: hidden;
  transition: transform 0.16s ease, box-shadow 0.16s ease;
  background: rgba(255, 255, 255, 0.96);
  border: 1px solid #e8ecf3;
  border-radius: 10px;
  box-shadow: 0 12px 28px rgba(17, 24, 39, 0.04);
}

.metric-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 16px 32px rgba(17, 24, 39, 0.08);
}

.metric-icon {
  width: 36px;
  height: 36px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: 9px;
  font-size: 18px;
}

.metric-icon.blue {
  color: #2563eb;
  background: #eff6ff;
}

.metric-icon.orange {
  color: #ea580c;
  background: #fff7ed;
}

.metric-icon.green {
  color: #16a34a;
  background: #f0fdf4;
}

.metric-icon.purple {
  color: #7c3aed;
  background: #f5f3ff;
}

.metric-label {
  color: #667085;
  font-size: 13px;
}

.metric-card strong {
  color: #111827;
  font-size: 28px;
  line-height: 1;
}

.metric-card small {
  color: #8a94a6;
}

@media (max-width: 1160px) {
  .metric-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 720px) {
  .metric-grid {
    grid-template-columns: 1fr;
  }
}
</style>
