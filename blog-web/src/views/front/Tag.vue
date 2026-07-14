<template>
  <div class="tag-page">
    <SkyPageHead
      :title="`# ${tag?.name || route.params.slug}`"
      :subtitle="tag?.description || (tag ? `${tag.name} 相关文章` : '')"
    />
    <div class="container page-body">
      <PostCardSky
        v-for="(a, i) in list"
        :key="a.id"
        :article="a"
        class="reveal"
        :style="{ transitionDelay: `${(i % 6) * 60}ms` }"
      />
      <el-empty v-if="!loading && !list.length" description="该标签下暂无文章" />
      <div class="pagination" v-if="total > pageSize">
        <el-pagination background layout="prev, pager, next, total"
          :current-page="page" :page-size="pageSize" :total="total"
          @current-change="(p) => { page = p; load() }" />
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import PostCardSky from '@/components/PostCardSky.vue'
import { articlesByTag, tagsAll } from '@/api/front'
import SkyPageHead from '@/components/SkyPageHead.vue'

const route = useRoute()
const tag = ref(null)
const list = ref([])
const total = ref(0)
const page = ref(1)
const pageSize = ref(12)
const loading = ref(false)

const load = async () => {
  loading.value = true
  try {
    const resp = await articlesByTag(route.params.slug, { page: page.value, size: pageSize.value })
    list.value = resp.data?.records || resp.data || []
    total.value = resp.data?.total || list.value.length
  } catch (_) {}
  loading.value = false
}

watch(() => route.params.slug, async () => {
  page.value = 1
  tag.value = null
  try {
    const resp = await tagsAll()
    tag.value = (resp.data || []).find(t => t.slug === route.params.slug)
  } catch (_) {}
  load()
}, { immediate: true })
</script>

<style scoped lang="scss">
.page-body {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(min(280px, 100%), 1fr));
  gap: 24px;
  padding: 40px 0 60px;
}
.pagination {
  grid-column: 1 / -1;
  display: flex;
  justify-content: center;
  margin-top: 20px;
}
.reveal {
  opacity: 0;
  transform: translateY(24px);
  transition: opacity 0.7s ease, transform 0.7s cubic-bezier(0.16, 1, 0.3, 1);
}
.reveal.visible { opacity: 1; transform: translateY(0); }
</style>