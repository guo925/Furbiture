<template>
  <DashboardPanel title="交易概览" subtitle="实时统计当前店铺交易结果">
    <div class="trend-bars">
      <div class="trend-row">
        <span>订单完成</span>
        <div><i :style="{ width: completeRate + '%' }"></i></div>
        <b>{{ completed }}</b>
      </div>
      <div class="trend-row">
        <span>商品供给</span>
        <div><i :style="{ width: productRate + '%' }"></i></div>
        <b>{{ productCount }}</b>
      </div>
      <div class="trend-row">
        <span>发货压力</span>
        <div><i class="danger" :style="{ width: pendingRate + '%' }"></i></div>
        <b>{{ paid }}</b>
      </div>
    </div>
  </DashboardPanel>
</template>

<script setup>
import DashboardPanel from './DashboardPanel.vue'

defineProps({
  completeRate: { type: Number, default: 0 },
  productRate: { type: Number, default: 0 },
  pendingRate: { type: Number, default: 0 },
  completed: { type: Number, default: 0 },
  productCount: { type: Number, default: 0 },
  paid: { type: Number, default: 0 }
})
</script>

<style scoped>
.trend-bars {
  display: grid;
  gap: 18px;
  padding-top: 6px;
}

.trend-row {
  display: grid;
  grid-template-columns: 72px minmax(0, 1fr) 44px;
  align-items: center;
  gap: 12px;
  color: #667085;
}

.trend-row div {
  height: 9px;
  border-radius: 999px;
  background: #edf1f7;
  overflow: hidden;
}

.trend-row i {
  display: block;
  height: 100%;
  border-radius: inherit;
  background: linear-gradient(90deg, #ff7a1a, #ffb15d);
}

.trend-row i.danger {
  background: linear-gradient(90deg, #ef4444, #fb923c);
}

.trend-row b {
  color: #17202b;
  text-align: right;
}
</style>
