<template>
  <div>
    <div class="seller-dashboard">
      <ShopOverviewCard :shop-name="shopName" :shop-initial="shopInitial" />
      <MetricGrid :metrics="metrics" />

      <section class="dashboard-columns">
        <TodoPanel :todos="todos" />
        <ShopHealthPanel
          :health-score="healthScore"
          :health-level="healthLevel"
          :health-items="healthItems"
        />
      </section>

      <section class="dashboard-columns bottom-columns">
        <ShortcutPanel :shortcuts="shortcuts" />
        <TrendPanel
          :complete-rate="completeRate"
          :product-rate="productRate"
          :pending-rate="pendingRate"
          :completed="orderFunnel.completed"
          :product-count="stats.productCount"
          :paid="orderFunnel.paid"
        />
      </section>

      <section class="chart-grid">
        <RevenueChart :data="revenueTrend" />
        <CategoryChart :data="categoryDistribution" />
      </section>

      <HotProductsPanel :products="hotProducts" />
    </div>
  </div>
</template>

<script setup>
import { onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '../../stores/user'
import { useDashboardData } from '../../composables/useDashboardData'
import ShopOverviewCard from '../../components/merchant/dashboard/ShopOverviewCard.vue'
import MetricGrid from '../../components/merchant/dashboard/MetricGrid.vue'
import TodoPanel from '../../components/merchant/dashboard/TodoPanel.vue'
import ShopHealthPanel from '../../components/merchant/dashboard/ShopHealthPanel.vue'
import ShortcutPanel from '../../components/merchant/dashboard/ShortcutPanel.vue'
import TrendPanel from '../../components/merchant/dashboard/TrendPanel.vue'
import RevenueChart from '../../components/merchant/dashboard/RevenueChart.vue'
import CategoryChart from '../../components/merchant/dashboard/CategoryChart.vue'
import HotProductsPanel from '../../components/merchant/dashboard/HotProductsPanel.vue'

const router = useRouter()
const userStore = useUserStore()

const {
  stats,
  revenueTrend,
  categoryDistribution,
  orderFunnel,
  hotProducts,
  shopName,
  shopInitial,
  healthScore,
  healthLevel,
  healthItems,
  completeRate,
  productRate,
  pendingRate,
  metrics,
  todos,
  shortcuts,
  loadStats
} = useDashboardData()

onMounted(async () => {
  if (!userStore.isAuthenticated) {
    router.push('/login')
    return
  }
  await loadStats()
})
</script>

<style scoped>
.seller-dashboard {
  display: grid;
  gap: 18px;
}

.dashboard-columns {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 360px;
  gap: 18px;
}

.bottom-columns {
  grid-template-columns: minmax(0, 1fr) minmax(360px, 0.62fr);
}

.chart-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.4fr) minmax(0, 1fr);
  gap: 18px;
}

@media (max-width: 1160px) {
  .dashboard-columns,
  .bottom-columns,
  .chart-grid {
    grid-template-columns: 1fr;
  }
}
</style>
