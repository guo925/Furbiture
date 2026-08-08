<template>
  <span class="star-rating" :class="{ interactive: !readonly }">
    <button
      v-for="n in 5"
      :key="n"
      type="button"
      :disabled="readonly"
      :class="{ filled: n <= displayValue }"
      @click="selectRating(n)"
      @mouseenter="!readonly && (hoverValue = n)"
      @mouseleave="!readonly && (hoverValue = 0)"
      :title="readonly ? `${value} 星` : `${n} 星`"
    >
      <el-icon :size="size">
        <StarFilled v-if="n <= displayValue" />
        <Star v-else />
      </el-icon>
    </button>
    <span v-if="showCount && count > 0" class="count">({{ count }})</span>
    <span v-if="showText" class="rating-text">{{ ratingText }}</span>
  </span>
</template>

<script setup>
import { ref, computed } from 'vue'
import { Star, StarFilled } from '@element-plus/icons-vue'

const props = defineProps({
  modelValue: { type: Number, default: 0 },
  count: { type: Number, default: 0 },
  size: { type: [Number, String], default: 18 },
  readonly: { type: Boolean, default: false },
  showCount: { type: Boolean, default: false },
  showText: { type: Boolean, default: false }
})

const emit = defineEmits(['update:modelValue'])

const hoverValue = ref(0)

const displayValue = computed(() => {
  if (readonly) return Math.round(props.modelValue)
  return hoverValue.value || props.modelValue
})

const ratingText = computed(() => {
  const v = Math.round(props.modelValue)
  return ['', '很差', '较差', '一般', '推荐', '力荐'][v] || ''
})

function selectRating(n) {
  if (props.readonly) return
  emit('update:modelValue', n)
}
</script>

<style scoped>
.star-rating {
  display: inline-flex;
  align-items: center;
  gap: 2px;
}

.star-rating button {
  border: 0;
  background: none;
  padding: 2px;
  cursor: pointer;
  color: #ddd;
  transition: color var(--transition-fast), transform var(--transition-fast);
  line-height: 1;
}

.star-rating.interactive button:hover {
  transform: scale(1.15);
}

.star-rating button:disabled {
  cursor: default;
  color: #ddd;
}

.star-rating button.filled,
.star-rating button.filled:disabled {
  color: #f59e0b;
}

.count {
  margin-left: 6px;
  color: var(--color-text-muted);
  font-size: 13px;
}

.rating-text {
  margin-left: 6px;
  color: #f59e0b;
  font-weight: 600;
  font-size: 14px;
}
</style>
