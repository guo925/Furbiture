<template>
  <DashboardPanel title="交易待办" subtitle="按买家履约优先级处理">
    <template #action>
      <router-link to="/merchant/orders">全部订单</router-link>
    </template>
    <div class="todo-list">
      <router-link v-for="todo in todos" :key="todo.label" :to="todo.to" class="todo-item">
        <span>
          <b>{{ todo.value }}</b>
          <small>{{ todo.label }}</small>
        </span>
        <el-icon><ArrowRight /></el-icon>
      </router-link>
    </div>
  </DashboardPanel>
</template>

<script setup>
import { ArrowRight } from '@element-plus/icons-vue'
import DashboardPanel from './DashboardPanel.vue'

defineProps({
  todos: { type: Array, default: () => [] }
})
</script>

<style scoped>
/* 插槽内容作用域属于本组件（而非 DashboardPanel），故 a 的样式在此定义 */
.panel-head a {
  display: inline-flex;
  margin-top: 14px;
  color: #ff7a1a;
  text-decoration: none;
  font-weight: 600;
}

.todo-list {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.todo-item {
  min-height: 86px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  padding: 16px;
  border-radius: 8px;
  background: #f7f9fc;
  color: inherit;
  text-decoration: none;
  border: 1px solid transparent;
}

.todo-item:hover {
  border-color: #ffd7b3;
  background: #fffaf5;
}

.todo-item span {
  display: grid;
  gap: 6px;
}

.todo-item b {
  color: #ff7a1a;
  font-size: 24px;
  line-height: 1;
}

.todo-item small {
  color: #667085;
}

@media (max-width: 720px) {
  .todo-list {
    grid-template-columns: 1fr;
  }
}
</style>
