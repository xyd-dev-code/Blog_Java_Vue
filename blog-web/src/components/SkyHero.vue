<template>
  <section class="sky-hero" ref="rootRef">
    <!-- 装饰层 -->
    <div class="deco">
      <div class="sun" :style="sunStyle"></div>
      <svg class="cloud cloud-a" viewBox="0 0 64 24" fill="white" aria-hidden="true">
        <ellipse cx="20" cy="14" rx="14" ry="8" />
        <ellipse cx="38" cy="12" rx="18" ry="10" />
        <ellipse cx="52" cy="14" rx="10" ry="6" />
      </svg>
      <svg class="cloud cloud-b" viewBox="0 0 64 24" fill="white" aria-hidden="true">
        <ellipse cx="32" cy="12" rx="22" ry="11" />
      </svg>
      <svg class="cloud cloud-c" viewBox="0 0 64 24" fill="white" aria-hidden="true">
        <ellipse cx="32" cy="12" rx="18" ry="9" />
      </svg>
    </div>

    <div class="container hero-grid">
      <!-- 左：标题与按钮 -->
      <div class="hero-text reveal">
        <div class="hero-eyebrow">
          <span class="dot"></span>
          <span>{{ greeting }} · {{ todayStr }}</span>
        </div>
        <h1 class="hero-title">
          <span class="title-main">{{ titleMain }}</span>
          <span class="title-accent">{{ titleAccent }}</span>
        </h1>
        <p class="hero-desc">{{ subtitle }}</p>
        <div class="hero-actions">
          <el-button type="primary" size="large" class="btn-pulse" @click="$emit('cta')">
            <el-icon><Reading /></el-icon>&nbsp;{{ ctaText }}
          </el-button>
          <el-button size="large" plain @click="$emit('secondary')">
            {{ secondaryText }}
          </el-button>
        </div>
        <ul class="hero-stats">
          <li v-for="s in stats" :key="s.label">
            <div class="stat-num">{{ s.value }}</div>
            <div class="stat-label">{{ s.label }}</div>
          </li>
        </ul>
      </div>

      <!-- 右：天气卡（玻璃态 + 渐变描边） -->
      <GradientBorderCard variant="mix" floating class="weather-card reveal">
        <div class="weather-head">
          <div>
            <div class="weather-eyebrow">{{ status === 'ready' ? location : 'today' }}</div>
            <div class="weather-temp">
              <template v-if="status === 'ready' || status === 'denied' || status === 'error'">
                {{ desc }} · {{ temp ?? '--' }}°
              </template>
              <template v-else-if="status === 'locating' || status === 'fetching'">
                正在获取你所在地的天气…
              </template>
              <template v-else>
                晴 · 24°
              </template>
            </div>
            <div class="weather-date">{{ todayStr }} · DemoAuthor</div>
          </div>
          <div class="weather-icon">{{ icon }}</div>
        </div>
        <div class="weather-divider"></div>
        <div class="weather-grid">
          <div v-for="s in stats" :key="s.label">
            <div class="wg-num">{{ s.value }}</div>
            <div class="wg-label">{{ s.label }}</div>
          </div>
        </div>
        <div class="weather-quote">{{ quote }}</div>
      </GradientBorderCard>
    </div>

    <!-- 底部波浪分隔 -->
    <svg class="wave" viewBox="0 0 1440 80" preserveAspectRatio="none" aria-hidden="true">
      <path d="M0,40 C240,80 480,0 720,40 C960,80 1200,0 1440,40 L1440,80 L0,80 Z" fill="#ffffff" />
    </svg>
  </section>
</template>

<script setup>
import { computed, ref, onMounted } from 'vue'
import { Reading } from '@element-plus/icons-vue'
import GradientBorderCard from './GradientBorderCard.vue'
import { useWeather } from '@/composables/useWeather'

const { temp, desc, icon, location, status } = useWeather()

defineProps({
  titleMain: { type: String, default: '草木蔓发' },
  titleAccent: { type: String, default: '春山可望' },
  subtitle: { type: String, default: '把博客做成一片晴天，慢一点，让灵魂跟上脚步。' },
  greeting: { type: String, default: '下午好' },
  ctaText: { type: String, default: '开始阅读' },
  secondaryText: { type: String, default: '关于我' },
  stats: { type: Array, default: () => [
    { label: '文章', value: 87 },
    { label: '标签', value: 12 },
    { label: '友人', value: 24 },
    { label: '读者', value: '2.4k' }
  ]},
  quote: { type: String, default: '「天气好的时候，写字也会明亮些。」' }
})

defineEmits(['cta', 'secondary'])

const rootRef = ref(null)
const sunX = ref(50)
const sunY = ref(50)

const onMouseMove = (e) => {
  if (!rootRef.value) return
  const rect = rootRef.value.getBoundingClientRect()
  sunX.value = ((e.clientX - rect.left) / rect.width) * 100
  sunY.value = ((e.clientY - rect.top) / rect.height) * 100
}

const sunStyle = computed(() => ({
  '--sx': `${sunX.value}%`,
  '--sy': `${sunY.value}%`
}))

const todayStr = new Date().toLocaleDateString('zh-CN', {
  year: 'numeric', month: '2-digit', day: '2-digit'
}).replace(/\//g, ' · ')

onMounted(() => {
  rootRef.value?.addEventListener('mousemove', onMouseMove, { passive: true })
})
</script>

<style scoped lang="scss">
.sky-hero {
  position: relative;
  overflow: hidden;
  padding: 80px 0 90px;
  background: linear-gradient(180deg, #e0f7ff 0%, #f0f9ff 50%, #ffffff 100%);
}

// 装饰：太阳 + 云
.deco {
  position: absolute;
  inset: 0;
  pointer-events: none;
  z-index: 0;
}
.sun {
  position: absolute;
  right: 8%;
  top: 12%;
  width: 140px; height: 140px;
  border-radius: 50%;
  background: radial-gradient(circle at 35% 35%, #fcd34d 0%, #fbbf24 55%, #f59e0b 100%);
  box-shadow: 0 0 60px rgba(251, 191, 36, 0.5),
              0 0 120px rgba(251, 191, 36, 0.25);
  animation: sun-pulse 5s ease-in-out infinite;
  transform: translate(calc(var(--sx, 50%) - 50% - 0px), calc(var(--sy, 50%) - 50% - 0px));
  transition: transform 1.2s cubic-bezier(0.16, 1, 0.3, 1);
}
@keyframes sun-pulse {
  0%, 100% { box-shadow: 0 0 50px rgba(251,191,36,.4), 0 0 100px rgba(251,191,36,.2); }
  50%      { box-shadow: 0 0 80px rgba(251,191,36,.65), 0 0 140px rgba(251,191,36,.35); }
}
.cloud {
  position: absolute;
  opacity: 0.85;
  filter: drop-shadow(0 6px 14px rgba(14,165,233,.12));
}
.cloud-a { top: 18%; right: 18%; width: 180px; animation: cloud-drift 14s ease-in-out infinite; }
.cloud-b { top: 56%; left:  4%;  width: 220px; animation: cloud-drift 18s ease-in-out infinite -4s; }
.cloud-c { top: 28%; left: 18%; width: 140px; animation: cloud-drift 11s ease-in-out infinite -2s; opacity: 0.6; }
@keyframes cloud-drift {
  0%, 100% { transform: translateX(0); }
  50%      { transform: translateX(30px); }
}

// 内容栅格
.hero-grid {
  position: relative;
  z-index: 1;
  display: grid;
  grid-template-columns: 1.3fr 1fr;
  gap: 56px;
  align-items: center;
}

.hero-eyebrow {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 6px 14px;
  background: rgba(255, 255, 255, 0.7);
  color: #0369a1;
  border-radius: 999px;
  font-size: 13px;
  margin-bottom: 20px;
  border: 1px solid rgba(125, 211, 252, 0.5);
  backdrop-filter: blur(6px);
}
.hero-eyebrow .dot {
  width: 6px; height: 6px; border-radius: 50%;
  background: #fbbf24;
  animation: pulse 2s ease-in-out infinite;
}
@keyframes pulse {
  0%, 100% { opacity: 1; transform: scale(1); }
  50%      { opacity: 0.5; transform: scale(0.85); }
}

.hero-title {
  font-family: var(--font-serif);
  font-size: 52px;
  line-height: 1.15;
  margin: 0 0 20px;
  font-weight: 600;
  color: var(--c-ink, #1e293b);
}
.title-main {
  display: block;
  background: linear-gradient(135deg, #0369a1 0%, #38bdf8 60%, #22d3ee 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
          background-clip: text;
}
.title-accent {
  display: block;
  margin-top: 6px;
  font-family: 'Caveat', 'Noto Serif SC', cursive;
  font-size: 60px;
  font-weight: 700;
  background: linear-gradient(135deg, #fbbf24 0%, #f59e0b 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
          background-clip: text;
}
.hero-desc {
  font-size: 16px;
  color: var(--c-ink-soft, #64748b);
  line-height: 1.85;
  max-width: 520px;
  margin: 0 0 32px;
}
.hero-actions {
  display: flex;
  gap: 14px;
  margin-bottom: 36px;
  flex-wrap: wrap;
}
.hero-stats {
  display: flex;
  gap: 32px;
  list-style: none;
  padding: 0;
  margin: 0;
}
.stat-num {
  font-family: var(--font-serif);
  font-size: 28px;
  font-weight: 600;
  color: #0ea5e9;
  line-height: 1;
}
.stat-label {
  font-size: 12px;
  color: var(--c-ink-soft, #64748b);
  margin-top: 6px;
  letter-spacing: 0.04em;
}

// 天气卡
.weather-card { padding: 26px 26px 22px; }
.weather-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
}
.weather-eyebrow {
  font-size: 11px;
  letter-spacing: 0.18em;
  text-transform: uppercase;
  color: #06b6d4;
  font-weight: 600;
}
.weather-temp {
  font-family: var(--font-serif);
  font-size: 32px;
  font-weight: 600;
  color: var(--c-ink, #1e293b);
  margin-top: 4px;
}
.weather-date {
  font-size: 12px;
  color: var(--c-ink-soft, #64748b);
  margin-top: 4px;
}
.weather-icon {
  font-size: 56px;
  animation: icon-spin 12s linear infinite;
  filter: drop-shadow(0 4px 12px rgba(251, 191, 36, 0.4));
}
@keyframes icon-spin {
  0%, 100% { transform: rotate(-6deg) scale(1); }
  50%      { transform: rotate(6deg) scale(1.08); }
}
.weather-divider {
  height: 1px;
  margin: 18px 0;
  background: linear-gradient(90deg, transparent, rgba(56,189,248,.3), transparent);
}
.weather-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 8px;
  text-align: center;
}
.wg-num {
  font-family: var(--font-serif);
  font-size: 22px;
  font-weight: 600;
  color: #0369a1;
}
.wg-label {
  font-size: 11px;
  color: var(--c-ink-soft, #64748b);
  margin-top: 2px;
}
.weather-quote {
  margin-top: 16px;
  font-size: 12px;
  color: var(--c-ink-soft, #64748b);
  font-style: italic;
  line-height: 1.6;
  padding: 10px 14px;
  background: linear-gradient(90deg, rgba(251,191,36,.08), transparent);
  border-left: 2px solid #fbbf24;
  border-radius: 0 8px 8px 0;
}

// 底部波浪
.wave {
  position: absolute;
  bottom: -1px;
  left: 0; right: 0;
  width: 100%;
  height: 60px;
  z-index: 1;
}

// reveal
.reveal {
  opacity: 0;
  transform: translateY(24px);
  transition: opacity 0.8s ease, transform 0.8s cubic-bezier(0.16, 1, 0.3, 1);
}
.reveal.visible {
  opacity: 1;
  transform: translateY(0);
}

@media (max-width: 900px) {
  .hero-grid { grid-template-columns: 1fr; }
  .weather-card { display: none; }
  .hero-title { font-size: 38px; }
  .title-accent { font-size: 44px; }
  .hero-stats { gap: 20px; }
  .stat-num { font-size: 22px; }
}
</style>