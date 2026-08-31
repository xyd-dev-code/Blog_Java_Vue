<template>
  <div class="project-mgmt-page">
    <el-tabs v-model="activeTab" class="project-mgmt-tabs">
      <el-tab-pane :label="wx('项目列表')" name="projects">
        <ProjectList />
      </el-tab-pane>
      <el-tab-pane :label="wx('项目分类')" name="categories">
        <ProjectCategoryList />
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup>
import { useWuxiaCopy } from '@/composables/useWuxiaCopy'
const { wx } = useWuxiaCopy()

import { ref, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import ProjectList from './ProjectList.vue'
import ProjectCategoryList from './ProjectCategoryList.vue'

const route = useRoute()
const router = useRouter()
const VALID = ['projects', 'categories']
const activeTab = ref('projects')

watch(activeTab, (v) => {
  if (route.query.tab !== v) router.replace({ query: { ...route.query, tab: v } })
})

onMounted(() => {
  const t = route.query.tab
  if (VALID.includes(t)) activeTab.value = t
})
</script>

<style scoped>
.project-mgmt-page { padding: 0; }
.project-mgmt-tabs :deep(.el-tabs__header) { margin-bottom: 16px; }
</style>