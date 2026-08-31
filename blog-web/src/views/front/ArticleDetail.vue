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
          <path fill="rgba(var(--theme-paper-rgb), .18)" d="M40,60 Q50,30 70,35 Q80,10 110,15 Q130,0 160,25 Q185,20 190,45 Q200,60 180,65 L30,65 Z"/>
        </svg>
        <svg class="cloud cloud-2" viewBox="0 0 160 60" preserveAspectRatio="none">
          <path fill="rgba(var(--theme-paper-rgb), .12)" d="M20,45 Q35,20 55,28 Q65,8 90,12 Q105,0 130,18 Q150,15 155,35 Q160,45 140,48 L15,48 Z"/>
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
              <img v-if="authorAvatar" :src="authorAvatar" class="avatar avatar-img" alt="" loading="lazy" decoding="async" />
              <div v-else class="avatar avatar-fallback">{{ initial }}</div>
              <div class="author-text">
                <div class="author-name">{{ authorName }}</div>
                <div class="author-meta">
                  <span><el-icon><Calendar /></el-icon>&nbsp;{{ fmtDate(article.createTime) }}</span>
                  <span class="dot">·</span>
                  <span><el-icon><View /></el-icon>&nbsp;{{ article.viewCount || 0 }} 阅读</span>
                  <span class="dot">·</span>
                  <span><el-icon><ChatDotRound /></el-icon>&nbsp;{{ article.commentCount || 0 }} {{ wx('评论') }}</span>
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

      <!-- 装饰圆角软分隔:代替原来会导致切割线的白色波浪 -->
      <div class="hero-divider" aria-hidden="true"></div>
    </header>

    <div class="container-narrow art-body">
      <!-- 左侧目录侧边栏：独立滚动容器 + sticky 固定 -->
      <aside class="art-toc" v-if="tocItems.length" aria-label="文章目录">
        <div class="toc-title">{{ wx('目录') }}</div>
        <ul class="toc-list">
          <li
            v-for="item in tocItems"
            :key="item.id"
            :class="['toc-item', 'lv-' + item.level, { active: activeId === item.id }]"
          >
            <a :href="'#' + item.id" @click.prevent="goAnchor(item.id)">{{ item.text }}</a>
          </li>
        </ul>
      </aside>

      <div class="art-main">
      <!-- 面包屑：首页 / 文章 / 标题 -->
      <nav class="art-breadcrumb" aria-label="面包屑导航">
        <router-link to="/" class="bc-item">首页</router-link>
        <span class="bc-sep">/</span>
        <router-link to="/articles" class="bc-item">{{ wx('文章') }}</router-link>
        <span class="bc-sep">/</span>
        <span class="bc-item bc-current" aria-current="page">{{ article.title }}</span>
      </nav>

      <!-- 正文（与后台 MdEditor 渲染一致） -->
      <article ref="contentRef" class="art-content md-preview-wrap">
        <MdPreview
          :sanitize="sanitizeMarkdown"
          :sanitize-mermaid="sanitizeDiagram"
          :model-value="article.content"
          v-bind="previewProps"
        />
      </article>

      <!-- 标签条（正文末尾） -->
      <div class="art-tags-foot reveal" v-if="article.tags?.length">
        <span class="tags-label">{{ wx('本文标签：') }}</span>
        <router-link v-for="t in article.tags" :key="t.id" :to="`/tags/${t.slug}`" class="tag-pill">
          # {{ t.name }}
        </router-link>
      </div>

      <!-- 分享条 -->
      <ShareBar :article="article" />

      <!-- 上下篇 -->
      <div class="art-footer-nav">
        <router-link v-if="prev" :to="`/articles/${prev.slug}`" class="nav-card reveal">
          <GradientBorderCard variant="sky" hoverable class="nav-inner">
            <span class="lbl">{{ wx('← 上一篇') }}</span>
            <span class="t">{{ prev.title }}</span>
          </GradientBorderCard>
        </router-link>
        <router-link v-if="next" :to="`/articles/${next.slug}`" class="nav-card reveal">
          <GradientBorderCard variant="sun" hoverable class="nav-inner nav-next">
            <span class="lbl">{{ wx('下一篇 →') }}</span>
            <span class="t">{{ next.title }}</span>
          </GradientBorderCard>
        </router-link>
      </div>

      <RelatedArticles :list="related" />

      <CommentSection :article-id="article.id" :allow-comment="article.allowComment === 1" />
      </div>
    </div>
  </div>
  <div v-else class="loading-page">
    <el-skeleton :rows="8" animated />
  </div>
</template>

<script setup>
import { useWuxiaCopy } from '@/composables/useWuxiaCopy'
const { wx } = useWuxiaCopy()

import { ref, computed, watch, onMounted, onUnmounted, nextTick } from 'vue'
import { useRoute } from 'vue-router'
import { Calendar, View, ChatDotRound, Folder } from '@element-plus/icons-vue'
import { articleBySlug } from '@/api/front'
import { MdPreview } from 'md-editor-v3'
import { sanitizeMarkdown, sanitizeDiagram } from '@/utils/markdownSecurity'
import 'md-editor-v3/lib/preview.css'
import { fmtDate } from '@/utils/format'
import { useAuthor } from '@/composables/useAuthor'
import { useWeather } from '@/composables/useWeather'
import GradientBorderCard from '@/components/GradientBorderCard.vue'
import RelatedArticles from '@/components/RelatedArticles.vue'
import CommentSection from '@/components/CommentSection.vue'
import ShareBar from '@/components/ShareBar.vue'

const previewProps = {
  theme: 'light',
  previewTheme: 'default',
  codeTheme: 'atom-one-light',
  showOutline: false
}

const route = useRoute()
const article = ref(null)
const prev = ref(null)
const next = ref(null)
const related = ref([])

/* ===== 文章目录（左侧独立侧边栏） ===== */
const tocItems = ref([])        // [{ id, text, level }]
const tocEls = []               // 缓存标题 DOM，用于滚动高亮
const activeId = ref('')
const contentRef = ref(null)

// 收集正文 h2/h3 构建目录；若库未生成 id 则兜底补一个
const buildToc = () => {
  const root = contentRef.value
  if (!root) return
  const nodes = Array.from(root.querySelectorAll('h2, h3'))
  const items = []
  const els = []
  nodes.forEach((n) => {
    if (!n.id) {
      n.id = 'h-' + (n.textContent || '').trim()
        .toLowerCase()
        .replace(/[^\w一-龥]+/g, '-')
        .replace(/^-+|-+$/g, '')
        .slice(0, 48) || 'h-auto'
    }
    items.push({ id: n.id, text: (n.textContent || '').trim(), level: Number(n.tagName[1]) })
    els.push(n)
  })
  tocItems.value = items
  tocEls.length = 0
  tocEls.push(...els)
  if (items.length) activeId.value = items[0].id
}

// 点击目录项：平滑滚动到对应标题（全局 scroll-padding-top 已为固定 header 留位）
const goAnchor = (id) => {
  const el = document.getElementById(id)
  if (el) el.scrollIntoView({ behavior: 'smooth', block: 'start' })
}

// 滚动时高亮当前可见章节
const setActive = () => {
  if (!tocEls.length) return
  const offset = 110
  let cur = tocEls[0].id
  for (const el of tocEls) {
    if (el.getBoundingClientRect().top - offset <= 0) cur = el.id
    else break
  }
  // 仅在章节切换时才写响应式，避免每帧无谓重渲染
  if (cur !== activeId.value) activeId.value = cur
}

const { authorName, authorAvatar, initial } = useAuthor(article)
const { temp, desc, location } = useWeather()

const readMinutes = computed(() => {
  const words = (article.value?.content || '').length
  if (!words) return null
  return Math.max(1, Math.round(words / 400))
})

// 阅读进度
const progressPercent = ref(0)
// rAF 合并滚动帧：避免每次 scroll 事件都触发 Vue 重渲染 + 同步读取布局（getBoundingClientRect）
let scrollTicking = false
const onScroll = () => {
  if (scrollTicking) return
  scrollTicking = true
  requestAnimationFrame(() => {
    const h = document.documentElement
    const total = h.scrollHeight - h.clientHeight
    progressPercent.value = total > 0 ? Math.min(100, (h.scrollTop / total) * 100) : 0
    setActive()
    scrollTicking = false
  })
}

const load = async () => {
  article.value = null
  tocItems.value = []
  try {
    const resp = await articleBySlug(route.params.slug)
    article.value = resp.data?.article
    prev.value = resp.data?.prev
    next.value = resp.data?.next
    related.value = resp.data?.related || []
  } catch (_) {}
  // 等 MdPreview 渲染出标题 DOM 再收集目录
  await nextTick()
  buildToc()
  // MdPreview 可能异步渲染，补一次兜底，确保标题都被收集
  setTimeout(buildToc, 80)
  setActive()
}

watch(() => route.params.slug, load)
onMounted(async () => {
  await load()
  window.addEventListener('scroll', onScroll, { passive: true })
  onScroll()
})
onUnmounted(() => { window.removeEventListener('scroll', onScroll) })
</script>

<style scoped lang="scss">
/* ===== 阅读进度条 ===== */
.art-progress {
  position: fixed;
  top: 0;
  left: 0;
  height: 3px;
  background: linear-gradient(90deg, var(--c-botany-500), var(--c-cyan-500), var(--c-autumn-500));
  z-index: 1000;
  transition: width 0.15s ease-out;
  border-radius: 0 2px 2px 0;
  box-shadow: 0 0 8px rgba(var(--theme-primary-rgb), 0.4);
}

/* ===== Hero 区 ===== */
.art-hero {
  position: relative;
  overflow: hidden;
  padding: 60px 0 80px;
  background: linear-gradient(180deg, var(--c-primary-mist) 0%, var(--c-botany-50) 60%, var(--c-white) 100%);
}
/* 有封面时:深蓝底色 + 底部留出柔和过渡到白色正文区,
   不再硬切,避免与下方 body 形成明显切割线。 */
.art-hero.has-cover {
  background: linear-gradient(180deg, var(--c-botany-950) 0%, var(--c-botany-950) 70%, var(--c-footer-deep) 88%, var(--c-botany-500) 96%, var(--c-white) 100%);
  color: var(--c-botany-50);
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
    linear-gradient(180deg, rgba(var(--theme-primary-deep-rgb), 0.45) 0%, rgba(var(--theme-primary-deep-rgb), 0.25) 50%, rgba(var(--theme-primary-deep-rgb), 0.55) 100%),
    radial-gradient(ellipse at center, transparent 0%, rgba(var(--theme-primary-deep-rgb), 0.4) 100%);
}
.has-cover .orb { opacity: 0.25; mix-blend-mode: screen; }
.has-cover .cloud path { fill: rgba(var(--theme-paper-rgb), 0.22); }
.orb {
  position: absolute;
  border-radius: 50%;
  filter: blur(80px);
  opacity: 0.55;
}
.orb-1 {
  width: 360px; height: 360px;
  background: radial-gradient(circle, var(--c-botany-500), transparent 70%);
  top: -60px; right: -40px;
  animation: orb-float 12s ease-in-out infinite;
}
.orb-2 {
  width: 280px; height: 280px;
  background: radial-gradient(circle, var(--c-autumn-500), transparent 70%);
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
  background: rgba(var(--theme-paper-rgb), 0.65);
  color: var(--c-botany-900);
  border: 1px solid rgba(var(--theme-primary-light-rgb), 0.6);
  border-radius: 999px;
  font-size: 12px;
  backdrop-filter: blur(6px);
  transition: all 0.2s ease;
}
.tag-pill:hover {
  background: var(--c-botany-500);
  color: var(--theme-on-primary);
  border-color: var(--c-botany-500);
}
.tag-pill-sun {
  color: var(--c-autumn-900);
  border-color: var(--c-autumn-200);
  background: rgba(var(--theme-accent-paper-rgb), 0.7);
}
.tag-pill-sun:hover {
  background: var(--c-autumn-500);
  color: var(--theme-on-primary);
  border-color: var(--c-autumn-500);
}

.art-title {
  font-family: var(--font-serif);
  font-size: 42px;
  line-height: 1.3;
  margin: 0 0 14px;
  color: var(--c-ink);
  background: linear-gradient(135deg, var(--c-botany-900) 0%, var(--c-botany-500) 60%, var(--c-cyan-500) 100%);
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
  background: linear-gradient(135deg, var(--c-white) 0%, var(--c-botany-200) 60%, var(--c-autumn-200) 100%);
  -webkit-background-clip: text;
          background-clip: text;
  -webkit-text-fill-color: transparent;
  text-shadow: 0 2px 18px rgba(var(--theme-primary-deep-rgb), 0.55);
}
.has-cover .art-deck { color: rgba(var(--theme-page-pale-rgb), 0.92); }
.has-cover .tag-pill {
  background: rgba(var(--theme-paper-rgb), 0.22);
  color: var(--c-botany-50);
  border-color: rgba(var(--theme-primary-soft-rgb), 0.55);
  backdrop-filter: blur(8px);
}
.has-cover .tag-pill:hover { background: var(--c-botany-500); color: var(--theme-on-primary); }
.has-cover .tag-pill-sun {
  color: var(--c-autumn-200);
  border-color: rgba(var(--theme-accent-light-rgb), 0.55);
  background: rgba(var(--theme-accent-paper-rgb), 0.18);
}
.has-cover .tag-pill-sun:hover { background: var(--c-autumn-500); color: var(--theme-on-primary); }
.has-cover .section-eyebrow { color: var(--c-autumn-200); }
.has-cover .weather-line { color: var(--c-botany-50); }
.has-cover .author-name { color: var(--c-botany-50); }
.has-cover .author-meta { color: rgba(var(--theme-page-pale-rgb), 0.78); }
.has-cover .author-meta .dot { color: rgba(var(--theme-page-pale-rgb), 0.5); }
.has-cover .avatar { box-shadow: 0 4px 12px rgba(var(--theme-primary-deep-rgb), 0.5); }

/* 穿透 GradientBorderCard:深色玻璃底 + 内部浅色文字 */
.has-cover :deep(.gradient-border-card) {
  background: rgba(var(--theme-ink-rgb), 0.55);
  backdrop-filter: blur(14px);
  -webkit-backdrop-filter: blur(14px);
  border: 1px solid rgba(var(--theme-primary-soft-rgb), 0.35);
}
.has-cover :deep(.gradient-border-card) .author-name { color: var(--c-botany-50); }
.has-cover :deep(.gradient-border-card) .author-meta { color: rgba(var(--theme-page-pale-rgb), 0.85); }
.has-cover :deep(.gradient-border-card) .author-meta .dot { color: rgba(var(--theme-page-pale-rgb), 0.55); }
.has-cover :deep(.gradient-border-card) .section-eyebrow { color: var(--c-autumn-200); }
.has-cover :deep(.gradient-border-card) .weather-line { color: var(--c-botany-50); }

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
  background: linear-gradient(135deg, var(--c-botany-500), var(--c-cyan-500));
  color: var(--theme-on-primary);
  font-family: var(--font-serif);
  font-size: 22px;
  display: grid; place-items: center;
  box-shadow: 0 4px 12px rgba(var(--theme-primary-rgb), 0.35);
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
.author-meta .dot { color: var(--c-ink-200); }

.author-right {
  text-align: right;
}
.section-eyebrow {
  display: block;
  font-size: 12px;
  letter-spacing: 0.2em;
  text-transform: uppercase;
  color: var(--c-cyan-700);
  font-weight: 600;
  margin-bottom: 4px;
}
.weather-line {
  font-family: var(--font-serif);
  font-size: 16px;
  color: var(--c-botany-900);
  font-weight: 600;
}

/* ===== Hero→Body 软分隔 =====
   之前的 .wave 是一个白色 SVG,会在深色 hero 底部留下一道硬切线。
   改用纯 CSS 渐变 + 微圆角,把 hero 与正文区柔和过渡。 */
.hero-divider {
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  height: 40px;
  pointer-events: none;
  background: linear-gradient(180deg,
    rgba(var(--theme-paper-rgb), 0)   0%,
    rgba(var(--theme-paper-rgb), 0.35) 60%,
    rgba(var(--theme-paper-rgb), 0.85) 100%);
  border-radius: 24px 24px 0 0;
}
.art-hero.has-cover .hero-divider {
  background: linear-gradient(180deg,
    rgba(var(--theme-paper-rgb), 0)   0%,
    rgba(var(--theme-primary-rgb), 0.25) 55%,
    rgba(var(--theme-page-pale-rgb), 0.95) 100%);
}
/* wave 类已全站移除 */

/* ===== 正文容器 ===== */
.art-body {
  /* .art-body 与 .container-narrow 复合在同一个 div 上,
     此前 .art-body 的 padding: 40px 0 80px 把 .container-narrow
     的水平 padding 0 24px 覆盖,导致内容贴到屏幕边缘。
     显式保留水平 padding,移动端缩到 16px。 */
  padding: 40px 24px 80px;
  position: relative;
  background: linear-gradient(180deg, var(--c-white) 0%, var(--c-botany-50) 60%, var(--c-primary-mist) 100%);
}
@media (max-width: 768px) {
  .art-body { padding: 32px 16px 60px; }
}
/* 桌面端(>1024px):正文区扩到 1100px 居中,让阅读体验更接近图一的"宽屏但留呼吸"效果。
   平板/手机仍走 .container-narrow 的 880px 或全屏,不影响移动端适配。 */
@media (min-width: 1025px) {
  .art-body {
    max-width: 1360px;
    display: flex;
    align-items: flex-start;
    gap: 36px;
  }
  .art-main { flex: 1; min-width: 0; }
}

/* ===== 左侧目录侧边栏 ===== */
.art-toc {
  position: sticky;
  top: 92px;                 /* 钉在固定 header 下方，不随文章滚动 */
  align-self: flex-start;    /* 不拉伸，sticky 才有生效空间 */
  flex-shrink: 0;
  width: 240px;
  /* 自身独立滚动容器：目录项过多时只滚这里，不影响右侧文章 */
  max-height: calc(100vh - 112px);
  overflow-y: auto;
  padding: 18px 14px 18px 16px;
  background: rgba(var(--theme-paper-rgb), 0.62);
  border: 1px solid rgba(var(--theme-primary-soft-rgb), 0.6);
  border-radius: 14px;
  backdrop-filter: blur(8px);
  -webkit-backdrop-filter: blur(8px);
  scrollbar-width: thin;
}
.toc-title {
  font-size: 13px;
  font-weight: 700;
  letter-spacing: 0.06em;
  color: var(--c-ink);
  margin-bottom: 10px;
  padding-left: 10px;
}
.toc-list { list-style: none; margin: 0; padding: 0; }
.toc-item a {
  display: block;
  padding: 6px 10px;
  font-size: 13px;
  line-height: 1.5;
  color: var(--c-ink-soft);
  text-decoration: none;
  border-left: 2px solid transparent;
  border-radius: 0 8px 8px 0;
  transition: color 0.18s ease, background 0.18s ease, border-color 0.18s ease;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.toc-item.lv-3 a { padding-left: 24px; font-size: 12.5px; }
.toc-item a:hover {
  color: var(--c-botany-500);
  background: var(--c-botany-50);
}
.toc-item.active a {
  color: var(--c-botany-900);
  background: rgba(var(--theme-primary-rgb), 0.12);
  border-left-color: var(--c-botany-500);
  font-weight: 600;
}
/* 锚点跳转时为固定 header 预留偏移，避免标题被遮挡 */
.art-content :deep(h2),
.art-content :deep(h3) {
  scroll-margin-top: 96px;
}
/* 平板 / 手机：隐藏目录，回归单栏阅读 */
@media (max-width: 1024px) {
  .art-toc { display: none; }
}

/* ===== 面包屑 ===== */
.art-breadcrumb {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 4px;
  margin-bottom: 24px;
  font-size: 13px;
}
.bc-item {
  color: var(--c-ink-soft);
  text-decoration: none;
  padding: 3px 6px;
  border-radius: 7px;
  transition: color 0.2s ease, background 0.2s ease;
  max-width: 280px;
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
}
.bc-item:hover {
  color: var(--c-botany-500);
  background: var(--c-botany-50);
}
.bc-current {
  color: var(--c-ink);
  font-weight: 600;
  cursor: default;
  max-width: 420px;
}
.bc-current:hover { background: transparent; }
.bc-sep {
  color: var(--c-ink-200);
  user-select: none;
}
@media (max-width: 480px) {
  .art-breadcrumb { font-size: 12px; margin-bottom: 18px; }
  .bc-item { max-width: 160px; }
  .bc-current { max-width: 200px; }
}

.cover {
  margin-bottom: 36px;
  border-radius: 18px;
  overflow: hidden;
  box-shadow: 0 12px 36px rgba(var(--theme-primary-strong-rgb), 0.15);
}
.cover img { width: 100%; display: block; }

.art-content {
  font-size: 16px;
  line-height: 1.95;
  color: var(--c-ink);
  padding: 16px 0;
}
/* 防止 Markdown 内嵌图片/表格/代码块撑出屏幕 */
.art-content :deep(img),
.art-content :deep(video),
.art-content :deep(iframe) {
  max-width: 100%;
  height: auto;
}
.art-content :deep(pre) {
  max-width: 100%;
  overflow-x: auto;
  -webkit-overflow-scrolling: touch;
}
.art-content :deep(table) {
  display: block;
  max-width: 100%;
  overflow-x: auto;
  -webkit-overflow-scrolling: touch;
}


/* 标签条 */
.art-tags-foot {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
  margin: 36px 0 0;
  padding: 16px 20px;
  background: rgba(var(--theme-paper-rgb), 0.6);
  border: 1px dashed var(--c-botany-200);
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
.lbl { font-size: 12px; letter-spacing: 0.1em; color: var(--c-cyan-700); font-weight: 600; }
.t {
  font-family: var(--font-serif);
  font-size: 15px;
  color: var(--c-ink);
  transition: color 0.2s ease;
  /* flex 子项默认 min-width:auto,会被长文本撑爆父容器;
     这里强制 min-width:0 允许收缩,再配合两行省略号。 */
  min-width: 0;
  max-width: 100%;
  display: -webkit-box;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
  line-clamp: 2;
  overflow: hidden;
  word-break: break-word;
  line-height: 1.45;
}
.nav-card:hover .t { color: var(--c-botany-900); }

/* Reveal */
.reveal {
  opacity: 0;
  transform: translateY(20px);
  transition: opacity 0.7s ease, transform 0.7s cubic-bezier(0.16, 1, 0.3, 1);
}
.reveal.visible { opacity: 1; transform: translateY(0); }

.loading-page { padding: 80px 24px; }
@media (max-width: 768px) { .loading-page { padding: 60px 16px; } }

@media (max-width: 768px) {
  .art-title { font-size: 28px; }
  .art-footer-nav { grid-template-columns: 1fr; }
  .nav-next { text-align: left; align-items: flex-start; }
  .author-right { text-align: left; }
}
@media (max-width: 480px) {
  .art-title { font-size: 22px; line-height: 1.3; }
  .art-content { font-size: 15px; }
  .art-author { flex-direction: column; text-align: center; padding: 24px 18px; }
  .author-avatar { width: 80px; height: 80px; }
  .nav-card { padding: 16px 18px; }
}
</style>
