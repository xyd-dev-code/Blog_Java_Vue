<template>
  <div class="exhibition-page">
    <!-- Hero -->
    <HeroProjects title="PROJECT" subtitle="拖拽浏览 · 每一张海报，一段旅程" />

    <!-- ===== Status States ===== -->
    <div v-if="loading" class="exh-status">
      <span class="status-blink">▮</span> 加载中…
    </div>
    <div v-else-if="rawProjects.length === 0" class="exh-status">
      <code class="status-path">~/projects</code> 暂无项目，敬请期待。
    </div>

    <!-- ===== Horizontal Track (infinite loop) ===== -->
    <div
      v-else
      class="exh-track"
      :class="{ 'exh-track-center': displayedProjects.length === 1 }"
      ref="trackRef"
      @mousedown="startDrag"
      @scroll.passive="onTrackScroll"
    >
      <article
        v-for="(p, idx) in displayedProjects"
        :key="`${p.id}-${idx}`"
        class="poster"
        :style="{ '--poster-color': p.color || '#38bdf8' }"
      >
        <!-- CSS grid lines background -->
        <div class="poster-grid"></div>

        <!-- Line numbers decoration -->
        <div class="poster-lines" aria-hidden="true">
          <span v-for="n in 14" :key="n">{{ n }}</span>
        </div>

        <!-- Main content area -->
        <div class="poster-inner">
          <!-- Left column: icon + category badge -->
          <div class="poster-col-left">
            <div class="poster-icon-box" :style="{ background: p.color || '#38bdf8' }">
              <el-icon :size="34">
                <component :is="resolveIcon(p)" />
              </el-icon>
            </div>
            <span class="poster-kind-badge">{{ p.kind }}</span>
          </div>

          <!-- Right column: details -->
          <div class="poster-col-right">
            <h2 class="poster-name">{{ p.name }}</h2>
            <p class="poster-desc">{{ p.description }}</p>

            <!-- ===== Terminal block ===== -->
            <div class="terminal">
              <div class="terminal-header">
                <span class="term-dot term-dot-red"></span>
                <span class="term-dot term-dot-yellow"></span>
                <span class="term-dot term-dot-green"></span>
                <span class="term-title">{{ terminalHost(p.name) }}</span>
              </div>
              <div class="terminal-body">
                <div class="terminal-line">
                  <span class="term-prompt">❯</span>
                  <span class="term-cmd">cat tech.stack</span>
                </div>
                <div class="terminal-line terminal-output-line">
                  <span class="term-prompt term-prompt-dim">❯</span>
                  <span class="term-tag" v-for="t in (p.stack || [])" :key="t">{{ t }}</span>
                </div>
              </div>
            </div>

            <!-- Action links -->
            <div class="poster-actions">
              <a v-if="p.githubUrl" :href="p.githubUrl" target="_blank" rel="noopener" class="action-btn">
                <el-icon><Link /></el-icon>
                <span>GitHub</span>
              </a>
              <a v-if="p.demoUrl" :href="p.demoUrl" target="_blank" rel="noopener" class="action-btn action-btn-primary">
                <el-icon><View /></el-icon>
                <span>Live Demo</span>
              </a>
            </div>
          </div>
        </div>

        <!-- Index number (bottom-right) -->
        <div class="poster-num">{{ String((idx % rawProjects.length) + 1).padStart(2, '0') }}</div>
      </article>
    </div>

    <!-- ===== Bottom Navigation ===== -->
    <div class="exh-nav" v-if="rawProjects.length > 0">
      <button class="nav-btn" @click="scrollPrev" aria-label="上一个">
        <el-icon><ArrowLeft /></el-icon>
      </button>

      <div class="nav-indicators">
        <button
          v-for="(p, i) in rawProjects"
          :key="p.id"
          class="nav-indicator"
          :class="{ active: activeRealIndex === i }"
          @click="scrollToReal(i)"
          :aria-label="`跳转到 ${p.name}`"
        >
          <span class="nav-num">{{ String(i + 1).padStart(2, '0') }}</span>
          <span class="nav-name">{{ p.name }}</span>
        </button>
      </div>

      <button class="nav-btn" @click="scrollNext" aria-label="下一个">
        <el-icon><ArrowRight /></el-icon>
      </button>
    </div>

    <!-- Keyboard hint -->
    <div class="key-hint" v-if="rawProjects.length > 0">
      <kbd>←</kbd><kbd>→</kbd> 键盘导航
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, nextTick, computed } from 'vue'
import {
  Link, View, Folder, ArrowLeft, ArrowRight,
  Document, Picture, Box, MagicStick, ChatDotRound,
  Calendar, Headset, Reading, DataAnalysis, Coin, Lock,
  Sunny, Cloudy, Lightning, HomeFilled, Star, Brush, Share,
} from '@element-plus/icons-vue'
import { projects as fetchProjects } from '@/api/front'
import HeroProjects from '@/components/HeroProjects.vue'

// ---- Icon mapping ----
const iconMap = {
  Folder, Link, View, Document, Picture, Box, MagicStick, ChatDotRound,
  Calendar, Headset, Reading, DataAnalysis, Coin, Lock,
  Sunny, Cloudy, Lightning, HomeFilled, Star, Brush, Share,
}

// 候选图标池（用作默认轮换）
const iconPool = [
  Document, Picture, Box, MagicStick, ChatDotRound,
  Calendar, Headset, Reading, DataAnalysis, Coin, Lock,
  Sunny, Cloudy, Lightning, HomeFilled, Star, Brush, Share, Folder,
]

// 为每个项目解析图标：1) 优先用后端 icon 字段；2) 否则按项目 id 哈希分配
const resolveIcon = (p) => {
  if (p.icon && iconMap[p.icon]) return iconMap[p.icon]
  const idNum = Number(p.id) || 0
  return iconPool[idNum % iconPool.length]
}

// ---- Reactive state ----
const rawProjects = ref([])
const displayedProjects = computed(() => {
  const raw = rawProjects.value
  if (raw.length < 2) return raw
  // Triple clone for seamless infinite loop
  return [...raw, ...raw, ...raw]
})
const loading = ref(false)
const activeDomIndex = ref(0)      // 0 ~ 3N-1, actual DOM index
const activeRealIndex = computed(() => {
  const N = rawProjects.value.length
  return N ? ((activeDomIndex.value % N) + N) % N : 0
})
const trackRef = ref(null)

// ---- Drag state ----
let isDragging = false
let dragStartX = 0
let dragScrollLeft = 0

// ---- Data ----
onMounted(async () => {
  loading.value = true
  try {
    rawProjects.value = (await fetchProjects()).data || []
  } catch (_) {
    /* keep empty */
  }
  loading.value = false
  await nextTick()
  initLoopPosition()
  window.addEventListener('keydown', onKeyDown)
  window.addEventListener('mousemove', onDrag)
  window.addEventListener('mouseup', endDrag)
})

onUnmounted(() => {
  window.removeEventListener('keydown', onKeyDown)
  window.removeEventListener('mousemove', onDrag)
  window.removeEventListener('mouseup', endDrag)
})

// ---- Terminal host helper ----
function terminalHost(name) {
  return `stack@${(name || 'project').toLowerCase().replace(/\s+/g, '-')}`
}

// ==================== INFINITE LOOP INIT ====================

function initLoopPosition() {
  const track = trackRef.value
  if (!track || !rawProjects.value.length) return
  const N = rawProjects.value.length
  if (N < 2) return
  const posters = track.querySelectorAll('.poster')
  if (posters.length < 3 * N) return

  // Start at the middle copy (index N), centered
  const mid = posters[N]
  if (!mid) return

  const targetLeft = mid.offsetLeft - (track.offsetWidth - mid.offsetWidth) / 2
  track.style.scrollBehavior = 'auto'
  track.scrollLeft = targetLeft
  track.style.scrollBehavior = ''
  activeDomIndex.value = N
}

// ==================== TRACK NAVIGATION ====================

function startDrag(e) {
  const track = trackRef.value
  if (!track) return
  isDragging = true
  dragStartX = e.pageX - track.offsetLeft
  dragScrollLeft = track.scrollLeft
  track.style.cursor = 'grabbing'
  track.style.scrollBehavior = 'auto'
}

function onDrag(e) {
  if (!isDragging) return
  const track = trackRef.value
  if (!track) return
  e.preventDefault()
  const x = e.pageX - track.offsetLeft
  const walk = (x - dragStartX) * 1.8
  track.scrollLeft = dragScrollLeft - walk
}

function endDrag() {
  if (!isDragging) return
  isDragging = false
  const track = trackRef.value
  if (track) {
    track.style.cursor = ''
    track.style.scrollBehavior = ''
  }
}

function onTrackScroll() {
  const track = trackRef.value
  if (!track || !rawProjects.value.length) return
  const N = rawProjects.value.length
  if (N < 2) return
  const posters = track.querySelectorAll('.poster')
  if (posters.length < 3 * N) return

  const start1 = posters[0].offsetLeft
  const start3 = posters[2 * N].offsetLeft
  const firstW = posters[0].offsetWidth || 1

  // --- Left boundary: near first copy start -> teleport to third copy ---
  if (track.scrollLeft < start1 + firstW) {
    track.style.scrollBehavior = 'auto'
    const delta = track.scrollLeft - start1
    track.scrollLeft = start3 + delta
    track.style.scrollBehavior = ''
    setTimeout(() => updateActiveDomIndex(), 0)
    return
  }

  // --- Right boundary: near third copy end -> teleport to first copy ---
  const last3 = posters[3 * N - 1]
  if (last3 && track.scrollLeft > last3.offsetLeft - firstW) {
    track.style.scrollBehavior = 'auto'
    const delta = track.scrollLeft - start3
    track.scrollLeft = start1 + delta
    track.style.scrollBehavior = ''
    setTimeout(() => updateActiveDomIndex(), 0)
    return
  }

  updateActiveDomIndex()
}

function updateActiveDomIndex() {
  const track = trackRef.value
  if (!track) return
  const posters = track.querySelectorAll('.poster')
  let closest = 0
  let minDist = Infinity
  const center = track.scrollLeft + track.offsetWidth / 2

  posters.forEach((el, i) => {
    const dist = Math.abs(el.offsetLeft + el.offsetWidth / 2 - center)
    if (dist < minDist) {
      minDist = dist
      closest = i
    }
  })
  activeDomIndex.value = closest
}

function scrollToDom(domIndex) {
  const track = trackRef.value
  if (!track || !rawProjects.value.length) return
  const N = rawProjects.value.length
  const posters = track.querySelectorAll('.poster')
  if (posters.length < 3 * N) return

  const target = posters[domIndex]
  if (!target) return

  const start1 = posters[0].offsetLeft
  const start3 = posters[2 * N].offsetLeft
  const firstW = posters[0].offsetWidth || 1
  const targetLeft = target.offsetLeft - (track.offsetWidth - target.offsetWidth) / 2

  // Pre-teleport if target would trigger left boundary detection
  if (domIndex < N && targetLeft < start1 + firstW) {
    const correspondingIndex = domIndex + 2 * N
    const correspondingTarget = posters[correspondingIndex]
    if (correspondingTarget) {
      track.style.scrollBehavior = 'auto'
      const correspondingLeft = correspondingTarget.offsetLeft - (track.offsetWidth - correspondingTarget.offsetWidth) / 2
      track.scrollLeft = correspondingLeft
      track.style.scrollBehavior = ''
      activeDomIndex.value = correspondingIndex
      return
    }
  }

  // Pre-teleport if target would trigger right boundary detection
  if (domIndex >= 2 * N) {
    const last3 = posters[3 * N - 1]
    if (last3 && targetLeft > last3.offsetLeft - firstW) {
      const correspondingIndex = domIndex - 2 * N
      const correspondingTarget = posters[correspondingIndex]
      if (correspondingTarget) {
        track.style.scrollBehavior = 'auto'
        const correspondingLeft = correspondingTarget.offsetLeft - (track.offsetWidth - correspondingTarget.offsetWidth) / 2
        track.scrollLeft = correspondingLeft
        track.style.scrollBehavior = ''
        activeDomIndex.value = correspondingIndex
        return
      }
    }
  }

  // Normal smooth scroll
  track.style.scrollBehavior = 'smooth'
  track.scrollTo({
    left: targetLeft,
    behavior: 'smooth',
  })
  activeDomIndex.value = domIndex
  setTimeout(() => {
    if (trackRef.value) trackRef.value.style.scrollBehavior = ''
  }, 450)
}

function scrollToReal(realIndex) {
  const N = rawProjects.value.length
  const domIndex = N + ((realIndex % N) + N) % N
  scrollToDom(domIndex)
}

function scrollPrev() {
  const N = rawProjects.value.length
  const prev = activeDomIndex.value - 1
  scrollToDom(prev < 0 ? 3 * N - 1 : prev)
}

function scrollNext() {
  const N = rawProjects.value.length
  const next = activeDomIndex.value + 1
  scrollToDom(next >= 3 * N ? 0 : next)
}

function onKeyDown(e) {
  if (e.key === 'ArrowLeft') {
    e.preventDefault()
    scrollPrev()
  } else if (e.key === 'ArrowRight') {
    e.preventDefault()
    scrollNext()
  }
}

// ==================== CODE RAIN CANVAS ====================
</script>

<style scoped lang="scss">
/* ==================== PAGE (晴空 v2) ==================== */
.exhibition-page {
  position: relative;
  overflow: hidden;
  background: linear-gradient(170deg,
    #f0f9ff 0%,
    #fffbeb 35%,
    #ffffff 65%,
    #f0f9ff 100%
  );
  display: flex;
  flex-direction: column;
  user-select: none;
}

/* ==================== STATUS ==================== */
.exh-status {
  position: relative;
  z-index: 2;
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 15px;
  color: var(--c-ink-soft);
  gap: 8px;
}

.status-blink {
  color: #fbbf24;
  animation: blink-cursor 0.8s step-end infinite;
}

.status-path {
  font-family: 'Consolas', 'Monaco', 'Courier New', monospace;
  font-size: 14px;
  color: #0369a1;
  background: rgba(56, 189, 248, 0.08);
  padding: 2px 8px;
  border-radius: 4px;
}

/* ==================== TRACK (HORIZONTAL SCROLL) ==================== */
.exh-track {
  position: relative;
  z-index: 2;
  flex: 1;
  display: flex;
  gap: 24px;
  overflow-x: scroll;
  overflow-y: hidden;
  cursor: grab;
  padding: 24px 0;
  align-items: center;

  // Hide scrollbar
  -ms-overflow-style: none;
  scrollbar-width: none;
  &::-webkit-scrollbar { display: none; }
}

/* 单项目居中 */
.exh-track-center {
  overflow-x: hidden;
  justify-content: center;
}

.exh-track:active {
  cursor: grabbing;
}

/* ==================== POSTER CARD ==================== */
.poster {
  position: relative;
  flex: 0 0 min(62vw, 900px);
  min-height: 55vh;
  max-height: 72vh;
  background: rgba(255, 255, 255, 0.82);
  backdrop-filter: blur(16px);
  -webkit-backdrop-filter: blur(16px);
  border: 1px solid rgba(56, 189, 248, 0.18);
  border-radius: 28px;
  padding: 56px 64px 64px;
  display: flex;
  flex-direction: column;
  justify-content: center;
  overflow: hidden;
  box-shadow:
    0 4px 24px rgba(14, 165, 233, 0.08),
    0 20px 60px rgba(251, 191, 36, 0.08);
  transition:
    transform 0.4s cubic-bezier(0.25, 0.8, 0.25, 1),
    box-shadow 0.4s cubic-bezier(0.25, 0.8, 0.25, 1),
    border-color 0.4s ease;
}

/* ==================== POSTER: GRID LINES BG ==================== */
.poster-grid {
  position: absolute;
  inset: 0;
  pointer-events: none;
  z-index: 0;
  background-image:
    linear-gradient(rgba(56, 189, 248, 0.05) 1px, transparent 1px),
    linear-gradient(90deg, rgba(251, 191, 36, 0.04) 1px, transparent 1px);
  background-size: 44px 44px;
  mask-image: radial-gradient(ellipse 70% 70% at 50% 50%, black 30%, transparent 75%);
  -webkit-mask-image: radial-gradient(ellipse 70% 70% at 50% 50%, black 30%, transparent 75%);
}

/* ==================== POSTER: LINE NUMBERS ==================== */
.poster-lines {
  position: absolute;
  top: 30px;
  left: 24px;
  z-index: 1;
  display: flex;
  flex-direction: column;
  gap: 12px;
  pointer-events: none;

  span {
    font-family: 'Consolas', 'Monaco', 'Courier New', monospace;
    font-size: 11px;
    color: rgba(56, 189, 248, 0.22);
    text-align: right;
    line-height: 1;
  }
}

/* ==================== POSTER: INNER CONTENT ==================== */
.poster-inner {
  position: relative;
  z-index: 1;
  display: flex;
  gap: 48px;
  align-items: flex-start;
}

.poster-col-left {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 14px;
  flex-shrink: 0;
  padding-top: 4px;
}

.poster-icon-box {
  width: 72px;
  height: 72px;
  border-radius: 18px;
  display: grid;
  place-items: center;
  color: #fff;
  background: linear-gradient(135deg, #38bdf8, #fbbf24) !important;
  box-shadow: 0 8px 24px rgba(14, 165, 233, 0.25);
  transition: transform 0.35s cubic-bezier(0.34, 1.56, 0.64, 1);
}

.poster-kind-badge {
  display: inline-block;
  font-family: 'Consolas', 'Monaco', 'Courier New', monospace;
  font-size: 11px;
  padding: 4px 12px;
  border-radius: 999px;
  background: rgba(56, 189, 248, 0.1);
  color: #0369a1;
  border: 1px solid rgba(56, 189, 248, 0.2);
  letter-spacing: 0.05em;
  text-transform: uppercase;
}

.poster-col-right {
  flex: 1;
  min-width: 0;
}

.poster-name {
  font-family: var(--font-serif);
  font-size: clamp(28px, 4vw, 42px);
  font-weight: 700;
  background: linear-gradient(135deg, #0369a1, #38bdf8);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
          background-clip: text;
  margin: 0 0 14px;
  line-height: 1.2;
  letter-spacing: 0.03em;
}

.poster-desc {
  font-size: 15px;
  color: var(--c-ink-soft);
  line-height: 1.8;
  margin: 0 0 28px;
  max-width: 540px;
}

/* ==================== TERMINAL BLOCK ==================== */
.terminal {
  background: linear-gradient(135deg, #0f172a 0%, #1e293b 100%);
  border-radius: 12px;
  overflow: hidden;
  border: 1px solid rgba(125, 211, 252, 0.25);
  box-shadow:
    0 4px 20px rgba(15, 23, 42, 0.15),
    0 0 30px rgba(56, 189, 248, 0.08);
  max-width: 540px;
  margin-bottom: 28px;
}

.terminal-header {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 16px;
  background: rgba(255, 255, 255, 0.04);
  border-bottom: 1px solid rgba(255, 255, 255, 0.06);
}

.term-dot {
  width: 11px;
  height: 11px;
  border-radius: 50%;
  flex-shrink: 0;
}

.term-dot-red { background: #ff5f56; }
.term-dot-yellow { background: #ffbd2e; }
.term-dot-green { background: #27c93f; }

.term-title {
  margin-left: 8px;
  font-family: 'Consolas', 'Monaco', 'Courier New', monospace;
  font-size: 12px;
  color: rgba(255, 255, 255, 0.45);
  letter-spacing: 0.03em;
}

.terminal-body {
  padding: 14px 18px;
}

.terminal-line {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  font-family: 'Consolas', 'Monaco', 'Courier New', monospace;
  font-size: 14px;
  line-height: 1.6;
}

.terminal-line + .terminal-line {
  margin-top: 8px;
}

.term-prompt {
  color: #38bdf8;
  flex-shrink: 0;
}

.term-prompt-dim {
  color: rgba(56, 189, 248, 0.5);
}

.term-cmd {
  color: #e6edf3;
}

.terminal-output-line {
  flex-wrap: wrap;
  padding-top: 10px;
  border-top: 1px solid rgba(255, 255, 255, 0.05);
}

.term-tag {
  display: inline-block;
  font-family: 'Consolas', 'Monaco', 'Courier New', monospace;
  font-size: 13px;
  padding: 3px 10px;
  background: rgba(56, 189, 248, 0.12);
  color: #7dd3fc;
  border-radius: 4px;
  border: 1px solid rgba(56, 189, 248, 0.18);
  margin-right: 6px;
  margin-bottom: 6px;
  transition: all 0.2s ease;
}

.term-tag:hover {
  background: rgba(56, 189, 248, 0.25);
  color: #bae6fd;
  border-color: rgba(56, 189, 248, 0.4);
}

/* ==================== POSTER: ACTION BUTTONS ==================== */
.poster-actions {
  display: flex;
  gap: 14px;
}

.action-btn {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 10px 22px;
  border-radius: 999px;
  font-size: 14px;
  font-weight: 500;
  color: #0369a1;
  background: rgba(56, 189, 248, 0.08);
  border: 1px solid rgba(56, 189, 248, 0.2);
  transition: all 0.25s ease;
  text-decoration: none;

  &:hover {
    color: #fff;
    background: linear-gradient(135deg, #38bdf8, #22d3ee);
    border-color: transparent;
    transform: translateY(-1px);
    box-shadow: 0 6px 20px rgba(56, 189, 248, 0.3);
  }
}

.action-btn-primary {
  color: #fff;
  background: linear-gradient(135deg, #fbbf24, #f59e0b);
  border-color: transparent;
  box-shadow: 0 4px 14px rgba(251, 191, 36, 0.3);

  &:hover {
    color: #fff;
    background: linear-gradient(135deg, #f59e0b, #d97706);
    box-shadow: 0 6px 20px rgba(251, 191, 36, 0.45);
  }
}

/* ==================== POSTER: INDEX NUMBER ==================== */
.poster-num {
  position: absolute;
  bottom: 32px;
  right: 40px;
  font-family: 'Consolas', 'Monaco', 'Courier New', monospace;
  font-size: 80px;
  font-weight: 700;
  line-height: 1;
  pointer-events: none;
  z-index: 0;
  letter-spacing: -0.02em;
  background: linear-gradient(135deg, rgba(56, 189, 248, 0.1), rgba(251, 191, 36, 0.08));
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
          background-clip: text;
}

/* ==================== NAVIGATION ==================== */
.exh-nav {
  position: relative;
  z-index: 3;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 20px;
  padding: 24px 24px 32px;
  flex-shrink: 0;
}

.nav-btn {
  width: 42px;
  height: 42px;
  border-radius: 50%;
  border: 1px solid rgba(56, 189, 248, 0.3);
  background: #fff;
  display: grid;
  place-items: center;
  cursor: pointer;
  color: #0369a1;
  font-size: 18px;
  transition: all 0.25s ease;
  flex-shrink: 0;

  &:hover {
    color: #fff;
    background: linear-gradient(135deg, #38bdf8, #22d3ee);
    border-color: transparent;
    box-shadow: 0 6px 18px rgba(56, 189, 248, 0.3);
    transform: scale(1.06);
  }
}

.nav-indicators {
  display: flex;
  gap: 6px;
  overflow-x: auto;
  max-width: 60vw;
  padding: 4px 0;
  scrollbar-width: none;

  &::-webkit-scrollbar { display: none; }
}

.nav-indicator {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 16px;
  border-radius: 999px;
  border: 1px solid transparent;
  background: rgba(255, 255, 255, 0.6);
  cursor: pointer;
  white-space: nowrap;
  transition: all 0.25s ease;
  font-size: 13px;
  color: var(--c-ink-soft);
  backdrop-filter: blur(6px);

  &:hover {
    background: rgba(56, 189, 248, 0.1);
    color: #0369a1;
  }

  &.active {
    background: linear-gradient(135deg, #38bdf8, #22d3ee);
    border-color: transparent;
    color: #fff;
    box-shadow: 0 4px 12px rgba(56, 189, 248, 0.3);

    .nav-num {
      background: rgba(255, 255, 255, 0.3);
      color: #fff;
    }
  }
}

.nav-num {
  display: inline-block;
  width: 22px;
  height: 22px;
  line-height: 22px;
  border-radius: 50%;
  background: #f0f9ff;
  color: #0369a1;
  font-family: 'Consolas', 'Monaco', 'Courier New', monospace;
  font-size: 11px;
  font-weight: 600;
  text-align: center;
  transition: all 0.25s ease;
}

.nav-name {
  max-width: 100px;
  overflow: hidden;
  text-overflow: ellipsis;
}

/* ==================== KEYBOARD HINT ==================== */
.key-hint {
  position: relative;
  z-index: 2;
  text-align: center;
  padding-bottom: 20px;
  font-size: 12px;
  color: var(--c-ink-300);
  flex-shrink: 0;

  kbd {
    display: inline-block;
    padding: 2px 7px;
    border: 1px solid rgba(56, 189, 248, 0.3);
    border-radius: 4px;
    background: #fff;
    font-family: 'Consolas', 'Monaco', 'Courier New', monospace;
    font-size: 11px;
    color: #0369a1;
    margin: 0 3px;
    box-shadow: 0 1px 0 rgba(56, 189, 248, 0.15);
  }
}

/* ==================== RESPONSIVE ==================== */
@media (max-width: 1024px) {
  .poster {
    flex: 0 0 75vw;
    padding: 44px 40px 52px;
    min-height: 50vh;
    max-height: 65vh;
  }

  .poster-inner {
    gap: 32px;
  }

  .poster-lines {
    display: none;
  }
}

@media (max-width: 768px) {
  .exhibition-page {
    min-height: auto;
    overflow: visible;
  }

  .exh-track {
    flex-direction: column;
    overflow-x: hidden;
    overflow-y: auto;
    padding: 16px 20px 40px;
    gap: 20px;
    cursor: auto;
  }

  .poster {
    flex: 0 0 auto;
    min-height: auto;
    max-height: none;
    padding: 36px 28px 44px;
    border-radius: 20px;
  }

  .poster-inner {
    flex-direction: column;
    gap: 20px;
  }

  .poster-col-left {
    flex-direction: row;
    gap: 12px;
  }

  .poster-icon-box {
    width: 52px;
    height: 52px;
    border-radius: 14px;
  }

  .poster-name {
    font-size: 26px;
  }

  .poster-num {
    font-size: 56px;
    bottom: 20px;
    right: 24px;
  }

  .terminal {
    max-width: 100%;
  }

  .poster-actions {
    flex-direction: column;
    gap: 10px;
  }

  .action-btn {
    justify-content: center;
  }

  .exh-nav {
    padding: 16px 20px 24px;
  }

  .nav-indicators {
    max-width: 50vw;
  }

  .key-hint {
    display: none;
  }
}

@media (max-width: 480px) {
  .exh-track { padding: 12px 14px 32px; gap: 16px; }
  .poster { padding: 28px 22px 36px; border-radius: 18px; }
  .poster-title { font-size: 22px; }
  .poster-desc { font-size: 14px; }
  .poster-stack-logos, .poster-stack-icons { gap: 8px; }
  .action-btn { padding: 10px 14px; font-size: 13px; }
  .exh-nav { padding: 12px 14px 18px; gap: 8px; }
  .nav-indicators { max-width: 70vw; gap: 6px; }
  .nav-dot { width: 18px; height: 4px; }
}
</style>
