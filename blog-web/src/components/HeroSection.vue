<template>
  <section class="hero" ref="heroRef">
    <ParticleBg />
    <div class="bg-shape shape-1"></div>
    <div class="bg-shape shape-2"></div>
    <div class="bg-shape shape-3"></div>
    <div class="container hero-inner">
      <div class="hero-text">
        <div class="hero-tag">
          <span class="dot"></span>
          欢迎来到我的小角落
        </div>
        <h1 class="hero-title">
          {{ siteStore.info?.siteName || 'DemoAuthor' }}
          <span class="title-accent">{{ siteStore.info?.motto || '草木蔓发，春山可望' }}</span>
        </h1>
        <p class="hero-desc">
          {{ siteStore.info?.description || '这里记录一些代码、设计、生活与思考。慢一点，让灵魂跟上脚步。' }}
        </p>
        <div class="hero-actions">
          <el-button type="primary" size="large" class="btn-pulse" @click="$router.push('/articles')">
            <el-icon><Reading /></el-icon>&nbsp;开始阅读
          </el-button>
          <el-button size="large" plain @click="$router.push('/about')">
            <el-icon><User /></el-icon>&nbsp;关于我
          </el-button>
        </div>
        <div class="hero-stats">
          <div class="stat">
            <div class="stat-num">{{ stats.articleCount || 0 }}</div>
            <div class="stat-label">文章</div>
          </div>
          <div class="stat">
            <div class="stat-num">{{ stats.categoryCount || 0 }}</div>
            <div class="stat-label">分类</div>
          </div>
          <div class="stat">
            <div class="stat-num">{{ stats.tagCount || 0 }}</div>
            <div class="stat-label">标签</div>
          </div>
          <div class="stat">
            <div class="stat-num">{{ stats.viewCount || 0 }}</div>
            <div class="stat-label">阅读</div>
          </div>
        </div>
      </div>
      <div class="hero-visual">
        <div class="visual-card vc-1">
          <div class="vc-tag">📝 随笔</div>
          <div class="vc-line"></div>
          <div class="vc-line w70"></div>
          <div class="vc-line w50"></div>
        </div>
        <div class="visual-card vc-2">
          <div class="vc-tag">💻 代码</div>
          <div class="vc-line"></div>
          <div class="vc-line w80"></div>
        </div>
        <div class="visual-card vc-3">
          <div class="vc-tag">🌿 生活</div>
          <div class="vc-line"></div>
          <div class="vc-line w60"></div>
        </div>
      </div>
    </div>
  </section>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount } from 'vue'
import { Reading, User } from '@element-plus/icons-vue'
import { useSiteStore } from '@/stores/site'
import ParticleBg from '@/components/ParticleBg.vue'

const siteStore = useSiteStore()
const heroRef = ref(null)

defineProps({
  stats: { type: Object, default: () => ({}) }
})

let scrollRaf = null
const onScroll = () => {
  if (!heroRef.value) return
  if (scrollRaf) return
  scrollRaf = requestAnimationFrame(() => {
    const s = window.scrollY
    heroRef.value.style.transform = `translateY(${s * 0.3}px)`
    const inner = heroRef.value.querySelector('.hero-inner')
    if (inner) inner.style.transform = `translateY(${-s * 0.15}px)`
    scrollRaf = null
  })
}

onMounted(() => window.addEventListener('scroll', onScroll, { passive: true }))
onBeforeUnmount(() => window.removeEventListener('scroll', onScroll))
</script>

<style scoped lang="scss">
.hero {
  position: relative;
  padding: 90px 0 70px;
  overflow: hidden;
  background: linear-gradient(180deg, #e0f7ff 0%, #f0f9ff 50%, #ffffff 100%);
}
.bg-shape {
  position: absolute;
  border-radius: 50%;
  filter: blur(60px);
  opacity: 0.55;
  z-index: 0;
  animation: drift 12s ease-in-out infinite;
}
.shape-1 {
  width: 380px; height: 380px;
  background: rgba(56, 189, 248, 0.25);
  top: -120px; right: -80px;
  animation-delay: 0s;
}
.shape-2 {
  width: 320px; height: 320px;
  background: rgba(34, 211, 238, 0.22);
  bottom: -100px; left: -60px;
  animation-delay: -4s;
}
.shape-3 {
  width: 240px; height: 240px;
  background: rgba(251, 191, 36, 0.18);
  top: 45%; right: 28%;
  animation-delay: -8s;
}
@keyframes drift {
  0%, 100% { transform: translate(0, 0) scale(1); }
  33% { transform: translate(20px, -20px) scale(1.05); }
  66% { transform: translate(-10px, 15px) scale(0.98); }
}

.hero-inner {
  position: relative;
  z-index: 1;
  display: grid;
  grid-template-columns: 1.3fr 1fr;
  gap: 60px;
  align-items: center;
}
.hero-tag {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 6px 14px;
  background: var(--c-botany-50);
  color: var(--c-botany-700);
  border-radius: 999px;
  font-size: 13px;
  margin-bottom: 20px;
  border: 1px solid var(--c-botany-100);
}
.hero-tag .dot {
  width: 6px; height: 6px; border-radius: 50%;
  background: var(--c-autumn-500);
  animation: pulse 2s infinite;
}
@keyframes pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.4; }
}
.hero-title {
  font-family: var(--font-serif);
  font-size: 52px;
  line-height: 1.2;
  margin: 0 0 20px;
  font-weight: 600;
  color: var(--c-ink);
}
.title-accent {
  display: block;
  font-size: 22px;
  color: var(--c-ink-soft);
  font-weight: 400;
  margin-top: 12px;
  letter-spacing: 0.05em;
}
.hero-desc {
  font-size: 16px;
  color: var(--c-ink-soft);
  line-height: 1.8;
  max-width: 520px;
  margin: 0 0 32px;
}
.hero-actions {
  display: flex;
  gap: 14px;
  margin-bottom: 40px;
}

.hero-stats {
  display: flex;
  gap: 32px;
}
.stat-num {
  font-family: var(--font-serif);
  font-size: 28px;
  font-weight: 600;
  color: var(--c-botany-700);
}
.stat-label {
  font-size: 13px;
  color: var(--c-ink-soft);
  margin-top: 4px;
}

.hero-visual {
  position: relative;
  height: 380px;
}
.visual-card {
  position: absolute;
  background: #fff;
  border-radius: 14px;
  padding: 18px 22px;
  box-shadow: var(--shadow-pop);
  border: 1px solid var(--c-line-soft);
}
.vc-1 {
  top: 0; left: 0;
  width: 220px;
  animation: float 6s ease-in-out infinite;
}
.vc-2 {
  top: 100px; right: 0;
  width: 240px;
  animation: float 7s ease-in-out infinite 0.5s;
}
.vc-3 {
  bottom: 0; left: 50px;
  width: 200px;
  animation: float 8s ease-in-out infinite 1s;
}
@keyframes float {
  0%, 100% { transform: translateY(0); }
  50% { transform: translateY(-12px); }
}
.vc-tag {
  font-size: 13px;
  color: var(--c-autumn-700);
  margin-bottom: 12px;
}
.vc-line {
  height: 8px;
  background: var(--c-line);
  border-radius: 4px;
  margin: 8px 0;
}
.vc-line.w70 { width: 70%; }
.vc-line.w50 { width: 50%; }
.vc-line.w80 { width: 80%; }
.vc-line.w60 { width: 60%; }

@media (max-width: 900px) {
  .hero-inner { grid-template-columns: 1fr; }
  .hero-visual { display: none; }
  .hero-title { font-size: 38px; }
  .hero-stats { gap: 20px; }
  .stat-num { font-size: 22px; }
}
</style>