<template>
  <article class="product-card" @click="$emit('open', product.id)">
    <div class="image-wrap">
      <img :src="imageSrc" :alt="product.name" loading="lazy">
      <span v-if="product.stock <= 0" class="sold-out">暂时缺货</span>
    </div>
    <div class="product-body">
      <h3>{{ product.name }}</h3>
      <p class="desc">{{ product.description || product.brand || '严选家居好物，适配多种户型风格。' }}</p>
      <div class="meta">
        <strong>¥{{ money(product.price) }}</strong>
        <span>已售 {{ product.sales || 0 }}</span>
      </div>
      <div class="tags">
        <em v-if="product.categoryName">{{ product.categoryName }}</em>
        <em v-if="product.brand">{{ product.brand }}</em>
      </div>
      <button type="button" @click.stop="$emit('cart', product.id)" :disabled="product.stock <= 0">
        {{ product.stock > 0 ? '加入购物车' : '到货提醒' }}
      </button>
    </div>
  </article>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  product: {
    type: Object,
    required: true
  }
})

defineEmits(['open', 'cart'])

const imageSrc = computed(() => props.product.mainImage || 'https://images.unsplash.com/photo-1555041469-a586c61ea9bc?auto=format&fit=crop&w=900&q=80')

const money = (value) => Number(value || 0).toFixed(2)
</script>

<style scoped>
.product-card {
  background: #fff;
  border: 1px solid #eee;
  border-radius: 8px;
  overflow: hidden;
  cursor: pointer;
  transition: transform 0.2s ease, box-shadow 0.2s ease, border-color 0.2s ease;
}

.product-card:hover {
  transform: translateY(-3px);
  border-color: #ff5000;
  box-shadow: 0 10px 24px rgba(255, 80, 0, 0.12);
}

.image-wrap {
  position: relative;
  aspect-ratio: 1 / 1;
  background: #f2f2f2;
  overflow: hidden;
}

.image-wrap img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.25s ease;
}

.product-card:hover img {
  transform: scale(1.04);
}

.sold-out {
  position: absolute;
  inset: auto 10px 10px;
  height: 28px;
  border-radius: 4px;
  background: rgba(0, 0, 0, 0.62);
  color: #fff;
  display: grid;
  place-items: center;
  font-size: 13px;
}

.product-body {
  padding: 12px;
}

h3 {
  height: 42px;
  font-size: 15px;
  line-height: 1.4;
  color: #222;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.desc {
  height: 36px;
  margin-top: 6px;
  color: #888;
  font-size: 12px;
  line-height: 1.5;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.meta {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  margin-top: 10px;
}

.meta strong {
  color: #ff5000;
  font-size: 20px;
}

.meta span {
  color: #999;
  font-size: 12px;
}

.tags {
  display: flex;
  gap: 6px;
  height: 24px;
  margin-top: 8px;
  overflow: hidden;
}

.tags em {
  border: 1px solid #ffd8c9;
  color: #ff5000;
  border-radius: 4px;
  padding: 2px 6px;
  font-size: 12px;
  font-style: normal;
  white-space: nowrap;
}

button {
  width: 100%;
  height: 34px;
  margin-top: 10px;
  border: 0;
  border-radius: 4px;
  background: #ff5000;
  color: #fff;
  font-weight: 700;
  cursor: pointer;
}

button:disabled {
  background: #bbb;
  cursor: not-allowed;
}
</style>
