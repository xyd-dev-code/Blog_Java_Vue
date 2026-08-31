<template>
  <div class="proj-page">
    <!-- ============ Hero + 引领句(同背景,卡片无缝衔接) ============ -->
    <section class="hero">
      <div class="hero-inner container">
        <div class="hero-left">
          <TextReveal text="我的项目" tag="h1" root-class="hero-title" />
          <p class="hero-subtitle">把想法变成看得见的作品</p>
          <nav class="hero-chips" v-if="categories.length">
            <button
              class="hero-chip"
              :class="{ active: activeCat === null }"
              @click="selectCat(null)"
            >全部</button>
            <button
              v-for="c in categories"
              :key="c.id"
              class="hero-chip"
              :class="{ active: activeCat === c.id }"
              @click="selectCat(c.id)"
            >{{ c.name }}</button>
          </nav>
          <p class="hero-tagline">最近完成与正在迭代的作品</p>
        </div>
        <div class="hero-right">
          <SunnyDecor variant="hero" :size="560" class="hero-decor" />
        </div>
      </div>
    </section>

    <!-- ============ 卡片网格(与 Hero 同背景) ============ -->
    <div class="proj-grid-section container">
      <!-- 飘浮云点缀（呼应设计图卡片网格的云朵） -->
      <svg class="float-cloud float-cloud--tr" viewBox="0 0 120 60" aria-hidden="true">
        <g fill="var(--c-white)">
          <ellipse cx="60" cy="40" rx="50" ry="14" opacity="0.85" />
          <ellipse cx="44" cy="32" rx="20" ry="16" opacity="0.85" />
          <ellipse cx="78" cy="28" rx="18" ry="14" opacity="0.85" />
        </g>
      </svg>
      <svg class="float-cloud float-cloud--br" viewBox="0 0 140 70" aria-hidden="true">
        <g fill="var(--c-white)">
          <ellipse cx="70" cy="46" rx="58" ry="16" opacity="0.85" />
          <ellipse cx="50" cy="38" rx="22" ry="18" opacity="0.85" />
          <ellipse cx="92" cy="32" rx="20" ry="16" opacity="0.85" />
        </g>
      </svg>
      <!-- 加载骨架 -->
      <div v-if="loading && projects.length === 0" class="proj-grid">
        <div class="proj-card skeleton" v-for="n in 4" :key="n">
          <div class="sk-illus"></div>
          <div class="sk-body">
            <div class="sk-line w50"></div>
            <div class="sk-line w80"></div>
            <div class="sk-line w60"></div>
          </div>
        </div>
      </div>

      <!-- 卡片网格 -->
      <div v-else class="proj-grid ef-stagger">
        <article
          v-for="p in projects"
          :key="p.id"
          class="proj-card ef-rise ef-card-glow"
          :style="cardVar(p)"
          v-tilt
          @click="openDetail(p)"
          @mousemove="onGlowMove"
        >
          <!-- 鼠标跟随径向光晕（真实 DOM，避免伪元素冲突） -->
          <div class="card-radial-glow" aria-hidden="true"></div>
          <!-- 大插画区 -->
          <div class="card-illus">
            <img
              v-if="p.coverUrl"
              :src="p.coverUrl"
              :alt="p.name"
              loading="lazy"
              decoding="async"
              class="illus-img"
            />
            <SunnyDecor v-else :variant="illusVariant(p)" :size="280" class="illus-decor" />
          </div>

          <!-- 标题区 -->
          <div class="card-head">
            <h3 class="card-title">
              <span class="title-dot"></span>
              {{ p.name }}
            </h3>
            <p class="card-desc" v-if="p.description">{{ p.description }}</p>
          </div>

          <!-- 标签 -->
          <div class="card-tags" v-if="p.stack && p.stack.length">
            <span class="tech-tag" v-for="t in p.stack" :key="t">{{ t }}</span>
          </div>

          <!-- 三按钮 -->
          <div class="card-actions" @click.stop>
            <a
              v-if="isLink(p.githubUrl)"
              :href="p.githubUrl"
              target="_blank"
              rel="noopener"
              class="btn btn-gh"
            >
              <svg class="gh-ic" viewBox="0 0 16 16" width="14" height="14" fill="currentColor" aria-hidden="true">
                <path d="M8 0C3.58 0 0 3.58 0 8c0 3.54 2.29 6.53 5.47 7.59.4.07.55-.17.55-.38 0-.19-.01-.82-.01-1.49-2.01.37-2.53-.49-2.69-.94-.09-.23-.48-.94-.82-1.13-.28-.15-.68-.52-.01-.53.63-.01 1.08.58 1.23.82.72 1.21 1.87.87 2.33.66.07-.52.28-.87.51-1.07-1.78-.2-3.64-.89-3.64-3.95 0-.87.31-1.59.82-2.15-.08-.2-.36-1.02.08-2.12 0 0 .67-.21 2.2.82.64-.18 1.32-.27 2-.27.68 0 1.36.09 2 .27 1.53-1.04 2.2-.82 2.2-.82.44 1.1.16 1.92.08 2.12.51.56.82 1.27.82 2.15 0 3.07-1.87 3.75-3.65 3.95.29.25.54.73.54 1.48 0 1.07-.01 1.93-.01 2.2 0 .21.15.46.55.38A8.013 8.013 0 0016 8c0-4.42-3.58-8-8-8z"></path>
              </svg>
              <span>GitHub</span>
            </a>
            <a
              v-if="isLink(p.demoUrl)"
              :href="p.demoUrl"
              target="_blank"
              rel="noopener"
              class="btn btn-demo"
            >
              <svg viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
                <path d="M2 12s3-7 10-7 10 7 10 7-3 7-10 7-10-7-10-7z"/>
                <circle cx="12" cy="12" r="3"/>
              </svg>
              <span>Demo</span>
            </a>
            <button class="btn btn-more" @click="openDetail(p)">
              <span>{{ wx('查看详情') }}</span>
            </button>
          </div>
        </article>
      </div>

      <!-- 加载更多 -->
      <div class="load-more" v-if="hasMore && !loading">
        <button class="btn-loadmore" @click="loadMore" :disabled="loadingMore">
          <span v-if="loadingMore" class="spinner"></span>
          {{ loadingMore ? wx('加载中…', '翻卷中…') : wx('加载更多项目', '翻阅更多兵谱') }}
        </button>
      </div>
      <p class="end-tip" v-else-if="projects.length && !hasMore">{{ wx('—— 云朵悠悠，已抵此页尽头 ——') }}</p>

      <!-- 空状态 -->
      <div class="empty-state" v-if="!loading && projects.length === 0">
        <div class="empty-art" aria-hidden="true">
          <div class="es-sun"></div>
          <div class="es-cloud es-cloud-1"></div>
          <div class="es-cloud es-cloud-2"></div>
          <div class="es-hill"></div>
        </div>
        <p class="empty-title">{{ wx('这片晴空下还没有项目') }}</p>
        <p class="empty-sub">
          {{ activeCat ? wx('该分类下暂时没有可展示的项目，换个分类看看？', '此门类尚未收录兵谱，不妨换个门类继续寻访。') : wx('敬请期待，新的作品正在向阳生长。', '敬请期待，新的兵谱正在炉火中淬炼。') }}
        </p>
      </div>
    </div>

    <!-- ============ 详情弹窗 ============ -->
    <el-dialog
      v-model="detailVisible"
      width="min(720px, 94vw)"
      class="proj-detail-dialog"
      :show-close="true"
      @closed="detailProject = null"
    >
      <div v-if="detailProject" class="proj-detail">
        <div class="pd-cover">
          <img v-if="detailProject.coverUrl" :src="detailProject.coverUrl" :alt="detailProject.name" loading="lazy" decoding="async" />
          <SunnyDecor v-else :variant="illusVariant(detailProject)" :size="320" />
        </div>
        <div class="pd-body">
          <div class="pd-badges" v-if="catName(detailProject)">
            <span class="pd-cat" :style="{ background: catColor(detailProject) }">
              {{ catName(detailProject) }}
            </span>
          </div>
          <h2 class="pd-title">
            <span class="title-dot"></span>
            {{ detailProject.name }}
          </h2>
          <p class="pd-desc">{{ detailProject.description || '暂无简介' }}</p>
          <div class="pd-section" v-if="detailProject.stack && detailProject.stack.length">
            <div class="pd-label">{{ wx('技术栈') }}</div>
            <div class="pd-tags">
              <span class="tech-tag" v-for="t in detailProject.stack" :key="t">{{ t }}</span>
            </div>
          </div>
          <div class="pd-actions">
            <a
              v-if="isLink(detailProject.githubUrl)"
              :href="detailProject.githubUrl"
              target="_blank"
              rel="noopener"
              class="btn btn-gh"
            >
              <svg class="gh-ic" viewBox="0 0 16 16" width="14" height="14" fill="currentColor" aria-hidden="true">
                <path d="M8 0C3.58 0 0 3.58 0 8c0 3.54 2.29 6.53 5.47 7.59.4.07.55-.17.55-.38 0-.19-.01-.82-.01-1.49-2.01.37-2.53-.49-2.69-.94-.09-.23-.48-.94-.82-1.13-.28-.15-.68-.52-.01-.53.63-.01 1.08.58 1.23.82.72 1.21 1.87.87 2.33.66.07-.52.28-.87.51-1.07-1.78-.2-3.64-.89-3.64-3.95 0-.87.31-1.59.82-2.15-.08-.2-.36-1.02.08-2.12 0 0 .67-.21 2.2.82.64-.18 1.32-.27 2-.27.68 0 1.36.09 2 .27 1.53-1.04 2.2-.82 2.2-.82.44 1.1.16 1.92.08 2.12.51.56.82 1.27.82 2.15 0 3.07-1.87 3.75-3.65 3.95.29.25.54.73.54 1.48 0 1.07-.01 1.93-.01 2.2 0 .21.15.46.55.38A8.013 8.013 0 0016 8c0-4.42-3.58-8-8-8z"></path>
              </svg>
              <span>GitHub 仓库</span>
            </a>
            <a
              v-if="isLink(detailProject.demoUrl)"
              :href="detailProject.demoUrl"
              target="_blank"
              rel="noopener"
              class="btn btn-demo"
            >
              <svg viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
                <path d="M2 12s3-7 10-7 10 7 10 7-3 7-10 7-10-7-10-7z"/>
                <circle cx="12" cy="12" r="3"/>
              </svg>
              <span>在线 Demo</span>
            </a>
          </div>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { useWuxiaCopy } from '@/composables/useWuxiaCopy'
const { wx } = useWuxiaCopy()

import { ref, computed, onMounted, nextTick } from 'vue'
import { projects as fetchProjects, projectCategories } from '@/api/front'
import SunnyDecor from '@/components/SunnyDecor.vue'
import TextReveal from '@/components/effects/TextReveal.vue'
import { useSiteStore } from '@/stores/site'
import { toThemeColor } from '@/utils/theme'

const siteStore = useSiteStore()
const githubUrl = computed(() => siteStore.info?.github || 'https://github.com')

const loading = ref(false)
const loadingMore = ref(false)
const categories = ref([])
const activeCat = ref(null)
const projects = ref([])
const page = ref(1)
const size = 12
const total = ref(0)
const hasMore = computed(() => projects.value.length < total.value && total.value > 0)

const detailVisible = ref(false)
const detailProject = ref(null)

const catMap = computed(() => {
  const m = {}
  categories.value.forEach((c) => { m[c.id] = c })
  return m
})

const isLink = (u) => !!u && u.trim() !== '' && u.trim() !== '#'
const catName = (p) => (p && p.categoryId && catMap.value[p.categoryId] ? catMap.value[p.categoryId].name : '')
const catColor = (p) => toThemeColor(p && p.categoryId && catMap.value[p.categoryId] ? catMap.value[p.categoryId].color : '')
const cardVar = (p) => ({ '--card-color': toThemeColor(p.color) })

// 按分类 slug 选占位插画；分类未命中时按 id 哈希回退到中性池
const illusVariant = (p) => {
  const slug = catMap.value[p.categoryId]?.slug || ''
  if (slug === 'open-source') return 'card-code'
  if (slug === 'tool') return 'card-toolbox'
  if (slug === 'experiment') return 'card-flask'
  // 6 种中性灰占位按 id 轮询,相邻项目视觉差异大
  const idNum = Number(p.id) || 0
  const pool = ['card-notebook', 'card-dashboard', 'card-code', 'card-toolbox', 'card-flask', 'card-document']
  return pool[idNum % pool.length]
}

async function loadCategories() {
  try {
    const resp = await projectCategories({}, { silent: true })
    const data = Array.isArray(resp?.data) ? resp.data : []
    categories.value = data.filter((c) => c.status === 1)
  } catch (_) { /* keep empty */ }
}

async function loadProjects(reset) {
  if (reset) {
    loading.value = true
  } else {
    loadingMore.value = true
  }
  try {
    const params = { page: page.value, size }
    if (activeCat.value != null) params.categoryId = activeCat.value
    const resp = await fetchProjects(params, { silent: true })
    const pageObj = resp?.data || {}
    const records = pageObj.records || []
    if (reset) {
      projects.value = records
    } else {
      const exist = new Set(projects.value.map((x) => x.id))
      projects.value = [...projects.value, ...records.filter((x) => !exist.has(x.id))]
    }
    total.value = pageObj.total || 0
    if (!reset && records.length === 0) total.value = projects.value.length
  } catch (_) {
    if (reset) {
      projects.value = []
      total.value = 0
    } else {
      page.value = Math.max(1, page.value - 1)
    }
  } finally {
    loading.value = false
    loadingMore.value = false
  }
}

function selectCat(id) {
  if (activeCat.value === id) return
  activeCat.value = id
  page.value = 1
  loadProjects(true)
  nextTick(() => window.scrollTo({ top: 0, behavior: 'smooth' }))
}

function loadMore() {
  if (!hasMore.value || loadingMore.value) return
  page.value += 1
  loadProjects(false)
}

function openDetail(p) {
  detailProject.value = p
  detailVisible.value = true
}

// 卡片鼠标跟随高光：把光标位置写成 --mx/--my，供 glow-card 的 ::before/::after 读取
function onGlowMove(e) {
  const el = e.currentTarget
  const rect = el.getBoundingClientRect()
  el.style.setProperty('--mx', `${((e.clientX - rect.left) / rect.width) * 100}%`)
  el.style.setProperty('--my', `${((e.clientY - rect.top) / rect.height) * 100}%`)
}

onMounted(async () => {
  await loadCategories()
  await loadProjects(true)
})
</script>

<style scoped lang="scss">
.proj-page {
  position: relative;
  min-height: 60vh;
  /* 只保留两处极淡透明光斑；天空底色由全局渐变 + 视差云层提供，
     不铺不透明底色，与 hero/内容区共融成一片天幕 */
  background:
    radial-gradient(circle at 88% 18%, rgba(var(--theme-primary-soft-rgb), 0.15) 0%, transparent 42%),
    radial-gradient(circle at 10% 92%, rgba(var(--theme-primary-soft-rgb), 0.18) 0%, transparent 48%);
}

/* ============ 通用容器 ============ */
.container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 24px;
  position: relative;
}

/* ============ Hero 横幅 ============ */
.hero {
  position: relative;
  /* 不裁切——hero-bg 光斑与装饰插画会溢出，overflow:hidden 会在边界形成硬截断线 */
  padding: 20px 0 10px;   /* 回涨,不再压缩 */
}
.hero-bg {
  position: absolute;
  inset: 0;
  /* 不铺不透明渐变底色——让全局天空透上来实现一体感；
     只保留极淡的 alpha 光斑 */
  background:
    radial-gradient(circle at 88% 18%, rgba(var(--theme-primary-soft-rgb), 0.20) 0%, transparent 42%),
    radial-gradient(circle at 10% 92%, rgba(var(--theme-primary-soft-rgb), 0.28) 0%, transparent 48%);
  z-index: 0;
}
.hero-inner {
  position: relative;
  z-index: 1;
  display: flex;
  align-items: center;
  gap: 32px;
  /* 不设 min-height,让 Hero 自适应内容,极致紧凑 */
}
.hero-left {
  flex: 1 1 50%;
  min-width: 0;
}
.hero-title {
  /* 与全站 SkyHero 基线对齐:衬线 + ~48px + weight 700;
     全局 h1-h4 已用 var(--font-serif),不再覆盖字体栈 */
  font-size: clamp(36px, 4.5vw, 48px);
  font-weight: 700;
  margin: 0 0 8px;
  color: var(--c-ink-900);
  line-height: 1.15;
}
.hero-subtitle {
  font-size: 15px;
  color: var(--c-ink-500);
  margin: 0 0 10px;
}
.hero-chips {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 6px;
}
.hero-tagline {
  font-size: 12px;
  color: var(--c-ink-300);
  margin: 0;
  letter-spacing: 0.05em;
}
.hero-chip {
  font-family: var(--font-sans);
  font-size: 14px;
  padding: 6px 16px;
  border-radius: 999px;
  border: 1px solid rgba(var(--theme-primary-rgb), 0.45);
  background: rgba(var(--theme-paper-rgb), 0.7);
  backdrop-filter: blur(8px);
  color: var(--c-botany-950);
  cursor: pointer;
  transition: all 0.25s ease;
}
.hero-chip:hover {
  border-color: var(--c-botany-500);
  background: rgba(var(--theme-primary-soft-rgb), 0.5);
  transform: translateY(-1px);
}
.hero-chip.active {
  background: linear-gradient(135deg, var(--c-botany-500), var(--c-botany-700));
  color: var(--theme-on-primary);
  border-color: transparent;
  box-shadow: 0 6px 18px rgba(var(--theme-primary-rgb), 0.35);
}

.hero-right {
  flex: 1 1 50%;
  display: flex;
  justify-content: center;
  align-items: center;
  min-width: 0;
}

/* Hero 底部 CTA：查看源码与演示 */
.hero-cta {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 12px;
  margin-top: 34px;
}
.cta-label {
  font-family: var(--font-sans);
  font-size: 15px;
  font-weight: 600;
  color: var(--c-ink-700);
  margin-right: 2px;
  letter-spacing: 0.04em;
}
.cta-btn {
  display: inline-flex;
  align-items: center;
  gap: 7px;
  font-family: var(--font-sans);
  font-size: 14px;
  font-weight: 500;
  padding: 10px 22px;
  border-radius: 999px;
  text-decoration: none;
  transition: all 0.25s ease;
  white-space: nowrap;
}
.cta-btn--gh {
  background: var(--c-neutral-700);
  color: var(--theme-on-primary);
}
.cta-btn--gh:hover {
  background: var(--c-neutral-800);
  transform: translateY(-1px);
  box-shadow: 0 6px 16px rgba(var(--theme-ink-deep-rgb), 0.3);
}
.cta-btn--demo {
  background: linear-gradient(135deg, var(--c-botany-300), var(--c-botany-500));
  color: var(--theme-on-primary);
}
.cta-btn--demo:hover {
  background: linear-gradient(135deg, var(--c-botany-500), var(--c-botany-700));
  transform: translateY(-1px);
  box-shadow: 0 6px 16px rgba(var(--theme-primary-rgb), 0.4);
}
.cta-btn .gh-ic { flex-shrink: 0; }
.hero-decor {
  width: 100%;
  max-width: 400px;   /* 回涨到此,不再缩——保持 */
  height: auto;
  filter: drop-shadow(0 12px 30px rgba(var(--theme-primary-rgb), 0.18));
  animation: hero-float 6s ease-in-out infinite;
}
@keyframes hero-float {
  0%, 100% { transform: translateY(0); }
  50% { transform: translateY(-8px); }
}

@media (max-width: 880px) {
  .hero-inner { flex-direction: column; text-align: center; }
  .hero-chips { justify-content: center; }
  .hero-cta { justify-content: center; }
  .hero-decor { max-width: 380px; }
}

/* ============ 「精选项目」区 ============ */
.featured {
  /* 已被扁平化,无圆角无独立背景,与 Hero 同背景 */
  padding: 0;
}
/* 圆角包裹块保留(若有 legacy 引用) */
.featured-header {
  background: rgba(var(--theme-paper-rgb), 0.92);
  backdrop-filter: blur(10px);
  border: 1px solid rgba(var(--theme-primary-soft-rgb), 0.5);
  border-radius: 22px;
  padding: 22px 26px 24px;
  box-shadow: 0 8px 28px rgba(var(--theme-primary-rgb), 0.08);
  margin-bottom: 28px;
  display: flex;
  flex-direction: column;
  gap: 16px;
}
/* ============ 卡片网格容器(与 Hero 同背景) ============ */
.proj-grid-section {
  padding: 0 24px 40px;
  position: relative;
}
/* 飘浮云点缀 */
.float-cloud {
  position: absolute;
  pointer-events: none;
  filter: drop-shadow(0 4px 10px rgba(var(--theme-primary-rgb), 0.15));
  animation: cloud-drift 18s ease-in-out infinite alternate;
}
.float-cloud--tr { top: -16px; right: 0; width: 120px; opacity: 0.7; }
.float-cloud--br { top: 60px; right: -10px; width: 140px; opacity: 0.85; }
@keyframes cloud-drift {
  0% { transform: translateX(0); }
  100% { transform: translateX(-14px); }
}
.feat-head {
  position: relative;
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 24px;
  padding-right: 140px;
}
.feat-head-text { text-align: left; }
.feat-title {
  font-size: 32px;
  font-weight: 700;
  margin: 0 0 8px;
  color: var(--c-ink-900);
}
.feat-subtitle {
  font-size: 14px;
  color: var(--c-ink-400);
  margin: 0;
}
.feat-decor {
  position: absolute;
  top: -10px;
  right: 0;
  filter: drop-shadow(0 6px 16px rgba(var(--theme-accent-rgb), 0.25));
  animation: hero-float 7s ease-in-out infinite;
}
@media (max-width: 680px) {
  .feat-head { padding-right: 0; flex-direction: column; }
  .feat-decor { position: relative; top: 0; align-self: flex-end; max-width: 80px; }
}

/* ============ 卡片网格 ============ */
.proj-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 26px;
}
@media (max-width: 960px) {
  .proj-grid { grid-template-columns: repeat(2, 1fr); }
}
@media (max-width: 560px) {
  .proj-grid { grid-template-columns: 1fr; gap: 18px; }
  .card-actions { flex-wrap: wrap; gap: 4px; padding: 6px 12px 10px; }
  .btn { font-size: 12px; padding: 5px 10px; }
}
@media (max-width: 480px) {
  .btn-more { margin-left: 0; }
}

/* ============ 卡片 ============ */
.proj-card {
  position: relative;
  display: flex;
  flex-direction: column;
  height: 100%;        /* grid 同行等高 */
  background: rgba(var(--theme-paper-rgb), 0.92);
  backdrop-filter: blur(10px);
  border: 1px solid rgba(var(--theme-primary-rgb), 0.18);
  border-radius: 14px;  /* 与设计图一致 */
  overflow: hidden;
  box-shadow: 0 4px 16px rgba(var(--theme-primary-strong-rgb), 0.06);
  cursor: pointer;
  transition: transform 0.35s cubic-bezier(0.25, 0.8, 0.25, 1),
    box-shadow 0.35s ease, border-color 0.35s ease;
}
.proj-card:hover {
  transform: translateY(-6px);
  border-color: rgba(var(--theme-primary-rgb), 0.5);
  box-shadow: 0 14px 36px rgba(var(--theme-primary-strong-rgb), 0.18), 0 20px 50px rgba(var(--theme-accent-rgb), 0.1);
}
/* 卡片 overflow:hidden 会裁掉外扩 1px 的流光描边，故收回到边框内侧 */
.proj-card.ef-card-glow::before {
  inset: 0;
}

/* 鼠标跟随径向光晕（真实 DOM，不占用 ::before/::after） */
.card-radial-glow {
  position: absolute;
  inset: 0;
  top: var(--my, 50%);
  left: var(--mx, 50%);
  width: 340px;
  height: 340px;
  transform: translate(-50%, -50%);
  background: radial-gradient(
    circle,
    rgba(var(--theme-accent-rgb), 0.13) 0%,
    rgba(var(--theme-primary-rgb), 0.09) 28%,
    transparent 60%
  );
  pointer-events: none;
  opacity: 0;
  transition: opacity 0.35s ease;
  z-index: 0;
}
.proj-card:hover .card-radial-glow { opacity: 1; }
.proj-card > :not(.card-radial-glow) {
  position: relative;
  z-index: 1;
}

/* 大插画区 */
.card-illus {
  position: relative;
  aspect-ratio: 2 / 1;    /* 回涨到此,不再缩 */
  overflow: hidden;
  background: linear-gradient(135deg, var(--c-botany-50) 0%, var(--c-botany-100) 100%);
  display: grid;
  place-items: center;
}
.illus-img {
  width: 100%; height: 100%;
  object-fit: cover;
  display: block;
  transition: transform 0.6s cubic-bezier(0.25, 0.8, 0.25, 1);
}
.proj-card:hover .illus-img { transform: scale(1.06); }
.illus-decor {
  width: 100%;
  height: 100%;
  max-width: 280px;
}

/* 标题区 */
.card-head {
  padding: 10px 14px 2px;
}
.card-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 15px;
  font-weight: 700;
  margin: 0 0 2px;
  color: var(--c-ink-900);
}
.title-dot {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: var(--c-autumn-500);
  flex-shrink: 0;
  box-shadow: 0 0 5px rgba(var(--theme-accent-rgb), 0.5);
}
.card-desc {
  font-family: var(--font-sans);
  font-size: 12px;
  color: var(--c-ink-400);
  margin: 0;
  line-height: 1.4;
  display: -webkit-box;
  -webkit-line-clamp: 1;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

/* 标签 */
.card-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 5px;
  padding: 2px 14px 6px;
}
.tech-tag {
  font-family: var(--font-sans);
  display: inline-block;
  font-size: 12px;
  padding: 2px 8px;
  border-radius: 999px;
  background: var(--c-ink-50);
  color: var(--c-ink-500);
  border: 1px solid var(--c-line);
}

/* 三按钮 */
.card-actions {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 6px 14px 10px;
  border-top: 1px solid var(--c-ink-100);
  margin-top: auto;
}
.btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  font-family: var(--font-sans);
  font-size: 12.5px;
  font-weight: 500;
  border-radius: 999px;
  border: none;
  cursor: pointer;
  transition: all 0.25s ease;
  text-decoration: none;
  white-space: nowrap;
}
.btn-gh {
  background: var(--c-neutral-700);
  color: var(--theme-on-primary);
  padding: 7px 14px;
}
.btn-gh:hover {
  background: var(--c-neutral-800);
  transform: translateY(-1px);
  box-shadow: 0 6px 16px rgba(var(--theme-ink-deep-rgb), 0.3);
}
.btn-demo {
  background: linear-gradient(135deg, var(--c-botany-300), var(--c-botany-500));
  color: var(--theme-on-primary);
  padding: 7px 14px;
}
.btn-demo:hover {
  background: linear-gradient(135deg, var(--c-botany-500), var(--c-botany-700));
  transform: translateY(-1px);
  box-shadow: 0 6px 16px rgba(var(--theme-primary-rgb), 0.4);
}
.btn-more {
  /* 浅边框+白底,和深灰 GitHub / 浅蓝 Demo 形成"深→中→浅"三级层次 */
  margin-left: auto;
  background: var(--c-paper);
  color: var(--c-ink-500);
  border: 1px solid var(--c-ink-200);
  padding: 6px 13px;
}
.btn-more:hover {
  background: var(--c-ink-100);
  color: var(--c-botany-950);
  border-color: var(--c-ink-300);
  transform: translateY(-1px);
}

/* ============ 加载更多 ============ */
.load-more {
  display: flex;
  justify-content: center;
  padding: 28px 0 48px;
}
.btn-loadmore {
  font-family: var(--font-sans);
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 11px 30px;
  border-radius: 999px;
  border: 1px dashed rgba(var(--theme-primary-rgb), 0.5);
  background: rgba(var(--theme-paper-rgb), 0.6);
  color: var(--c-botany-950);
  font-size: 14px;
  cursor: pointer;
  transition: all 0.25s ease;
}
.btn-loadmore:hover {
  background: rgba(var(--theme-primary-rgb), 0.1);
  border-style: solid;
}
.btn-loadmore:disabled { opacity: 0.7; cursor: default; }
.spinner {
  width: 14px; height: 14px;
  border: 2px solid rgba(var(--theme-primary-rgb), 0.3);
  border-top-color: var(--c-botany-500);
  border-radius: 50%;
  animation: spin 0.7s linear infinite;
}
@keyframes spin { to { transform: rotate(360deg); } }
.end-tip {
  text-align: center;
  color: var(--c-ink-300);
  font-size: 13px;
  padding: 12px 0 48px;
  letter-spacing: 0.05em;
}

/* ============ 骨架屏 ============ */
.skeleton { pointer-events: none; }
.sk-illus {
  aspect-ratio: 3 / 2;
  background: linear-gradient(90deg, var(--c-skeleton-base) 25%, var(--c-skeleton-shine) 50%, var(--c-skeleton-base) 75%);
  background-size: 200% 100%;
  animation: shimmer 1.4s infinite;
}
.sk-body { padding: 16px 18px; }
.sk-line {
  height: 12px;
  border-radius: 6px;
  margin-bottom: 10px;
  background: linear-gradient(90deg, var(--c-skeleton-base) 25%, var(--c-skeleton-shine) 50%, var(--c-skeleton-base) 75%);
  background-size: 200% 100%;
  animation: shimmer 1.4s infinite;
}
.w50 { width: 50%; } .w80 { width: 80%; } .w60 { width: 60%; }
@keyframes shimmer {
  0% { background-position: 200% 0; }
  100% { background-position: -200% 0; }
}

/* ============ 空状态 ============ */
.empty-state {
  text-align: center;
  padding: 40px 0 60px;
}
.empty-art {
  position: relative;
  width: 220px;
  height: 130px;
  margin: 0 auto 18px;
}
.es-sun {
  position: absolute; top: 6px; left: 50%; transform: translateX(-50%);
  width: 50px; height: 50px; border-radius: 50%;
  background: radial-gradient(circle, var(--c-autumn-200) 0%, var(--c-autumn-500) 70%);
  box-shadow: 0 0 30px rgba(var(--theme-accent-rgb), 0.5);
  animation: float-sun 3.5s ease-in-out infinite;
}
@keyframes float-sun {
  0%,100% { transform: translateX(-50%) translateY(0); }
  50% { transform: translateX(-50%) translateY(-6px); }
}
.es-cloud {
  position: absolute;
  background: var(--c-paper);
  border-radius: 999px;
  box-shadow: 0 6px 18px rgba(var(--theme-primary-rgb), 0.15);
}
.es-cloud::before, .es-cloud::after {
  content: ''; position: absolute; background: var(--c-paper); border-radius: 50%;
}
.es-cloud-1 { width: 80px; height: 22px; top: 40px; left: 20px; animation: drift 6s ease-in-out infinite alternate; }
.es-cloud-1::before { width: 32px; height: 32px; top: -14px; left: 12px; }
.es-cloud-1::after { width: 24px; height: 24px; top: -10px; left: 40px; }
.es-cloud-2 { width: 60px; height: 18px; top: 64px; right: 20px; animation: drift 7s ease-in-out infinite alternate-reverse; }
.es-cloud-2::before { width: 26px; height: 26px; top: -12px; left: 10px; }
.es-cloud-2::after { width: 20px; height: 20px; top: -8px; left: 32px; }
@keyframes drift { 0% { transform: translateX(-8px); } 100% { transform: translateX(8px); } }
.es-hill {
  position: absolute; bottom: 0; left: 0; right: 0; height: 40px;
  background: linear-gradient(180deg, var(--c-botany-200) 0%, var(--c-botany-300) 100%);
  border-radius: 50% 50% 0 0 / 100% 100% 0 0;
  opacity: 0.5;
}
.empty-title {
  font-size: 18px;
  font-weight: 700;
  color: var(--c-ink-900);
  margin: 0 0 6px;
}
.empty-sub {
  font-family: var(--font-sans);
  font-size: 14px;
  color: var(--c-ink-400);
  margin: 0;
}

/* ============ 详情弹窗 ============ */
.proj-detail-dialog :deep(.el-dialog) {
  /* 强制居中,内容超视口时也保持垂直水平居中 */
  position: fixed;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  margin: 0;
  border-radius: 18px;
  overflow: hidden;
  max-height: 88vh;
  display: flex;
  flex-direction: column;
}
.proj-detail-dialog :deep(.el-dialog__header) { display: none; }
.proj-detail-dialog :deep(.el-dialog__body) {
  padding: 0;
  flex: 1;
  min-height: 0;             /* 允许 flex 收缩 */
  overflow-y: auto;          /* 内容过长时弹窗内部滚动 */
}
.proj-detail { display: flex; flex-direction: column; }
.pd-cover {
  width: 100%;
  aspect-ratio: 16 / 7;
  background: linear-gradient(135deg, var(--c-botany-50) 0%, var(--c-botany-100) 100%);
  overflow: hidden;
  display: grid;
  place-items: center;
}
.pd-cover img { width: 100%; height: 100%; object-fit: cover; display: block; }
.pd-body { padding: 22px 28px 28px; }
.pd-badges { display: flex; gap: 8px; margin-bottom: 12px; }
.pd-cat { font-size: 12px; padding: 3px 12px; border-radius: 999px; color: var(--theme-on-primary); }
.pd-title {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 24px;
  font-weight: 700;
  margin: 0 0 12px;
  color: var(--c-ink-900);
}
.pd-desc {
  font-family: var(--font-sans);
  font-size: 15px;
  line-height: 1.8;
  color: var(--c-ink-500);
  margin: 0 0 16px;
}
.pd-section { margin-bottom: 16px; }
.pd-label {
  font-family: var(--font-sans);
  font-size: 13px;
  color: var(--c-ink-300);
  margin-bottom: 8px;
  letter-spacing: 0.05em;
}
.pd-tags { display: flex; flex-wrap: wrap; gap: 6px; }
.pd-actions { display: flex; gap: 12px; padding-top: 8px; }
.pd-actions .btn { padding: 9px 18px; font-size: 13.5px; }
</style>
