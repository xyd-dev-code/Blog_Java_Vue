<template>
  <div class="tool-mgmt-page">
    <el-tabs v-model="activeTab" class="tool-mgmt-tabs">
      <el-tab-pane :label="wx('工具列表')" name="items">
        <ToolItemList />
      </el-tab-pane>
      <el-tab-pane :label="wx('工具分类')" name="categories">
        <ToolCategoryList />
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup>
import { useWuxiaCopy } from '@/composables/useWuxiaCopy'
const { wx } = useWuxiaCopy()

import { ref, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import ToolItemList from './ToolItemList.vue'
import ToolCategoryList from './ToolCategoryList.vue'

const route = useRoute()
const router = useRouter()
const VALID = ['items', 'categories']
const activeTab = ref('items')

watch(activeTab, (v) => {
  if (route.query.tab !== v) router.replace({ query: { ...route.query, tab: v } })
})

onMounted(() => {
  const t = route.query.tab
  if (VALID.includes(t)) activeTab.value = t
})
</script>

<style scoped>
.tool-mgmt-page { padding: 0; }
.tool-mgmt-tabs :deep(.el-tabs__header) { margin-bottom: 16px; }
</style>