<template>
  <div class="data-table-wrapper">
    <!-- 搜索栏 -->
    <div class="table-toolbar" v-if="$slots.toolbar || showSearch">
      <div class="toolbar-left">
        <slot name="toolbar"></slot>
        <el-input
          v-if="showSearch"
          v-model="searchValue"
          :placeholder="searchPlaceholder"
          clearable
          style="width: 240px"
          @keyup.enter="handleSearch"
          @clear="handleSearch"
        >
          <template #prefix>
            <el-icon><Search /></el-icon>
          </template>
        </el-input>
        <el-button v-if="showSearch" type="primary" @click="handleSearch" style="margin-left: 8px">
          搜索
        </el-button>
      </div>
      <div class="toolbar-right">
        <slot name="actions"></slot>
      </div>
    </div>

    <!-- 表格 -->
    <el-table
      :data="data"
      v-loading="loading"
      :border="border"
      :stripe="stripe"
      v-bind="$attrs"
      style="width: 100%"
    >
      <slot></slot>
    </el-table>

    <!-- 分页 -->
    <div class="table-pagination" v-if="showPagination">
      <el-pagination
        v-model:current-page="currentPage"
        v-model:page-size="pageSize"
        :page-sizes="[10, 20, 50, 100]"
        :total="total"
        layout="total, sizes, prev, pager, next, jumper"
        @size-change="handleSizeChange"
        @current-change="handlePageChange"
      />
    </div>
  </div>
</template>

<script setup>
import { ref, watch } from 'vue'
import { Search } from '@element-plus/icons-vue'

const props = defineProps({
  data: { type: Array, default: () => [] },
  total: { type: Number, default: 0 },
  loading: { type: Boolean, default: false },
  showSearch: { type: Boolean, default: false },
  searchPlaceholder: { type: String, default: '请输入关键词搜索' },
  showPagination: { type: Boolean, default: true },
  border: { type: Boolean, default: true },
  stripe: { type: Boolean, default: true },
  page: { type: Number, default: 1 },
  size: { type: Number, default: 10 }
})

const emit = defineEmits(['update:page', 'update:size', 'search', 'page-change', 'size-change'])

const searchValue = ref('')
const currentPage = ref(props.page)
const pageSize = ref(props.size)

watch(() => props.page, (val) => { currentPage.value = val })
watch(() => props.size, (val) => { pageSize.value = val })

const handleSearch = () => {
  emit('search', searchValue.value)
}

const handlePageChange = (page) => {
  currentPage.value = page
  emit('update:page', page)
  emit('page-change', page)
}

const handleSizeChange = (size) => {
  pageSize.value = size
  emit('update:size', size)
  emit('size-change', size)
}
</script>

<style scoped>
.data-table-wrapper {
  background: #fff;
  padding: 20px;
  border-radius: 8px;
}
.table-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
  flex-wrap: wrap;
  gap: 12px;
}
.toolbar-left, .toolbar-right {
  display: flex;
  align-items: center;
  gap: 8px;
}
.table-pagination {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}
</style>
