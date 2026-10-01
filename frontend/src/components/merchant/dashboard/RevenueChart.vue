<template>
  <DashboardPanel title="营收趋势" subtitle="近 7 天销售额走势">
    <div ref="chartRef" class="chart-box"></div>
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
  tooltip: {
    trigger: 'axis',
    backgroundColor: '#fff',
    borderColor: '#e8ecf3',
    textStyle: { color: '#17202b' }
  },
  grid: { left: '3%', right: '4%', bottom: '3%', top: '10%', containLabel: true },
  xAxis: {
    type: 'category',
    data: props.data.map((i) => i.date),
    axisLine: { lineStyle: { color: '#e8ecf3' } },
    axisLabel: { color: '#8a94a6' }
  },
  yAxis: {
    type: 'value',
    name: '销售额 (元)',
    splitLine: { lineStyle: { color: '#f7f9fc' } },
    axisLabel: { color: '#8a94a6' }
  },
  series: [
    {
      name: '销售额',
      type: 'line',
      smooth: true,
      symbol: 'circle',
      symbolSize: 7,
      data: props.data.map((i) => i.sales),
      lineStyle: { color: '#ff7a1a', width: 3 },
      itemStyle: { color: '#ff7a1a' },
      areaStyle: {
        color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
          { offset: 0, color: 'rgba(255, 122, 26, 0.25)' },
          { offset: 1, color: 'rgba(255, 122, 26, 0.02)' }
        ])
      }
    }
  ]
})

const handleResize = () => chart?.resize()

/**
 * 首次 init + 后续 setOption（不再每次 dispose 重建）。
 * setOption 第二参 notMerge=true：语义等价于旧实现的「dispose 后重新 init」，
 * 保证数据刷新时旧 series 不残留。
 */
const render = () => {
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

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  if (chart) {
    chart.dispose()
    chart = null
  }
})
</script>

<style scoped>
.chart-box {
  width: 100%;
  height: 280px;
}
</style>
