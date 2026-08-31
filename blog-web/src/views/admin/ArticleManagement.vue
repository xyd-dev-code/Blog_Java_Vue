<template>
  <div class="article-mgmt-page">
    <el-tabs v-model="activeTab" class="article-mgmt-tabs">
      <el-tab-pane :label="wx('文章列表')" name="articles">
        <ArticleList />
      </el-tab-pane>
      <el-tab-pane :label="wx('文章分类')" name="categories">
        <CategoryList />
      </el-tab-pane>
      <el-tab-pane label="标签" name="tags">
        <TagList />
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup>
import { useWuxiaCopy } from '@/composables/useWuxiaCopy'
const { wx } = useWuxiaCopy()

import { ref, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import ArticleList from './ArticleList.vue'
import CategoryList from './CategoryList.vue'
import TagList from './TagList.vue'

const route = useRoute()
const router = useRouter()
const VALID = ['articles', 'categories', 'tags']
const activeTab = ref('articles')

watch(activeTab, (v) => {
  if (route.query.tab !== v) router.replace({ query: { ...route.query, tab: v } })
})

onMounted(() => {
  const t = route.query.tab
  if (VALID.includes(t)) activeTab.value = t
})
</script>

<style scoped>
.article-mgmt-page { padding: 0; }
.article-mgmt-tabs :deep(.el-tabs__header) { margin-bottom: 16px; }
</style>