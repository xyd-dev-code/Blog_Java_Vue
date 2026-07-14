<template>
  <div class="search-page">
    <SkyPageHead title="搜索结果" :subtitle="kw ? `关键词：${kw}，共 ${total} 条结果` : '输入关键词开始探索'" />
    <div class="container-narrow page-body">
      <div class="search-bar reveal">
        <el-input
          v-model="kw"
          placeholder="搜点什么…（回车搜索）"
          size="large"
          clearable
          @keyup.enter="doSearch"
        >
          <template #prefix><el-icon><Search /></el-icon></template>
        </el-input>
      </div>
      <PostCardSky
        v-for="(a, i) in list"
        :key="a.id"
        :article="a"
        class="reveal"
        :style="{ transitionDelay: `${(i % 6) * 60}ms` }"
      />
      <el-empty v-if="!loading && !list.length" :description="kw ? '没有找到相关文章' : '请输入关键词'" />
      <div class="pagination" v-if="total > pageSize">
        <el-pagination background layout="prev, pager, next, total"
          :current-page="page" :page-size="pageSize" :total="total"
          @current-change="(p) => { page = p; doSearch() }" />
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Search } from '@element-plus/icons-vue'
import PostCardSky from '@/components/PostCardSky.vue'
import { search } from '@/api/front'
import SkyPageHead from '@/components/SkyPageHead.vue'

const route = useRoute()
const router = useRouter()
const kw = ref('')
const list = ref([])
const total = ref(0)
const page = ref(1)
const pageSize = ref(10)
const loading = ref(false)

const doSearch = async () => {
  if (!kw.value.trim()) return
  loading.value = true
  router.replace({ name: 'search', query: { kw: kw.value, page: page.value } })
  try {
    const resp = await search({ kw: kw.value, page: page.value, size: pageSize.value })
    list.value = resp.data?.records || resp.data || []
    total.value = resp.data?.total || list.value.length
  } catch (_) {}
  loading.value = false
}

watch(() => route.query, (q) => {
  kw.value = q.kw || ''
  page.value = Number(q.page) || 1
  if (kw.value) doSearch()
}, { immediate: true })
</script>

<style scoped lang="scss">
.page-body {
  padding: 40px 0 60px;
  display: grid;
  gap: 24px;
}
.search-bar {
  position: relative;
  padding: 14px;
  background: rgba(255, 255, 255, 0.6);
  border: 1px solid rgba(125, 211, 252, 0.4);
  border-radius: 18px;
  backdrop-filter: blur(10px);
  box-shadow:
    0 4px 24px rgba(14, 165, 233, 0.08),
    inset 0 1px 0 rgba(255, 255, 255, 0.7);
  transition: border-color 0.25s ease, box-shadow 0.25s ease;
}
.search-bar:focus-within {
  border-color: #38bdf8;
  box-shadow:
    0 8px 32px rgba(14, 165, 233, 0.15),
    inset 0 1px 0 rgba(255, 255, 255, 0.7);
}
.search-bar :deep(.el-input__wrapper) {
  border-radius: 12px;
  background: #fff;
  box-shadow: 0 0 0 1px #e0f2fe inset !important;
}
.search-bar :deep(.el-input__wrapper.is-focus) {
  box-shadow: 0 0 0 1px #38bdf8 inset !important;
}
.pagination {
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