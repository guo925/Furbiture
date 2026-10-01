<template>
  <!-- 副标题必须与 healthScore 的公式一致（履约率 + 商品供给），
       原先写"基础项越完整，买家信任越高"但公式里根本没有资料完整度 -->
  <DashboardPanel title="店铺健康" subtitle="按履约率与商品供给综合评估">
    <div class="health-score">
      <el-progress type="dashboard" :percentage="healthScore" :width="118" color="#ff7a1a" />
      <div>
        <strong>{{ healthScore }}分</strong>
        <span>{{ healthLevel }}</span>
      </div>
    </div>
    <div class="health-list">
      <span v-for="item in healthItems" :key="item.label">
        <i :class="item.tone"></i> {{ item.label }}
      </span>
    </div>
  </DashboardPanel>
</template>

<script setup>
import DashboardPanel from './DashboardPanel.vue'

defineProps({
  healthScore: { type: Number, default: 0 },
  /** 由分数派生的等级文案，如「经营状态良好」 */
  healthLevel: { type: String, default: '' },
  /** 健康分构成项，形如 [{ label: '履约率 62%', tone: 'ok' | 'warn' }] */
  healthItems: { type: Array, default: () => [] }
})
</script>

<style scoped>
.health-score {
  display: flex;
  align-items: center;
  gap: 18px;
  padding: 4px 0 14px;
}

.health-score div {
  display: grid;
  gap: 6px;
}

.health-score strong {
  color: #17202b;
  font-size: 24px;
}

.health-score span {
  color: #6b7280;
}

.health-list {
  display: grid;
  gap: 10px;
  color: #4b5563;
}

.health-list span {
  display: flex;
  align-items: center;
  gap: 8px;
}

.health-list i {
  width: 8px;
  height: 8px;
  border-radius: 50%;
}

.health-list .ok {
  background: #22c55e;
}

.health-list .warn {
  background: #ff7a1a;
}
</style>
