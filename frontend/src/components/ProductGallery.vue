<template>
  <div class="product-gallery">
    <!-- 主图区域（带放大镜） -->
    <div class="main-image-wrapper" ref="mainWrapper" @mousemove="onZoomMove" @mouseleave="onZoomLeave">
      <img :src="currentImage" :alt="productName" class="main-image" ref="mainImage">
      <!-- 放大镜遮罩 -->
      <div v-if="zooming" class="zoom-lens" :style="lensStyle"></div>
    </div>

    <!-- 放大预览 -->
    <div v-if="zooming" class="zoom-preview" :style="{
      backgroundImage: `url(${currentImage})`,
      backgroundPosition: zoomBgPosition,
      backgroundSize: `${zoomBgSize}px ${zoomBgSize}px`
    }"></div>

    <!-- 缩略图列表 -->
    <div v-if="images.length > 1" class="thumbnail-strip">
      <button
        v-for="(img, idx) in images"
        :key="idx"
        :class="{ active: idx === activeIndex }"
        @click="setActive(idx)"
      >
        <img :src="img" :alt="`${productName} - ${idx + 1}`">
      </button>
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'

const props = defineProps({
  images: { type: Array, default: () => [] },
  productName: { type: String, default: '' }
})

const activeIndex = ref(0)
const mainWrapper = ref(null)
const mainImage = ref(null)
const zooming = ref(false)
const lensPosition = ref({ x: 0, y: 0 })
const zoomScale = 2.5

const currentImage = computed(() => {
  return props.images[activeIndex.value] || 'https://images.unsplash.com/photo-1555041469-a586c61ea9bc?auto=format&fit=crop&w=600&q=80'
})

const lensStyle = computed(() => ({
  left: `${lensPosition.value.x}px`,
  top: `${lensPosition.value.y}px`
}))

const zoomBgSize = computed(() => {
  if (!mainImage.value) return 0
  return mainImage.value.naturalWidth * zoomScale
})

const zoomBgPosition = computed(() => {
  const lensW = 100, lensH = 100
  const x = -(lensPosition.value.x * zoomScale - lensW / 2)
  const y = -(lensPosition.value.y * zoomScale - lensH / 2)
  return `${x}px ${y}px`
})

function setActive(idx) {
  activeIndex.value = idx
}

function onZoomMove(e) {
  if (!mainImage.value) return
  const rect = mainImage.value.getBoundingClientRect()
  const lensW = 100, lensH = 100
  let x = e.clientX - rect.left - lensW / 2
  let y = e.clientY - rect.top - lensH / 2
  x = Math.max(0, Math.min(x, rect.width - lensW))
  y = Math.max(0, Math.min(y, rect.height - lensH))
  lensPosition.value = { x, y }
  zooming.value = true
}

function onZoomLeave() {
  zooming.value = false
}
</script>

<style scoped>
.product-gallery {
  display: flex;
  gap: 16px;
  position: relative;
}

.main-image-wrapper {
  position: relative;
  width: 420px;
  height: 420px;
  border-radius: var(--radius-md);
  overflow: hidden;
  cursor: crosshair;
  background: #f5f5f5;
  flex-shrink: 0;
}

.main-image {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

.zoom-lens {
  position: absolute;
  width: 100px;
  height: 100px;
  border: 2px solid rgba(255, 80, 0, 0.5);
  background: rgba(255, 80, 0, 0.08);
  pointer-events: none;
  border-radius: var(--radius-sm);
}

.zoom-preview {
  position: absolute;
  left: 440px;
  top: 0;
  width: 420px;
  height: 420px;
  border-radius: var(--radius-md);
  border: 1px solid var(--color-border);
  background-repeat: no-repeat;
  z-index: 10;
  box-shadow: var(--shadow-lg);
  background-color: #fff;
}

.thumbnail-strip {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.thumbnail-strip button {
  width: 72px;
  height: 72px;
  padding: 2px;
  border: 2px solid transparent;
  border-radius: var(--radius-sm);
  background: none;
  cursor: pointer;
  overflow: hidden;
  transition: border-color var(--transition-fast);
}

.thumbnail-strip button:hover { border-color: var(--color-border); }
.thumbnail-strip button.active { border-color: var(--color-primary); }

.thumbnail-strip img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  border-radius: 2px;
}

@media (max-width: 960px) {
  .product-gallery { flex-direction: column; }
  .main-image-wrapper { width: 100%; height: auto; aspect-ratio: 1; }
  .zoom-preview { display: none; }
  .thumbnail-strip { flex-direction: row; overflow-x: auto; }
  .thumbnail-strip button { width: 56px; height: 56px; flex-shrink: 0; }
}
</style>
