<template>
  <div class="search-page">
    <HeroSearch :title="'搜索结果'" :subtitle="kw ? subtitleText : '输入关键词开始探索'" />

    <div class="container page-body">
      <!-- 空态：未输入关键词 -->
      <el-empty v-if="!kw" description="请输入关键词以搜索站内内容" :image-size="80" />

      <!-- 已搜索但全空 -->
      <el-empty v-else-if="!loading && grandTotal === 0" description="没有找到相关内容，换个关键词试试" :image-size="80" />

      <template v-else>
        <!-- 文章 -->
        <section v-if="articleList.length" class="search-section">
          <header class="ss-head">
            <span class="ss-icon ss-icon-article">📰</span>
            <h2 class="ss-title">文章</h2>
            <span class="ss-count">{{ articleList.length }} 条</span>
          </header>
          <div class="search-result-grid">
            <PostCardSky
              v-for="(a, i) in articleList"
              :key="a.id"
              :article="a"
              class="reveal"
              :style="{ transitionDelay: `${(i % 6) * 45}ms` }"
            />
          </div>
        </section>

        <!-- 项目（直接复用 ProjectCard） -->
        <section v-if="projectList.length" class="search-section">
          <header class="ss-head">
            <span class="ss-icon ss-icon-project">🚀</span>
            <h2 class="ss-title">项目</h2>
            <span class="ss-count">{{ projectList.length }} 条</span>
          </header>
          <div class="search-card-grid search-card-grid--projects">
            <ProjectCard
              v-for="p in projectList"
              :key="p.id"
              :project="p"
              @detail="goProjectDetail"
            />
          </div>
        </section>

        <!-- 工具（直接复用 ToolCard） -->
        <section v-if="toolList.length" class="search-section">
          <header class="ss-head">
            <span class="ss-icon ss-icon-tool">🔧</span>
            <h2 class="ss-title">工具</h2>
            <span class="ss-count">{{ toolList.length }} 条</span>
          </header>
          <div class="search-card-grid search-card-grid--tools">
            <ToolCard
              v-for="t in toolList"
              :key="t.id"
              :tool="t"
              :categories="toolCategories"
              :is-fav="isToolFav(t.id)"
              @use="onToolUse"
              @fav="onToolFav"
            />
          </div>
        </section>

        <!-- 友链（直接复用 FriendCard） -->
        <section v-if="friendList.length" class="search-section">
          <header class="ss-head">
            <span class="ss-icon ss-icon-friend">🔗</span>
            <h2 class="ss-title">友链</h2>
            <span class="ss-count">{{ friendList.length }} 条</span>
          </header>
          <div class="search-card-grid search-card-grid--friends">
            <a
              v-for="f in friendList"
              :key="f.id"
              :href="f.url"
              target="_blank"
              rel="noopener noreferrer"
              class="friend-card-link"
            >
              <FriendCard :friend="f" :group="f.linkGroup" />
            </a>
          </div>
        </section>

        <!-- 留言（直接复用 CommentItem variant=card） -->
        <section v-if="commentList.length" class="search-section">
          <header class="ss-head">
            <span class="ss-icon ss-icon-comment">💬</span>
            <h2 class="ss-title">留言</h2>
            <span class="ss-count">{{ commentList.length }} 条</span>
          </header>
          <div class="search-comment-grid">
            <CommentItem
              v-for="c in commentList"
              :key="c.id"
              :comment="c"
              variant="card"
              class="reveal"
            />
          </div>
        </section>

        <!-- 总汇提示（仅多类别时显示） -->
        <div v-if="grandSections >= 2" class="search-grand">
          共在 <b>{{ grandSections }}</b> 个类别下找到 <b>{{ grandTotal }}</b> 条匹配结果
        </div>
      </template>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import PostCardSky from '@/components/PostCardSky.vue'
import HeroSearch from '@/components/HeroSearch.vue'
import ProjectCard from '@/components/ProjectCard.vue'
import ToolCard from '@/components/ToolCard.vue'
import FriendCard from '@/components/FriendCard.vue'
import CommentItem from '@/components/CommentItem.vue'
import {
  search,
  projects,
  friendLinks,
  guestbookComments,
  publicTools,
  publicClickTool,
  publicToolCategories
} from '@/api/front'

const route = useRoute()
const router = useRouter()

const kw = ref('')
const loading = ref(false)

// 五类数据
const articleList = ref([])
const projectList = ref([])
const toolList = ref([])
const friendList = ref([])
const commentList = ref([])
// 工具分类（与 Tools.vue 一致，传给 ToolCard 解析分类名，避免显示「其他」）
const toolCategories = ref([{ value: 'all', label: '全部' }])

// 工具收藏（与 Tools.vue 一致的 localStorage）
const toolFavIds = ref([])
try {
  const raw = localStorage.getItem('tool_favs')
  if (raw) toolFavIds.value = JSON.parse(raw) || []
} catch (_) {}
const isToolFav = (id) => toolFavIds.value.includes(id)
const onToolFav = (t) => {
  const i = toolFavIds.value.indexOf(t.id)
  if (i >= 0) toolFavIds.value.splice(i, 1)
  else toolFavIds.value.unshift(t.id)
  try { localStorage.setItem('tool_favs', JSON.stringify(toolFavIds.value)) } catch (_) {}
}

const grandTotal = computed(() =>
  articleList.value.length +
  projectList.value.length +
  toolList.value.length +
  friendList.value.length +
  commentList.value.length
)
const grandSections = computed(() =>
  (articleList.value.length ? 1 : 0) +
  (projectList.value.length ? 1 : 0) +
  (toolList.value.length ? 1 : 0) +
  (friendList.value.length ? 1 : 0) +
  (commentList.value.length ? 1 : 0)
)

const subtitleText = computed(() => {
  if (!kw.value) return ''
  return `关键词：${kw.value}，共 ${grandTotal.value} 条结果`
})

/* ---------------- 跳转路由 ---------------- */
// 项目：站内没有单项目路由，调到项目页并把当前关键词也带上
function goProjectDetail(p) {
  router.push({ name: 'projects', query: { kw: kw.value } })
}
// 工具：复用 Tools.vue 内的 onUse 语义（外链打开，内链路由）
async function onToolUse(t) {
  try { await publicClickTool(t.id) } catch (_) {}
  if (t.type === 1 && /^https?:\/\//i.test(t.url || '')) {
    window.open(t.url, '_blank', 'noopener,noreferrer')
    return
  }
  if (t.url && t.url.startsWith('/')) {
    router.push(t.url)
    return
  }
  ElMessage.warning('该工具尚未配置地址，请联系站长')
}

/* ---------------- 客户端匹配 ---------------- */
function matchKw(item, fields) {
  if (!kw.value) return true
  const k = kw.value.toLowerCase()
  return fields.some((f) => {
    const v = item[f]
    return v != null && String(v).toLowerCase().includes(k)
  })
}

/* ---------------- 主流程 ---------------- */
async function doSearch() {
  const k = kw.value.trim()
  if (k.length < 2) {
    articleList.value = []
    projectList.value = []
    toolList.value = []
    friendList.value = []
    commentList.value = []
    return
  }
  loading.value = true
  if (route.query.kw !== k) router.replace({ name: 'search', query: { kw: k } })

  // 6 个请求并行，任一失败不阻塞其它
  await Promise.allSettled([
    // 0) 工具分类（供 ToolCard 解析分类名，与 Tools.vue 一致）
    publicToolCategories().then((resp) => {
      const arr = Array.isArray(resp?.data) ? resp.data : []
      toolCategories.value = [
        { value: 'all', label: '全部' },
        ...arr.map((c) => ({ value: c.code, label: c.name }))
      ]
    }).catch(() => {
      toolCategories.value = [{ value: 'all', label: '全部' }]
    }),

    // 1) 文章（后端 search）
    search({ kw: k, limit: 30 }).then((resp) => {
      articleList.value = (resp?.data?.records || resp?.data || []).slice(0, 30)
    }).catch(() => { articleList.value = [] }),

    // 2) 项目（前端筛：拉首页大小 100）
    projects({ page: 1, size: 100 }, { silent: true }).then((resp) => {
      const list = Array.isArray(resp?.data?.records) ? resp.data.records
        : Array.isArray(resp?.data) ? resp.data : []
      projectList.value = list.filter((p) =>
        matchKw(p, ['name', 'description', 'techStack'])
      ).slice(0, 12)
    }).catch(() => { projectList.value = [] }),

    // 3) 工具（前端筛：拉全部已发布工具）
    publicTools().then((resp) => {
      const list = Array.isArray(resp?.data) ? resp.data : []
      toolList.value = list.filter((t) =>
        matchKw(t, ['name', 'description', 'slug'])
      ).slice(0, 12)
    }).catch(() => { toolList.value = [] }),

    // 4) 友链（前端筛：拉全部）
    friendLinks().then((resp) => {
      const list = Array.isArray(resp?.data) ? resp.data : []
      friendList.value = list.filter((f) =>
        matchKw(f, ['name', 'url', 'description'])
      ).slice(0, 12)
    }).catch(() => { friendList.value = [] }),

    // 5) 留言（前端筛：拉全部留言）
    guestbookComments().then((resp) => {
      const list = Array.isArray(resp?.data) ? resp.data : []
      commentList.value = list.filter((c) =>
        matchKw(c, ['nickname', 'content'])
      ).slice(0, 8)
    }).catch(() => { commentList.value = [] })
  ])

  loading.value = false
}

// 监听字符串而非 query 对象
watch(() => route.query.kw, (v) => {
  kw.value = v || ''
  doSearch()
}, { immediate: true })
</script>

<style scoped lang="scss">
.page-body {
  padding: 16px 0 60px;
  display: grid;
  gap: 36px;
}

/* 分区标题 */
.search-section {
  display: flex;
  flex-direction: column;
  gap: 18px;
}
.ss-head {
  display: flex;
  align-items: center;
  gap: 10px;
  padding-bottom: 8px;
  border-bottom: 1px dashed rgba(125, 211, 252, 0.45);
}
.ss-icon {
  width: 30px;
  height: 30px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: 8px;
  font-size: 16px;
  color: #fff;
  box-shadow: 0 2px 8px rgba(14, 165, 233, 0.2);
}
.ss-icon-article  { background: linear-gradient(135deg, #38bdf8, #6366f1); }
.ss-icon-project  { background: linear-gradient(135deg, #a855f7, #ec4899); }
.ss-icon-tool     { background: linear-gradient(135deg, #14b8a6, #0ea5e9); }
.ss-icon-friend   { background: linear-gradient(135deg, #f59e0b, #fbbf24); }
.ss-icon-comment  { background: linear-gradient(135deg, #f43f5e, #ec4899); }
.ss-title {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
  color: var(--c-ink);
}
.ss-count {
  font-size: 13px;
  color: var(--c-ink-soft);
}

/* 文章大卡多列密排——与文章列表页 .article-list 一致 */
.search-result-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(min(300px, 100%), 1fr));
  gap: 24px;
}
@media (max-width: 600px) {
  .search-result-grid { grid-template-columns: 1fr; gap: 18px; }
}

/* 工具：复用 Tools.vue 的 .grid 排布（4 列 / 22px gap / 1100→3 / 800→2 / 520→1） */
.search-card-grid--tools {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 22px;
}
@media (max-width: 1100px) { .search-card-grid--tools { grid-template-columns: repeat(3, 1fr); } }
@media (max-width: 800px)  { .search-card-grid--tools { grid-template-columns: repeat(2, 1fr); } }
@media (max-width: 520px)  { .search-card-grid--tools { grid-template-columns: 1fr; } }

/* 项目：复用 Projects.vue 的 .proj-grid 排布（3 列 / 26px gap / 960→2 / 560→1） */
.search-card-grid--projects {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 26px;
}
@media (max-width: 960px) { .search-card-grid--projects { grid-template-columns: repeat(2, 1fr); } }
@media (max-width: 560px) { .search-card-grid--projects { grid-template-columns: 1fr; gap: 18px; } }

/* 友链：复用 Friends.vue 的 .friend-grid 排布（3 列 / 18px gap / 1024→2 / 768→1） */
.search-card-grid--friends {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 18px;
}
@media (max-width: 1024px) { .search-card-grid--friends { grid-template-columns: repeat(2, 1fr); } }
@media (max-width: 768px)  { .search-card-grid--friends { grid-template-columns: 1fr; } }

/* 留言：复用 Guestbook.vue 的 .gb-grid 排布（CSS 多列 column-count: 3 / 16px / 1199→2 / 640→1） */
.search-comment-grid {
  column-count: 3;
  column-gap: 16px;
}
.search-comment-grid > * { break-inside: avoid; margin-bottom: 16px; }
@media (max-width: 1199px) { .search-comment-grid { column-count: 2; } }
@media (max-width: 640px)  { .search-comment-grid { column-count: 1; gap: 12px; } }

/* 友链卡的 <a> 包装 */
.friend-card-link {
  text-decoration: none;
  color: inherit;
  display: flex;
  height: 100%;
}
.friend-card-link > * { flex: 1; }

/* 总汇提示 */
.search-grand {
  text-align: center;
  font-size: 13px;
  color: var(--c-ink-soft);
  padding-top: 12px;
  border-top: 1px dashed rgba(125, 211, 252, 0.4);
}
.search-grand b {
  color: #0ea5e9;
  font-weight: 600;
}

.reveal {
  opacity: 0;
  transform: translateY(24px);
  transition: opacity 0.7s ease, transform 0.7s cubic-bezier(0.16, 1, 0.3, 1);
}
.reveal.visible { opacity: 1; transform: translateY(0); }
</style>
