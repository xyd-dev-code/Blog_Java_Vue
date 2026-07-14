<template>
  <div class="page-detail" v-if="page">
    <SkyPageHead v-if="page" :title="page.title" />
    <div class="container-narrow page-body markdown-body" v-html="rendered"></div>
  </div>
  <div v-else class="loading-page container-narrow"><el-skeleton :rows="8" animated /></div>
</template>

<script setup>
import { ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { pageBySlug } from '@/api/front'
import { renderMarkdown } from '@/utils/markdown'
import SkyPageHead from '@/components/SkyPageHead.vue'

const route = useRoute()
const page = ref(null)

const rendered = ref('')

watch(() => route.params.slug, async (slug) => {
  if (!slug) slug = route.query.slug || 'about'
  try {
    const resp = await pageBySlug(slug)
    page.value = resp.data
    rendered.value = renderMarkdown(resp.data?.contentMd || resp.data?.content || '')
  } catch (_) {}
}, { immediate: true })
</script>

<style scoped lang="scss">
.page-body {
  font-size: 16px;
  line-height: 1.9;
  padding: 30px 0 60px;
}
:deep(.markdown-body) h2 { font-size: 24px; color: var(--c-botany-700); margin: 30px 0 14px; }
:deep(.markdown-body) p { margin: 14px 0; }
.loading-page { padding: 80px 24px; }
</style>
