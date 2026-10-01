<template>
  <DashboardPanel title="品类分布" subtitle="在售商品分类占比">
    <div v-if="!data.length" class="chart-empty">
      <el-empty description="暂无数据" :image-size="60" />
    </div>
    <div v-else ref="chartRef" class="chart-box"></div>
  </DashboardPanel>
</template>

<script setup>
import { nextTick, onMounted, onUnmounted, ref, watch } from 'vue'
import echarts from '../../../utils/echarts'
import DashboardPanel from './DashboardPanel.vue'

const props = defineProps({
  data: { type: Array, default: () => [] }
})

const chartRef = ref(null)
let chart = null

const buildOption = () => ({
  tooltip: { trigger: 'item', formatter: '{b}: {c} ({d}%)' },
  legend: { bottom: 0, textStyle: { color: '#667085' } },
  color: ['#ff7a1a', '#f6ad55', '#2563eb', '#16a34a', '#7c3aed', '#ef4444', '#0ea5e9', '#f59e0b'],
  series: [
    {
      type: 'pie',
      radius: ['40%', '68%'],
      center: ['50%', '44%'],
      avoidLabelOverlap: true,
      itemStyle: { borderRadius: 6, borderColor: '#fff', borderWidth: 2 },
      label: { formatter: '{b}\n{c}件' },
      labelLine: { length: 10, length2: 8 },
      data: props.data.map((i) => ({ name: i.name, value: i.value }))
    }
  ]
})

const handleResize = () => chart?.resize()

const disposeChart = () => {
  window.removeEventListener('resize', handleResize)
  if (chart) {
    chart.dispose()
    chart = null
  }
}

/**
 * 首次 init + 后续 setOption。
 * 本组件在无数据时以 v-else 摘除图表容器，此时必须 dispose，
 * 否则容器重建后旧实例仍指向已脱离文档的 DOM，setOption 不会生效。
 */
const render = () => {
  if (!props.data.length) {
    disposeChart()
    return
  }
  if (!chartRef.value) return
  if (!chart) {
    chart = echarts.init(chartRef.value)
    window.addEventListener('resize', handleResize)
  }
  chart.setOption(buildOption(), true)
}

onMounted(async () => {
  await nextTick()
  render()
})

watch(() => props.data, async () => {
  await nextTick()
  render()
}, { deep: true })

onUnmounted(disposeChart)
</script>

<style scoped>
.chart-box {
  width: 100%;
  height: 280px;
}

.chart-empty {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 280px;
}
</style>
