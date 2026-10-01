<template>
  <section class="overview-panel">
    <div class="shop-card">
      <div class="shop-top">
        <div class="shop-avatar">{{ shopInitial }}</div>
        <div>
          <span class="eyebrow">店铺概览</span>
          <h2>{{ shopName }}</h2>
          <p>今天重点处理待发货订单、库存巡检和商品曝光维护。</p>
        </div>
      </div>
      <div class="shop-actions">
        <el-button type="primary" color="#ff7a1a" @click="router.push('/merchant/products')">
          <el-icon><Plus /></el-icon>
          发布商品
        </el-button>
        <el-button @click="router.push('/merchant/orders')">
          <el-icon><Tickets /></el-icon>
          处理订单
        </el-button>
      </div>
    </div>

    <div class="notice-card">
      <div class="notice-title">
        <el-icon><Bell /></el-icon>
        <span>平台提醒</span>
      </div>
      <p>保持商品图片、库存和发货时效完整，可提升买家下单转化。</p>
      <router-link to="/merchant/profile">完善店铺资料</router-link>
    </div>
  </section>
</template>

<script setup>
import { useRouter } from 'vue-router'
import { Bell, Plus, Tickets } from '@element-plus/icons-vue'

defineProps({
  shopName: { type: String, default: '' },
  shopInitial: { type: String, default: '' }
})

const router = useRouter()
</script>

<style scoped>
.overview-panel {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 360px;
  gap: 18px;
}

.shop-card,
.notice-card {
  background: rgba(255, 255, 255, 0.96);
  border: 1px solid #e8ecf3;
  border-radius: 10px;
  box-shadow: 0 12px 28px rgba(17, 24, 39, 0.04);
}

.shop-card {
  min-height: 176px;
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 24px;
  padding: 26px;
  background:
    radial-gradient(circle at right top, rgba(255, 122, 26, 0.12), transparent 32%),
    linear-gradient(135deg, #ffffff 0%, #fff9f3 100%);
}

.shop-top {
  display: flex;
  align-items: flex-start;
  gap: 16px;
}

.shop-avatar {
  width: 58px;
  height: 58px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex: 0 0 auto;
  border-radius: 14px;
  background: linear-gradient(135deg, #ff7a1a, #ffae63);
  color: #fff;
  font-size: 24px;
  font-weight: 800;
}

.eyebrow {
  color: #ff7a1a;
  font-size: 13px;
  font-weight: 700;
}

.shop-card h2 {
  margin: 8px 0 10px;
  color: #17202b;
  font-size: 28px;
}

.shop-card p,
.notice-card p {
  margin: 0;
  color: #6b7280;
  line-height: 1.6;
}

.shop-actions {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

.notice-card {
  padding: 22px;
}

.notice-title {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 12px;
  color: #17202b;
  font-weight: 700;
}

.notice-card a {
  display: inline-flex;
  margin-top: 14px;
  color: #ff7a1a;
  text-decoration: none;
  font-weight: 600;
}

@media (max-width: 1160px) {
  .overview-panel {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 720px) {
  .shop-card {
    flex-direction: column;
  }

  .shop-top {
    flex-direction: column;
  }
}
</style>
