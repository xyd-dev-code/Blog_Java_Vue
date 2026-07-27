<template>
  <div class="article-detail" v-if="article">
    <!-- 阅读进度条 -->
    <div class="art-progress" :style="{ width: progressPercent + '%' }"></div>

    <!-- 顶部 Hero：标题 + 元信息 + 封面 -->
    <header class="art-hero" :class="{ 'has-cover': !!article.coverImage }">
      <div class="hero-bg">
        <div class="orb orb-1"></div>
        <div class="orb orb-2"></div>
        <svg class="cloud cloud-1" viewBox="0 0 200 80" preserveAspectRatio="none">
          <path fill="rgba(255,255,255,.18)" d="M40,60 Q50,30 70,35 Q80,10 110,15 Q130,0 160,25 Q185,20 190,45 Q200,60 180,65 L30,65 Z"/>
        </svg>
        <svg class="cloud cloud-2" viewBox="0 0 160 60" preserveAspectRatio="none">
          <path fill="rgba(255,255,255,.12)" d="M20,45 Q35,20 55,28 Q65,8 90,12 Q105,0 130,18 Q150,15 155,35 Q160,45 140,48 L15,48 Z"/>
        </svg>
        <div v-if="article.coverImage" class="hero-cover" :style="{ backgroundImage: `url(${article.coverImage})` }"></div>
        <div class="hero-cover-mask"></div>
      </div>

      <div class="container-narrow hero-inner reveal">
        <div class="art-tags">
          <router-link v-for="t in article.tags" :key="t.id" :to="`/tags/${t.slug}`" class="tag-pill">
            # {{ t.name }}
          </router-link>
          <router-link v-if="article.categoryName" :to="`/categories/${article.categorySlug}`" class="tag-pill tag-pill-sun">
            <el-icon><Folder /></el-icon>&nbsp;{{ article.categoryName }}
          </router-link>
        </div>
        <h1 class="art-title">{{ article.title }}</h1>
        <p class="art-deck" v-if="article.summary">{{ article.summary }}</p>

        <!-- 作者条（晴空 v2：渐变描边 + 头像） -->
        <GradientBorderCard variant="mix" class="art-author-bar">
          <div class="author-row">
            <div class="author-left">
              <img v-if="authorAvatar" :src="authorAvatar" class="avatar avatar-img" alt="" />
              <div v-else class="avatar avatar-fallback">{{ initial }}</div>
              <div class="author-text">
                <div class="author-name">{{ authorName }}</div>
                <div class="author-meta">
                  <span><el-icon><Calendar /></el-icon>&nbsp;{{ fmtDate(article.createTime) }}</span>
                  <span class="dot">·</span>
                  <span><el-icon><View /></el-icon>&nbsp;{{ article.viewCount || 0 }} 阅读</span>
                  <span class="dot">·</span>
                  <span><el-icon><ChatDotRound /></el-icon>&nbsp;{{ article.commentCount || 0 }} 评论</span>
                  <span v-if="readMinutes" class="dot">·</span>
                  <span v-if="readMinutes">⏱ {{ readMinutes }} 分钟</span>
                </div>
              </div>
            </div>
            <div class="author-right">
              <span class="section-eyebrow">today · {{ location }}</span>
              <div class="weather-line">{{ desc }} · {{ temp ?? '--' }}°</div>
            </div>
          </div>
        </GradientBorderCard>
      </div>

      <svg class="wave" viewBox="0 0 1440 80" preserveAspectRatio="none">
        <path d="M0,40 C240,80 480,0 720,40 C960,80 1200,0 1440,40 L1440,80 L0,80 Z" fill="#ffffff" />
      </svg>
    </header>

    <div class="container-narrow art-body">
      <!-- 正文 -->
      <article class="art-content markdown-body" v-html="rendered"></article>

      <!-- 标签条（正文末尾） -->
      <div class="art-tags-foot reveal" v-if="article.tags?.length">
        <span class="tags-label">本文标签：</span>
        <router-link v-for="t in article.tags" :key="t.id" :to="`/tags/${t.slug}`" class="tag-pill">
          # {{ t.name }}
        </router-link>
      </div>

      <!-- 上下篇 -->
      <div class="art-footer-nav">
        <router-link v-if="prev" :to="`/articles/${prev.slug}`" class="nav-card reveal">
          <GradientBorderCard variant="sky" hoverable class="nav-inner">
            <span class="lbl">← 上一篇</span>
            <span class="t">{{ prev.title }}</span>
          </GradientBorderCard>
        </router-link>
        <router-link v-if="next" :to="`/articles/${next.slug}`" class="nav-card reveal">
          <GradientBorderCard variant="sun" hoverable class="nav-inner nav-next">
            <span class="lbl">下一篇 →</span>
            <span class="t">{{ next.title }}</span>
          </GradientBorderCard>
        </router-link>
      </div>

      <RelatedArticles :list="related" />

      <CommentSection :article-id="article.id" />
    </div>
  </div>
  <div v-else class="loading-page">
    <el-skeleton :rows="8" animated />
  </div>
</template>

<script setup>
import { ref, computed, watch, onMounted, onUnmounted } from 'vue'
import { useRoute } from 'vue-router'
import { Calendar, View, ChatDotRound, Folder } from '@element-plus/icons-vue'
import { articleBySlug } from '@/api/front'
import { renderMarkdown } from '@/utils/markdown'
import { fmtDate } from '@/utils/format'
import { useAuthor } from '@/composables/useAuthor'
import { useWeather } from '@/composables/useWeather'
import GradientBorderCard from '@/components/GradientBorderCard.vue'
import RelatedArticles from '@/components/RelatedArticles.vue'
import CommentSection from '@/components/CommentSection.vue'

const route = useRoute()
const article = ref(null)
const prev = ref(null)
const next = ref(null)
const related = ref([])

const { authorName, authorAvatar, initial } = useAuthor(article)
const { temp, desc, location } = useWeather()

const rendered = computed(() => renderMarkdown(article.value?.content || ''))

const readMinutes = computed(() => {
  const words = (article.value?.content || '').length
  if (!words) return null
  return Math.max(1, Math.round(words / 400))
})

// 阅读进度
const progressPercent = ref(0)
const onScroll = () => {
  const h = document.documentElement
  const total = h.scrollHeight - h.clientHeight
  progressPercent.value = total > 0 ? Math.min(100, (h.scrollTop / total) * 100) : 0
}

const load = async () => {
  article.value = null
  try {
    const resp = await articleBySlug(route.params.slug)
    article.value = resp.data?.article
    prev.value = resp.data?.prev
    next.value = resp.data?.next
    related.value = resp.data?.related || []
  } catch (_) {}
}

watch(() => route.params.slug, load)
onMounted(() => { load(); window.addEventListener('scroll', onScroll, { passive: true }) })
onUnmounted(() => { window.removeEventListener('scroll', onScroll) })
</script>

<style scoped lang="scss">
/* ===== 阅读进度条 ===== */
.art-progress {
  position: fixed;
  top: 0;
  left: 0;
  height: 3px;
  background: linear-gradient(90deg, #38bdf8, #22d3ee, #fbbf24);
  z-index: 1000;
  transition: width 0.15s ease-out;
  border-radius: 0 2px 2px 0;
  box-shadow: 0 0 8px rgba(56,189,248,0.4);
}

/* ===== Hero 区 ===== */
.art-hero {
  position: relative;
  overflow: hidden;
  padding: 60px 0 80px;
  background: linear-gradient(180deg, #e0f7ff 0%, #f0f9ff 60%, #ffffff 100%);
}
.art-hero.has-cover {
  background: #0c4a6e;
  color: #f0f9ff;
}
.hero-bg { position: absolute; inset: 0; pointer-events: none; }

/* 封面叠加层:背景图 + 暗色遮罩,保留底层 orb/cloud 氛围 */
.hero-cover {
  position: absolute;
  inset: 0;
  background-size: cover;
  background-position: center;
  background-repeat: no-repeat;
  opacity: 0.42;
  filter: saturate(1.05) blur(2px);
  transform: scale(1.05);
}
.hero-cover-mask {
  position: absolute;
  inset: 0;
  background:
    linear-gradient(180deg, rgba(12, 74, 110, 0.45) 0%, rgba(12, 74, 110, 0.25) 50%, rgba(12, 74, 110, 0.55) 100%),
    radial-gradient(ellipse at center, transparent 0%, rgba(12, 74, 110, 0.4) 100%);
}
.has-cover .orb { opacity: 0.25; mix-blend-mode: screen; }
.has-cover .cloud path { fill: rgba(255, 255, 255, 0.22); }
.orb {
  position: absolute;
  border-radius: 50%;
  filter: blur(80px);
  opacity: 0.55;
}
.orb-1 {
  width: 360px; height: 360px;
  background: radial-gradient(circle, #38bdf8, transparent 70%);
  top: -60px; right: -40px;
  animation: orb-float 12s ease-in-out infinite;
}
.orb-2 {
  width: 280px; height: 280px;
  background: radial-gradient(circle, #fbbf24, transparent 70%);
  bottom: -40px; left: -40px;
  opacity: 0.35;
  animation: orb-float 16s ease-in-out infinite -3s;
}
@keyframes orb-float {
  0%, 100% { transform: translate(0, 0) scale(1); }
  50%      { transform: translate(20px, -15px) scale(1.05); }
}
.cloud {
  position: absolute;
  pointer-events: none;
}
.cloud-1 { top: 12%; right: 8%; width: 180px; animation: cloud-drift 14s ease-in-out infinite; }
.cloud-2 { top: 38%; left: 6%; width: 140px; animation: cloud-drift 18s ease-in-out infinite -4s; opacity: 0.7; }
@keyframes cloud-drift {
  0%, 100% { transform: translateX(0); }
  50%      { transform: translateX(24px); }
}

.hero-inner { position: relative; z-index: 2; text-align: center; }

/* ===== 标签 / 标题 / 摘要 ===== */
.art-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  justify-content: center;
  margin-bottom: 18px;
}
.tag-pill {
  display: inline-flex;
  align-items: center;
  padding: 4px 12px;
  background: rgba(255, 255, 255, 0.65);
  color: #0369a1;
  border: 1px solid rgba(125, 211, 252, 0.6);
  border-radius: 999px;
  font-size: 12px;
  backdrop-filter: blur(6px);
  transition: all 0.2s ease;
}
.tag-pill:hover {
  background: #38bdf8;
  color: #fff;
  border-color: #38bdf8;
}
.tag-pill-sun {
  color: #b45309;
  border-color: #fde68a;
  background: rgba(255, 251, 235, 0.7);
}
.tag-pill-sun:hover {
  background: #fbbf24;
  color: #fff;
  border-color: #fbbf24;
}

.art-title {
  font-family: var(--font-serif);
  font-size: 42px;
  line-height: 1.3;
  margin: 0 0 14px;
  color: var(--c-ink);
  background: linear-gradient(135deg, #0369a1 0%, #38bdf8 60%, #22d3ee 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
          background-clip: text;
}
.art-deck {
  color: var(--c-ink-soft);
  font-size: 15px;
  line-height: 1.85;
  max-width: 640px;
  margin: 0 auto 28px;
}

/* 有封面时:hero 文本切到浅色 + 玻璃质卡片 */
.has-cover .art-title {
  background: linear-gradient(135deg, #ffffff 0%, #bae6fd 60%, #fde68a 100%);
  -webkit-background-clip: text;
          background-clip: text;
  -webkit-text-fill-color: transparent;
  text-shadow: 0 2px 18px rgba(12, 74, 110, 0.55);
}
.has-cover .art-deck { color: rgba(240, 249, 255, 0.92); }
.has-cover .tag-pill {
  background: rgba(255, 255, 255, 0.22);
  color: #f0f9ff;
  border-color: rgba(186, 230, 253, 0.55);
  backdrop-filter: blur(8px);
}
.has-cover .tag-pill:hover { background: #38bdf8; color: #fff; }
.has-cover .tag-pill-sun {
  color: #fde68a;
  border-color: rgba(253, 230, 138, 0.55);
  background: rgba(255, 251, 235, 0.18);
}
.has-cover .tag-pill-sun:hover { background: #fbbf24; color: #fff; }
.has-cover .section-eyebrow { color: #fde68a; }
.has-cover .weather-line { color: #f0f9ff; }
.has-cover .author-name { color: #f0f9ff; }
.has-cover .author-meta { color: rgba(240, 249, 255, 0.78); }
.has-cover .author-meta .dot { color: rgba(240, 249, 255, 0.5); }
.has-cover .avatar { box-shadow: 0 4px 12px rgba(12, 74, 110, 0.5); }

/* 穿透 GradientBorderCard:深色玻璃底 + 内部浅色文字 */
.has-cover :deep(.gradient-border-card) {
  background: rgba(15, 23, 42, 0.55);
  backdrop-filter: blur(14px);
  -webkit-backdrop-filter: blur(14px);
  border: 1px solid rgba(186, 230, 253, 0.35);
}
.has-cover :deep(.gradient-border-card) .author-name { color: #f0f9ff; }
.has-cover :deep(.gradient-border-card) .author-meta { color: rgba(240, 249, 255, 0.85); }
.has-cover :deep(.gradient-border-card) .author-meta .dot { color: rgba(240, 249, 255, 0.55); }
.has-cover :deep(.gradient-border-card) .section-eyebrow { color: #fde68a; }
.has-cover :deep(.gradient-border-card) .weather-line { color: #f0f9ff; }

/* ===== 作者条（渐变描边卡内嵌） ===== */
.art-author-bar {
  max-width: 680px;
  margin: 0 auto;
  padding: 18px 22px;
}
.author-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 18px;
  flex-wrap: wrap;
}
.author-left {
  display: flex;
  align-items: center;
  gap: 12px;
}
.avatar {
  width: 48px; height: 48px;
  border-radius: 50%;
  background: linear-gradient(135deg, #38bdf8, #22d3ee);
  color: #fff;
  font-family: var(--font-serif);
  font-size: 22px;
  display: grid; place-items: center;
  box-shadow: 0 4px 12px rgba(56, 189, 248, 0.35);
  flex-shrink: 0;
}
.avatar-img { object-fit: cover; }
.avatar-fallback { font-weight: 600; }

.author-name {
  font-family: var(--font-serif);
  font-size: 16px;
  font-weight: 600;
  color: var(--c-ink);
}
.author-meta {
  display: flex;
  align-items: center;
  gap: 6px;
  flex-wrap: wrap;
  font-size: 12px;
  color: var(--c-ink-soft);
  margin-top: 4px;
}
.author-meta .dot { color: #cbd5e1; }

.author-right {
  text-align: right;
}
.section-eyebrow {
  display: block;
  font-size: 10px;
  letter-spacing: 0.2em;
  text-transform: uppercase;
  color: #06b6d4;
  font-weight: 600;
  margin-bottom: 4px;
}
.weather-line {
  font-family: var(--font-serif);
  font-size: 16px;
  color: #0369a1;
  font-weight: 600;
}

/* ===== 波浪分隔 ===== */
.wave {
  position: absolute;
  bottom: -1px; left: 0; right: 0;
  width: 100%; height: 60px;
}

/* ===== 正文容器 ===== */
.art-body { padding: 40px 0 80px; position: relative; }
.cover {
  margin-bottom: 36px;
  border-radius: 18px;
  overflow: hidden;
  box-shadow: 0 12px 36px rgba(14, 165, 233, 0.15);
}
.cover img { width: 100%; display: block; }

.art-content {
  font-size: 16px;
  line-height: 1.95;
  color: var(--c-ink);
  padding: 16px 0;
}

/* Markdown 样式（晴空 v2 配色） */
:deep(.markdown-body) h1, :deep(.markdown-body) h2, :deep(.markdown-body) h3 {
  margin: 36px 0 16px;
  font-family: var(--font-serif);
  color: #0369a1;
  font-weight: 600;
}
:deep(.markdown-body) h2 {
  font-size: 26px;
  padding-bottom: 10px;
  border-bottom: 1px dashed #bae6fd;
  position: relative;
}
:deep(.markdown-body) h2::before {
  content: '';
  position: absolute;
  left: 0; bottom: -1px;
  width: 60px; height: 3px;
  background: linear-gradient(90deg, #38bdf8, #fbbf24);
  border-radius: 2px;
}
:deep(.markdown-body) h3 {
  font-size: 20px;
  color: #0c4a6e;
  padding-left: 12px;
  border-left: 4px solid #fbbf24;
}
:deep(.markdown-body) p { margin: 18px 0; }
:deep(.markdown-body) img {
  max-width: 100%;
  border-radius: 12px;
  box-shadow: 0 6px 24px rgba(15, 23, 42, 0.08);
  margin: 20px 0;
}
:deep(.markdown-body) code {
  background: rgba(56, 189, 248, 0.1);
  padding: 2px 8px;
  border-radius: 5px;
  font-size: 14px;
  color: #0369a1;
  border: 1px solid rgba(56, 189, 248, 0.2);
}
:deep(.markdown-body) pre {
  background: linear-gradient(135deg, #0f172a 0%, #1e293b 100%);
  color: #e2e8f0;
  padding: 20px 24px;
  border-radius: 12px;
  overflow-x: auto;
  line-height: 1.7;
  border: 1px solid rgba(125, 211, 252, 0.2);
  box-shadow: 0 8px 24px rgba(15, 23, 42, 0.15);
  margin: 24px 0;
}
:deep(.markdown-body) pre code {
  background: transparent;
  color: inherit;
  padding: 0;
  border: none;
}
:deep(.markdown-body) blockquote {
  margin: 24px 0;
  padding: 14px 22px;
  background: linear-gradient(90deg, rgba(56, 189, 248, 0.08), transparent);
  border-left: 4px solid #38bdf8;
  color: var(--c-ink-soft);
  border-radius: 0 10px 10px 0;
  font-style: italic;
}
:deep(.markdown-body) ul, :deep(.markdown-body) ol {
  padding-left: 26px;
  margin: 14px 0;
}
:deep(.markdown-body) li { margin: 8px 0; }
:deep(.markdown-body) a {
  color: #0ea5e9;
  border-bottom: 1px dashed #7dd3fc;
  transition: color 0.2s ease;
}
:deep(.markdown-body) a:hover { color: #0369a1; border-bottom-color: #0369a1; }
:deep(.markdown-body) table {
  width: 100%;
  border-collapse: separate;
  border-spacing: 0;
  margin: 28px 0;
  border-radius: 10px;
  overflow: hidden;
  border: 2px solid #bae6fd;
  box-shadow: 0 4px 16px rgba(14, 165, 233, 0.08);
  background: #ffffff;
}
:deep(.markdown-body) th, :deep(.markdown-body) td {
  padding: 12px 16px;
  border: 1px solid #bae6fd;
  font-size: 15px;
  line-height: 1.6;
}
:deep(.markdown-body) th {
  background: linear-gradient(135deg, #0c4a6e 0%, #0369a1 100%);
  font-weight: 600;
  color: #ffffff;
  text-align: left;
  letter-spacing: 0.02em;
  border-color: #0c4a6e;
}
:deep(.markdown-body) td {
  color: #0f172a;
  vertical-align: top;
}
:deep(.markdown-body) tbody tr:nth-child(even) td {
  background: #f0f9ff;
}
:deep(.markdown-body) tbody tr:hover td {
  background: #e0f7ff;
  transition: background 0.15s ease;
}

/* 标签条 */
.art-tags-foot {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
  margin: 36px 0 0;
  padding: 16px 20px;
  background: rgba(255, 255, 255, 0.6);
  border: 1px dashed #bae6fd;
  border-radius: 12px;
}
.tags-label {
  font-size: 13px;
  color: var(--c-ink-soft);
  margin-right: 4px;
}

/* 上下篇 */
.art-footer-nav {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 18px;
  margin: 40px 0;
}
.nav-card {
  display: block;
  text-decoration: none;
}
.nav-inner {
  padding: 16px 20px;
  display: flex;
  flex-direction: column;
  gap: 6px;
  min-height: 80px;
}
.nav-next { text-align: right; align-items: flex-end; }
.lbl { font-size: 11px; letter-spacing: 0.1em; color: #06b6d4; font-weight: 600; }
.t {
  font-family: var(--font-serif);
  font-size: 15px;
  color: var(--c-ink);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  max-width: 100%;
  transition: color 0.2s ease;
}
.nav-card:hover .t { color: #0369a1; }

/* Reveal */
.reveal {
  opacity: 0;
  transform: translateY(20px);
  transition: opacity 0.7s ease, transform 0.7s cubic-bezier(0.16, 1, 0.3, 1);
}
.reveal.visible { opacity: 1; transform: translateY(0); }

.loading-page { padding: 80px 0; }

@media (max-width: 768px) {
  .art-title { font-size: 28px; }
  .art-footer-nav { grid-template-columns: 1fr; }
  .nav-next { text-align: left; align-items: flex-start; }
  .author-right { text-align: left; }
}
@media (max-width: 480px) {
  .art-title { font-size: 22px; line-height: 1.3; }
  .art-content { font-size: 15px; }
  :deep(.markdown-body) pre { padding: 14px 16px; font-size: 13px; }
  :deep(.markdown-body) blockquote { padding: 12px 16px; font-size: 14px; }
  .art-author { flex-direction: column; text-align: center; padding: 24px 18px; }
  .author-avatar { width: 80px; height: 80px; }
  .nav-card { padding: 16px 18px; }
}
</style>