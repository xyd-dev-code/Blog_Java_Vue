<template>
  <div class="articles-page">
    <HeroArticles title="所有文章" :subtitle="`共 ${total} 篇 · 慢一点，让灵魂跟上脚步`" />

    <!-- 浮光粒子 -->
    <div class="af-motes" aria-hidden="true">
      <span v-for="i in 12" :key="i" class="af-mote"
        :style="{
          left: (8 + (i * 7.5)) + '%',
          animationDelay: (i * 0.8) + 's',
          animationDuration: (6 + (i % 4) * 2) + 's',
          width: (3 + (i % 3)) + 'px',
          height: (3 + (i % 3)) + 'px'
        }"
      ></span>
    </div>

    <!-- 文章搜索 & 装饰条 -->
    <div class="container articles-search-row reveal">
      <div class="articles-search">
        <el-input
          v-model="kw"
          :placeholder="wx('搜索文章标题、内容或标签…（回车搜索）')"
          clearable
          size="default"
          inputmode="search"
          @keyup.enter="doSearch"
          @clear="exitSearch"
        >
          <template #prefix><el-icon><Search /></el-icon></template>
        </el-input>
      </div>

      <!-- 搜索态提示条：结果就地展示，不再跳转独立页面 -->
      <p v-if="searchTip" class="search-tip">{{ searchTip }}</p>
      <div v-else-if="isSearch" class="search-status">
        <span class="ss-text">
          「<b>{{ activeKw }}</b>」找到 <b>{{ results.length }}</b> 篇
        </span>
        <button type="button" class="ss-exit" @click="exitSearch">清除搜索</button>
      </div>

      <div class="sky-divider"></div>
    </div>

    <div class="container page-body">
      <!-- 搜索结果：就地展示，卡片样式与列表一致 -->
      <template v-if="isSearch">
        <div v-if="searching" class="loading">
          <el-skeleton :rows="3" animated />
        </div>
        <el-empty v-else-if="!results.length" :description="wx('没有找到相关文章')" />
        <div v-else class="article-list">
          <PostCardSky
            v-for="(a, i) in results"
            :key="a.id"
            :article="a"
            class="reveal"
            :style="{ transitionDelay: `${(i % 6) * 60}ms` }"
          />
        </div>
      </template>

      <div v-else class="article-list">
        <div v-if="loading" class="loading">
          <el-skeleton :rows="3" animated />
        </div>
        <el-empty v-else-if="!list.length" :description="wx('暂无文章')" />
        <PostCardSky
          v-for="(a, i) in list"
          v-else
          :key="a.id"
          :article="a"
          class="reveal"
          :style="{ transitionDelay: `${(i % 6) * 60}ms` }"
        />
        <div class="pagination" v-if="total > pageSize">
          <el-pagination background layout="prev, pager, next, total"
            :current-page="page" :page-size="pageSize" :total="total"
            @current-change="(p) => { page = p; reload() }" />
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { useWuxiaCopy } from '@/composables/useWuxiaCopy'
const { wx } = useWuxiaCopy()

import { ref, onMounted } from 'vue'
import { Search } from '@element-plus/icons-vue'
import PostCardSky from '@/components/PostCardSky.vue'
import HeroArticles from '@/components/HeroArticles.vue'
import { articles, search } from '@/api/front'
import { useFoldFit } from '@/composables/useFoldFit'

const list = ref([])
const total = ref(0)
const page = ref(1)
const pageSize = ref(12)
const loading = ref(false)

// 首屏折痕对齐：撑高 hero，让第一行文章卡完整可见、第二行藏到折痕下
// pad 需小于卡片行间距（.article-list gap: 24px）；搜索态无 .post-card-sky，
// useFoldFit 取不到卡片会自动还原 hero 自然高度，正好符合预期
const { refit } = useFoldFit({
  hero: '.articles-page .hero-articles',
  item: '.articles-page .post-card-sky',
  min: 150,
  max: 560,
  pad: 14
})

/* ---------- 站内搜索：结果就地展示 ---------- */
const kw = ref('')
const isSearch = ref(false)     // 是否处于搜索态
const activeKw = ref('')        // 已生效的关键词（不随输入框实时变）
const searching = ref(false)
const results = ref([])
const searchTip = ref('')       // 校验提示（后端要求 2~50 字）

const doSearch = async () => {
  const k = kw.value.trim()
  if (!k) return exitSearch()
  if (k.length < 2) {
    searchTip.value = wx('搜索关键词至少 2 个字', '寻卷关键词至少 2 个字')
    return
  }
  searchTip.value = ''
  isSearch.value = true
  activeKw.value = k
  searching.value = true
  try {
    const resp = await search({ kw: k, limit: 50 })
    results.value = resp.data?.records || resp.data || []
  } catch (_) {
    results.value = []
  }
  searching.value = false
  refit()
}

const exitSearch = () => {
  isSearch.value = false
  activeKw.value = ''
  searchTip.value = ''
  results.value = []
  kw.value = ''
  refit()
}

const reload = async () => {
  loading.value = true
  try {
    const resp = await articles({ page: page.value, size: pageSize.value })
    list.value = resp.data?.records || resp.data || []
    total.value = resp.data?.total || list.value.length
  } catch (_) {}
  loading.value = false
  refit()
}

onMounted(reload)
</script>

<style scoped lang="scss">
/* 浮光粒子 */
.af-motes {
  position: fixed;
  inset: 0;
  pointer-events: none;
  z-index: 0;
}
.af-mote {
  position: absolute;
  top: -10px;
  border-radius: 50%;
  background: var(--c-botany-500);
  opacity: 0;
  animation: mote-fall ease-in-out infinite;
  box-shadow: 0 0 6px var(--c-botany-500);
}
@keyframes mote-fall {
  0% { transform: translateY(0) scale(0); opacity: 0; }
  15% { opacity: 0.5; transform: scale(1); }
  85% { opacity: 0.15; }
  100% { transform: translateY(100vh) scale(0.5); opacity: 0; }
}
/* 手机端关掉 12 个连续 CSS 动画粒子，省电省重绘 */
@media (max-width: 600px) {
  .af-motes { display: none; }
}

.articles-page :deep(.hero-articles) {
  margin-bottom: 0;
  padding-bottom: 12px;
}
.articles-search-row {
  margin-top: 6px;
  text-align: center;
}
.articles-search {
  max-width: 520px;
  margin: 0 auto;
  padding: 2px 2px 2px 14px;
  background: rgba(var(--theme-paper-rgb), 0.65);
  border: 1px solid rgba(var(--theme-primary-light-rgb), 0.4);
  border-radius: 999px;
  backdrop-filter: blur(10px);
  -webkit-backdrop-filter: blur(10px);
  box-shadow:
    0 4px 20px rgba(var(--theme-primary-strong-rgb), 0.08),
    inset 0 1px 0 rgba(var(--theme-paper-rgb), 0.7);
  transition: border-color 0.25s ease, box-shadow 0.25s ease, transform 0.25s ease;
}
.articles-search:hover {
  border-color: rgba(var(--theme-primary-light-rgb), 0.7);
  transform: translateY(-1px);
}
.articles-search:focus-within {
  border-color: var(--c-botany-500);
  box-shadow:
    0 8px 28px rgba(var(--theme-primary-strong-rgb), 0.18),
    inset 0 1px 0 rgba(var(--theme-paper-rgb), 0.7);
  transform: translateY(-1px);
}
.articles-search :deep(.el-input__wrapper) {
  background: transparent;
  box-shadow: none !important;
  padding-left: 0;
}
.articles-search :deep(.el-input__inner) {
  color: var(--c-ink);
  font-size: 14px;
}
.articles-search :deep(.el-input__inner::placeholder) {
  color: var(--c-ink-300);
}
/* 前缀搜索图标：与站内 AppHeader/Search.vue 风格一致——无圆、无背景，仅浅蓝着色 */
.articles-search :deep(.el-input__prefix-inner) {
  color: var(--c-botany-700);
  font-size: 16px;
  line-height: 1;
  margin-right: 2px;
}
.articles-search :deep(.el-input__prefix-inner .el-icon),
.articles-search :deep(.el-input__prefix-inner svg) {
  display: block;
  width: 16px;
  height: 16px;
  font-size: 16px;
  line-height: 16px;
}
.articles-search :deep(.el-input__wrapper.is-focus) ~ * .el-input__prefix-inner,
.articles-search:focus-within :deep(.el-input__prefix-inner) {
  color: var(--c-botany-900);
}

@media (max-width: 600px) {
  .articles-search-row { margin-top: 6px; padding: 0 16px; }
  .articles-search { max-width: none; padding: 4px 4px 4px 12px; }
  .articles-search :deep(.el-input__inner) { font-size: 13px; }
  .articles-search :deep(.el-input__inner::placeholder) { font-size: 13px; }
}

/* 搜索态提示条 */
.search-tip,
.search-status {
  margin: 10px auto 0;
  font-size: 13px;
  color: var(--c-ink-soft, var(--c-ink-400));
}
.search-tip { color: var(--c-orange-text); }
.search-status {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
}
.search-status b { color: var(--c-botany-900); font-weight: 600; }
.ss-exit {
  padding: 2px 12px;
  font-size: 12.5px;
  color: var(--c-botany-900);
  background: var(--c-botany-50);
  border: 1px solid var(--c-botany-200);
  border-radius: 999px;
  cursor: pointer;
  transition: background 0.2s ease, border-color 0.2s ease;
}
.ss-exit:hover { background: var(--c-botany-100); border-color: var(--c-botany-500); }

/* 搜索结果复用 .article-list 网格，卡片样式与正常列表完全一致 */

.sky-divider {
  height: 3px;
  margin: 14px auto 0;
  max-width: 320px;
  background: linear-gradient(90deg, transparent, var(--c-botany-500), var(--c-autumn-500), var(--c-botany-500), transparent);
  background-size: 200% 100%;
  border-radius: 999px;
  animation: divider-shimmer 4s ease-in-out infinite;
}
@keyframes divider-shimmer {
  0%, 100% { background-position: 0% 50%; }
  50%      { background-position: 100% 50%; }
}

.article-list {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(min(300px, 100%), 1fr));
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
.reveal.visible {
  opacity: 1;
  transform: translateY(0);
}
</style>
