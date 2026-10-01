<template>
  <section class="hot-panel">
    <DashboardPanel title="热销商品" subtitle="按销量排序 TOP 5">
      <div class="hot-list">
        <div v-for="(item, idx) in products.slice(0, 5)" :key="idx" class="hot-item">
          <span class="hot-rank" :class="'rank-' + (idx + 1)">{{ idx + 1 }}</span>
          <span class="hot-name">{{ item.name }}</span>
          <span class="hot-count">{{ item.salesCount }} 件</span>
        </div>
        <el-empty v-if="!products.length" description="暂无数据" :image-size="60" />
      </div>
    </DashboardPanel>
  </section>
</template>

<script setup>
import DashboardPanel from './DashboardPanel.vue'

defineProps({
  products: { type: Array, default: () => [] }
})
</script>

<style scoped>
.hot-panel {
  margin-top: 18px;
}

.hot-list {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 14px;
  padding: 6px 0;
}

.hot-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 14px;
  border-radius: 8px;
  background: #f7f9fc;
}

.hot-rank {
  width: 24px;
  height: 24px;
  border-radius: 6px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  font-weight: 700;
  background: #edf1f7;
  color: #8a94a6;
  flex: 0 0 auto;
}

.hot-rank.rank-1 {
  background: #fff3cd;
  color: #f59e0b;
}

.hot-rank.rank-2 {
  background: #e2e8f0;
  color: #718096;
}

.hot-rank.rank-3 {
  background: #fed7d7;
  color: #e53e3e;
}

.hot-name {
  flex: 1;
  font-size: 13px;
  color: #344054;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.hot-count {
  font-size: 12px;
  color: #ff7a1a;
  font-weight: 600;
  flex: 0 0 auto;
}

@media (max-width: 1160px) {
  .hot-list {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 720px) {
  .hot-list {
    grid-template-columns: 1fr;
  }
}
</style>
