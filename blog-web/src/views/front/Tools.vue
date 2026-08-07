<template>
  <div class="tools-page">
    <!-- ============ Hero 区（天空蓝渐变 + 云朵装饰） ============ -->
    <section class="hero">
      <div class="hero-bg" aria-hidden="true">
        <!-- 太阳 -->
        <svg class="hero-sun" viewBox="0 0 64 64" width="56" height="56">
          <circle cx="32" cy="32" r="14" fill="#fbbf24" />
          <g stroke="#fbbf24" stroke-width="3" stroke-linecap="round">
            <line x1="32" y1="2" x2="32" y2="10" />
            <line x1="32" y1="54" x2="32" y2="62" />
            <line x1="2" y1="32" x2="10" y2="32" />
            <line x1="54" y1="32" x2="62" y2="32" />
            <line x1="11" y1="11" x2="16" y2="16" />
            <line x1="48" y1="48" x2="53" y2="53" />
            <line x1="53" y1="11" x2="48" y2="16" />
            <line x1="16" y1="48" x2="11" y2="53" />
          </g>
        </svg>
        <!-- 漂浮云朵（CSS 绘制，更蓬松） -->
        <div class="cloud cloud-1" aria-hidden="true"></div>
        <div class="cloud cloud-2" aria-hidden="true"></div>
        <div class="cloud cloud-3" aria-hidden="true"></div>
        <div class="cloud cloud-4" aria-hidden="true"></div>
      </div>

      <div class="hero-inner container">
        <TextReveal text="在线工具箱" tag="h1" root-class="hero-title" />
        <p class="hero-subtitle">把重复操作变得更简单</p>
        <div class="hero-search">
          <input
            v-model="kw"
            class="hero-search-input"
            placeholder="搜索你需要的工具"
            @keyup.enter="kw = kw"
          />
          <svg class="hero-search-icon" width="18" height="18" viewBox="0 0 24 24"
            fill="none" stroke="#38bdf8" stroke-width="2" aria-hidden="true">
            <circle cx="11" cy="11" r="7" />
            <line x1="16.5" y1="16.5" x2="21" y2="21" />
          </svg>
        </div>
        <p class="hero-hint">
          <el-icon><Clock /></el-icon>
          时间工作需要的工具
          <el-tooltip content="免登录即用 · 轻量高效 · 持续更新中" placement="top">
            <el-icon class="hint-info"><InfoFilled /></el-icon>
          </el-tooltip>
        </p>
      </div>
    </section>

    <!-- ============ 公告横幅（如有） ============ -->
    <transition name="banner-fade">
      <div v-if="announcement" class="banner container">
        <el-icon><WarningFilled /></el-icon>
        <span>{{ announcement }}</span>
      </div>
    </transition>

    <!-- ============ 工具卡片网格 ============ -->
    <section class="grid-section container">
      <!-- 分类 Tab 紧贴卡片区左上角 -->
      <div class="tabs-row">
        <button
          v-for="c in categories"
          :key="c.value"
          class="tabs-chip"
          :class="{ active: activeCat === c.value }"
          @click="activeCat = c.value"
        >{{ c.label }}</button>
      </div>
      <div v-if="loading" class="grid">
        <div v-for="n in 8" :key="n" class="tool-card skeleton">
          <div class="sk-line w40"></div>
          <div class="sk-icon"></div>
          <div class="sk-line w70"></div>
          <div class="sk-line w50"></div>
          <div class="sk-line w60"></div>
        </div>
      </div>

      <div v-else-if="!filteredTools.length" class="empty">
        <el-empty description="没有匹配的工具" :image-size="80" />
        <el-button type="primary" plain @click="kw = ''; activeCat = 'all'">清除筛选</el-button>
      </div>

      <div v-else class="grid ef-stagger">
        <article
          v-for="t in filteredTools"
          :key="t.id"
          class="tool-card ef-rise ef-card-glow"
          :class="cardClass(t)"
          v-tilt
          @mousemove="onGlowMove"
        >
          <!-- 鼠标跟随径向光晕（真实 DOM，避免与 ef-card-glow::before 伪元素冲突） -->
          <div class="card-radial-glow" aria-hidden="true"></div>
          <!-- 左上：小分类标签 -->
          <span class="cat-tag">
            {{ catLabel(t.category) }}
          </span>
          <!-- 右上：圆形 + 按钮（收藏） -->
          <button
            class="add-btn"
            :class="{ active: isFav(t.id) }"
            :title="isFav(t.id) ? '取消收藏' : '收藏'"
            @click.stop="toggleFav(t)"
          >
            <el-icon>
              <component :is="isFav(t.id) ? 'StarFilled' : 'Plus'" />
            </el-icon>
          </button>

          <!-- 标题 + 描述 -->
          <h3 class="card-title">{{ t.name }}</h3>
          <p class="card-desc">{{ t.description }}</p>

          <!-- 右上图标：图片 URL 优先渲染，否则蓝线稿文档图标兜底 -->
          <img v-if="isIconUrl(t.icon)" :src="t.icon" class="card-icon card-icon-img" alt="" aria-hidden="true" />
          <svg v-else class="card-icon" width="60" height="60" viewBox="0 0 60 60"
            fill="none" stroke="#38bdf8" stroke-width="2" stroke-linejoin="round"
            aria-hidden="true">
            <rect x="14" y="8" width="32" height="44" rx="3" />
            <line x1="20" y1="18" x2="40" y2="18" />
            <line x1="20" y1="26" x2="40" y2="26" />
            <line x1="20" y1="34" x2="34" y2="34" />
          </svg>

          <!-- 底部按钮区 -->
          <div class="card-foot">
            <span class="cat-pill" :class="{ muted: t.status === 2 }">
              {{ catLabel(t.category) }}
            </span>
            <template v-if="t.status === 1">
              <span class="use-btn ef-neon" @click.stop="onUse(t)">开始使用</span>
            </template>
            <template v-else-if="t.status === 2">
              <span class="use-btn muted" @click.stop="onMaintain">维护中</span>
            </template>
            <template v-else-if="t.status === 3">
              <span class="use-btn upcoming" @click.stop="onUpcoming">即将上线</span>
            </template>
            <template v-else>
              <span class="use-btn muted">已下线</span>
            </template>
          </div>
        </article>
      </div>
    </section>

    <!-- ============ 底部标语 ============ -->
    <footer class="footer container">
      <span>免登录即用</span>
      <span class="dot"></span>
      <span>轻量高效</span>
      <span class="dot"></span>
      <span>持续更新中</span>
    </footer>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { Clock, InfoFilled, StarFilled, Plus, WarningFilled } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { useRouter } from 'vue-router'
import { publicTools, publicClickTool, publicHotTools, publicToolCategories } from '@/api/front'
import TextReveal from '@/components/effects/TextReveal.vue'

const router = useRouter()
const tools = ref([])
const loading = ref(false)
const kw = ref('')
const activeCat = ref('all')
const favIds = ref([]) // localStorage 收藏

// 兜底仅保留"全部"，分类 tab 完全由后端驱动（含状态过滤）
const categories = ref([{ value: 'all', label: '全部' }])

const catLabel = (v) => (categories.value.find((c) => c.value === v) || {}).label || '其他'

// 图标：URL（http/https// 或 data:）才渲染成 <img>，其余回退蓝线稿文档图标
const isIconUrl = (v) => /^(https?:\/\/|\/|data:)/.test(v || '')

const loadCategories = async () => {
  try {
    const resp = await publicToolCategories()
    const arr = resp.data || []
    // 后端 PublicToolCategoryController 已过滤 status!=1 的分类
    // 始终用接口结果覆盖，确保下线分类不在前台出现
    categories.value = [{ value: 'all', label: '全部' }, ...arr.map(c => ({ value: c.code, label: c.name }))]
  } catch (_) {
    // 接口异常时仅保留"全部"tab，不展示硬编码分类
    categories.value = [{ value: 'all', label: '全部' }]
  }
}

const filteredTools = computed(() => {
  const q = kw.value.trim().toLowerCase()
  return tools.value.filter((t) => {
    if (activeCat.value !== 'all' && t.category !== activeCat.value) return false
    if (q && !(`${t.name}${t.slug}${t.description}`.toLowerCase().includes(q))) return false
    return true
  })
})

const announcement = computed(() => {
  const m = tools.value.find((t) => t.status === 2 && t.announcement)
  return m ? m.announcement : ''
})

const cardClass = (t) => ({
  'is-maintain': t.status === 2,
  'is-upcoming': t.status === 3,
  'is-offline': t.status === 0
})

// 卡片鼠标跟随高光：把光标位置写成 --mx/--my，供 glow-card 的 ::before/::after 读取
const onGlowMove = (e) => {
  const el = e.currentTarget
  const rect = el.getBoundingClientRect()
  el.style.setProperty('--mx', `${((e.clientX - rect.left) / rect.width) * 100}%`)
  el.style.setProperty('--my', `${((e.clientY - rect.top) / rect.height) * 100}%`)
}

const isFav = (id) => favIds.value.includes(id)
const toggleFav = (t) => {
  const i = favIds.value.indexOf(t.id)
  if (i >= 0) favIds.value.splice(i, 1)
  else favIds.value.unshift(t.id)
  try { localStorage.setItem('tool_favs', JSON.stringify(favIds.value)) } catch (_) {}
}

const onUse = async (t) => {
  // 1) 后端累计点击
  try { await publicClickTool(t.id) } catch (_) {}
  // 2) type=0 同页内嵌 → /tools/<slug>;type=1 外链 → 新标签打开 url
  if (t.type === 1 && /^https?:\/\//i.test(t.url)) {
    window.open(t.url, '_blank', 'noopener,noreferrer')
    return
  }
  if (t.url && t.url.startsWith('/')) {
    router.push(t.url)
    return
  }
  ElMessage.warning('该工具尚未配置地址,请联系站长')
}

const onMaintain = () => ElMessage.warning('该工具正在维护中,请稍后再来')
const onUpcoming = () => ElMessage.info('该工具即将上线,敬请期待')

const load = async () => {
  loading.value = true
  try {
    const resp = await publicTools()
    tools.value = Array.isArray(resp?.data) ? resp.data : []
  } catch (e) {
    console.error('[Tools] 加载工具列表失败:', e)
    tools.value = []
  } finally {
    loading.value = false
  }
}

// 收藏初始化
try {
  const raw = localStorage.getItem('tool_favs')
  if (raw) favIds.value = JSON.parse(raw) || []
} catch (_) { /* ignore */ }

// 收藏初始化

// 暴露给 AppHeader 共享同一个 kw(active keyword)?
onMounted(async () => { loadCategories(); await load() })
</script>

<style scoped lang="scss">
.tools-page {
  position: relative;
  /* 天空底色交给 FrontLayout（全局渐变 + 视差云层）承担；
     此处只叠一层半透明柔光，否则不透明底色会把云朵整片盖住 */
  background:
    radial-gradient(ellipse 80% 30% at 50% 100%, rgba(255, 255, 255, 0.6) 0%, transparent 70%);
}

.container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 24px;
  position: relative;
}

/* ============ Hero ============ */
.hero {
  position: relative;
  /* 不裁切——hero-bg 里的太阳光晕和云朵装饰会溢出，
     overflow:hidden 会在视口边缘形成硬截断线 */
  /* 紧凑高度：标题+副标题+搜索+提示 自然排列即可，
     不再靠大 min-height 撑开制造空洞 */
  padding: 40px 0 20px;
  text-align: center;
}
.hero-bg {
  position: absolute;
  inset: 0;
  /* 不铺不透明渐变底色——让全局天空透上来；
     只留极淡的 alpha 光斑作为装饰 */
  background:
    radial-gradient(ellipse 60% 40% at 50% 0%, rgba(255, 255, 255, 0.35) 0%, transparent 55%);
  z-index: 0;
  pointer-events: none;
}

/* 太阳：截图在标题右上方，带柔和光晕 */
.hero-sun {
  position: absolute;
  top: 8%;
  left: 58%;
  width: 64px;
  height: 64px;
  filter: drop-shadow(0 0 18px rgba(251, 191, 36, 0.6)) drop-shadow(0 0 40px rgba(251, 191, 36, 0.25));
  opacity: 0.95;
  animation: sun-pulse 4s ease-in-out infinite;
}
@keyframes sun-pulse {
  0%, 100% { transform: scale(1); opacity: 0.95; }
  50% { transform: scale(1.04); opacity: 1; }
}

/* 用 CSS 画更蓬松的白云 */
.cloud {
  position: absolute;
  border-radius: 999px;
  background: #fff;
  filter: drop-shadow(0 8px 24px rgba(125, 211, 252, 0.22));
  opacity: 0.92;
  &::before,
  &::after {
    content: '';
    position: absolute;
    background: #fff;
    border-radius: 50%;
  }
}

/* 右上角大云（截图最显眼的一朵） */
.cloud-1 {
  width: 220px;
  height: 70px;
  top: 6%;
  right: 2%;
  &::before { width: 110px; height: 110px; top: -55px; left: 28px; }
  &::after  { width: 80px; height: 80px; top: -38px; right: 34px; }
}

/* 左上角中云 */
.cloud-2 {
  width: 160px;
  height: 54px;
  top: 18%;
  left: 3%;
  opacity: 0.8;
  &::before { width: 80px; height: 80px; top: -40px; left: 22px; }
  &::after  { width: 60px; height: 60px; top: -28px; right: 26px; }
}

/* 中下部飘云 */
.cloud-3 {
  width: 130px;
  height: 44px;
  top: 62%;
  right: 18%;
  opacity: 0.72;
  &::before { width: 64px; height: 64px; top: -32px; left: 18px; }
  &::after  { width: 48px; height: 48px; top: -22px; right: 20px; }
}

/* 更远处的小云，增加层次 */
.cloud-4 {
  width: 90px;
  height: 32px;
  top: 36%;
  left: 14%;
  opacity: 0.55;
  &::before { width: 44px; height: 44px; top: -22px; left: 12px; }
  &::after  { width: 34px; height: 34px; top: -16px; right: 14px; }
}

.hero-inner {
  position: relative;
  z-index: 1;
}
.hero-title {
  font-family: var(--font-serif);
  font-size: clamp(32px, 4vw, 44px);
  font-weight: 700;
  color: #0c4a6e;
  margin: 0 0 12px;
  letter-spacing: 0.06em;
  text-shadow: 0 2px 16px rgba(255, 255, 255, 0.7);
}
/* 扫光阶段文字填充为透明，白色投影会从字形后透出一圈毛边，故关闭 */
.hero-title.ef-title-shine {
  text-shadow: none;
}
.hero-subtitle {
  font-size: 15px;
  color: #0369a1;
  margin: 0 0 18px;
  font-weight: 500;
  letter-spacing: 0.04em;
}
.hero-search {
  position: relative;
  max-width: 580px;
  margin: 0 auto;
  background: rgba(255, 255, 255, 0.98);
  border: 1px solid rgba(56, 189, 248, 0.35);
  border-radius: 999px;
  padding: 14px 22px;
  display: flex;
  align-items: center;
  gap: 10px;
  box-shadow: 0 8px 28px rgba(56, 189, 248, 0.18);
}
.hero-search-input {
  flex: 1;
  border: none;
  outline: none;
  background: transparent;
  font-size: 14px;
  color: #1e293b;
  &::placeholder { color: #94a3b8; }
}
.hero-search-icon { flex-shrink: 0; }
.hero-hint {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  margin-top: 10px;
  padding: 6px 16px;
  border-radius: 999px;
  border: 1px solid rgba(56, 189, 248, 0.3);
  background: rgba(255, 255, 255, 0.65);
  backdrop-filter: blur(8px);
  color: #0369a1;
  font-size: 13px;
  white-space: nowrap;
  .el-icon { color: #38bdf8; }
  .hint-info { color: #94a3b8; cursor: help; }
}

/* ============ Tab 行（嵌在 grid-section 内，与卡片同宽左对齐） ============ */
.tabs-row {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 16px;
  flex-wrap: wrap;
}
.tabs-chip {
  font-family: var(--font-sans);
  font-size: 13px;
  padding: 6px 14px;
  border-radius: 999px;
  border: 1px solid rgba(56, 189, 248, 0.35);
  background: rgba(255, 255, 255, 0.7);
  backdrop-filter: blur(8px);
  color: #0c4a6e;
  cursor: pointer;
  transition: all 0.25s ease;
  &:hover {
    border-color: #38bdf8;
    background: rgba(186, 230, 253, 0.5);
    transform: translateY(-1px);
  }
  &.active {
    background: linear-gradient(135deg, #38bdf8, #0ea5e9);
    color: #fff;
    border-color: transparent;
    box-shadow: 0 6px 18px rgba(56, 189, 248, 0.35);
  }
}

/* ============ 公告横幅 ============ */
.banner {
  display: flex;
  align-items: center;
  gap: 8px;
  margin: 0 auto 4px;
  padding: 10px 16px;
  border-radius: 10px;
  background: rgba(254, 243, 199, 0.6);
  border: 1px solid rgba(217, 119, 6, 0.2);
  color: #854f0b;
  font-size: 13px;
  .el-icon { color: #d97706; }
}

/* ============ 卡片网格 ============ */
.grid-section {
  padding: 0 24px 48px;
}
.grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 22px;
}
@media (max-width: 1100px) { .grid { grid-template-columns: repeat(3, 1fr); } }
@media (max-width: 800px)  { .grid { grid-template-columns: repeat(2, 1fr); } }
@media (max-width: 520px)  { .grid { grid-template-columns: 1fr; } }

.tool-card {
  position: relative;
  background: #fff;
  border: 1px solid rgba(186, 230, 253, 0.6);
  border-radius: 14px;
  padding: 22px 18px 18px;
  display: grid;
  grid-template-columns: 1fr auto;
  grid-template-rows: auto auto auto;
  grid-template-areas:
    "tag add"
    "title icon"
    "desc  desc"
    "foot  foot";
  gap: 10px 14px;
  box-shadow: 0 4px 16px rgba(56, 189, 248, 0.08);
  transition: transform 0.25s ease, box-shadow 0.25s ease, border-color 0.25s ease;
  cursor: pointer;
  overflow: hidden;

  &:hover {
    box-shadow: 0 10px 28px rgba(56, 189, 248, 0.18);
    border-color: rgba(56, 189, 248, 0.4);
  }

  &.is-maintain {
    background: #f8fafc;
    border-style: dashed;
    border-color: #cbd5e1;
    opacity: 0.7;
    .card-title { color: #64748b; }
  }
  &.is-upcoming {
    background: #fff;
    border-style: dashed;
    border-color: rgba(56, 189, 248, 0.4);
  }
  &.is-offline { opacity: 0.5; }
}

/* 鼠标跟随径向光晕层（真实 DOM，不占用 ::before/::after，
   避免 ef-card-glow / border-glow 等全局类的伪元素冲突） */
.card-radial-glow {
  position: absolute;
  inset: 0;
  top: var(--my, 50%);
  left: var(--mx, 50%);
  width: 320px;
  height: 320px;
  transform: translate(-50%, -50%);
  background: radial-gradient(
    circle,
    rgba(251, 191, 36, 0.14) 0%,
    rgba(56, 189, 248, 0.10) 30%,
    transparent 65%
  );
  pointer-events: none;
  opacity: 0;
  transition: opacity 0.35s ease;
  z-index: 0;
}
.tool-card:hover .card-radial-glow {
  opacity: 1;
}
/* 网格子内容浮于光晕层之上（光晕 absolute + z-index: 0 会盖住正常流内容） */
.tool-card > :not(.card-radial-glow) {
  position: relative;
  z-index: 1;
}

/* 骨架态 */
.skeleton {
  .sk-line, .sk-icon {
    background: linear-gradient(90deg, #f1f5f9 25%, #e2e8f0 50%, #f1f5f9 75%);
    background-size: 200% 100%;
    animation: sk-shimmer 1.4s infinite linear;
    border-radius: 6px;
  }
  .sk-icon { width: 56px; height: 56px; border-radius: 14px; }
  .sk-line { height: 12px; }
  .sk-line.w40 { width: 40%; }
  .sk-line.w50 { width: 50%; }
  .sk-line.w60 { width: 60%; }
  .sk-line.w70 { width: 70%; }
  cursor: default;
}
@keyframes sk-shimmer {
  from { background-position: 200% 0; }
  to   { background-position: -200% 0; }
}

.cat-tag {
  grid-area: tag;
  display: inline-block;
  padding: 3px 10px;
  font-size: 11px;
  background: #f1f5f9;
  color: #64748b;
  border-radius: 6px;
  align-self: flex-start;
}
.add-btn {
  grid-area: add;
  width: 22px;
  height: 22px;
  border: 1px solid #e2e8f0;
  background: #fff;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  color: #94a3b8;
  transition: all 0.2s ease;
  .el-icon { font-size: 12px; }
  &:hover {
    color: #0ea5e9;
    border-color: #0ea5e9;
    transform: scale(1.05);
  }
  &.active {
    color: #fbbf24;
    border-color: #fbbf24;
    background: rgba(251, 191, 36, 0.08);
  }
}
.card-title {
  grid-area: title;
  font-family: var(--font-serif);
  font-size: 17px;
  font-weight: 600;
  color: #0c4a6e;
  margin: 0;
  line-height: 1.3;
  padding-right: 50px; // 避开右上 icon
  align-self: center;
}
.card-desc {
  grid-area: desc;
  font-size: 12px;
  color: #64748b;
  line-height: 1.6;
  margin: 0;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  min-height: 38px;
}
.card-icon {
  grid-area: icon;
  width: 56px;
  height: 56px;
  align-self: center;
  justify-self: end;
  opacity: 0.85;
}
.card-icon-img {
  object-fit: contain;
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.55);
  padding: 4px;
}

.card-foot {
  grid-area: foot;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  margin-top: 4px;
  padding-top: 12px;
  border-top: 1px dashed rgba(186, 230, 253, 0.6);
}
.cat-pill {
  display: inline-block;
  padding: 5px 14px;
  font-size: 12px;
  border-radius: 999px;
  background: linear-gradient(135deg, #38bdf8, #0ea5e9);
  color: #fff;
  &.muted { background: #f1f5f9; color: #94a3b8; }
}
.use-btn {
  display: inline-block;
  padding: 6px 18px;
  font-size: 12px;
  border-radius: 999px;
  background: #fff;
  border: 1px solid rgba(56, 189, 248, 0.5);
  color: #0ea5e9;
  cursor: pointer;
  transition: all 0.2s ease;
  &:hover {
    background: linear-gradient(135deg, #38bdf8, #0ea5e9);
    color: #fff;
    border-color: transparent;
    box-shadow: 0 4px 12px rgba(56, 189, 248, 0.3);
  }
  &.muted {
    border-color: #cbd5e1;
    color: #94a3b8;
    cursor: not-allowed;
    &:hover { background: #fff; color: #94a3b8; box-shadow: none; }
  }
  &.upcoming {
    background: rgba(56, 189, 248, 0.1);
    border-color: rgba(56, 189, 248, 0.4);
    color: #0ea5e9;
    cursor: default;
    &:hover { background: rgba(56, 189, 248, 0.1); color: #0ea5e9; }
  }
}

/* 空状态 —— 与首行卡片等高（~280px），flex 居中，
   避免「有工具时 hero+卡=满屏 / 无工具时 hero 撑大卡区变空」的视觉落差 */
.empty {
  flex: 1;
  min-height: 280px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  text-align: center;
  padding: 32px 0;
  :deep(.el-button) { margin-top: 12px; }
}

/* ============ 底部 ============ */
.footer {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 14px;
  padding: 24px;
  font-size: 13px;
  color: #94a3b8;
  .dot {
    width: 4px;
    height: 4px;
    background: #cbd5e1;
    border-radius: 50%;
  }
}

/* 横幅出现动效 */
.banner-fade-enter-active, .banner-fade-leave-active {
  transition: all 0.3s ease;
}
.banner-fade-enter-from, .banner-fade-leave-to {
  opacity: 0;
  transform: translateY(-8px);
}
</style>