<template>
  <div class="comment-mgmt-page">
    <el-tabs v-model="activeTab" class="comment-tabs">
      <el-tab-pane :label="wx('评论')" name="comment">
        <CommentList />
      </el-tab-pane>
      <el-tab-pane label="举报" name="report">
        <ReportList />
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup>
import { useWuxiaCopy } from '@/composables/useWuxiaCopy'
const { wx } = useWuxiaCopy()

import { ref, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import CommentList from './CommentList.vue'
import ReportList from './ReportList.vue'

const route = useRoute()
const router = useRouter()
const VALID = ['comment', 'report']
const activeTab = ref('comment')

watch(activeTab, (v) => {
  if (route.query.tab !== v) router.replace({ query: { ...route.query, tab: v } })
})

onMounted(() => {
  const t = route.query.tab
  if (VALID.includes(t)) activeTab.value = t
})
</script>

<style scoped>
.comment-mgmt-page { padding: 0; }
.comment-tabs :deep(.el-tabs__header) { margin-bottom: 16px; }
</style>