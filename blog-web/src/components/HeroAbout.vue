<template>
  <div class="hero-about">
    <!-- 光屏扫过 -->
    <div class="ha-light-scan"></div>

    <!-- 柔光背景层 -->
    <div class="ha-aura ha-aura-1"></div>
    <div class="ha-aura ha-aura-2"></div>

    <!-- 时间轴标记线 -->
    <div class="ha-timeline-marks" aria-hidden="true">
      <span v-for="i in 6" :key="i" class="ha-tl-mark" :style="{ animationDelay: i * 0.8 + 's' }"></span>
    </div>

    <div class="container ha-inner">
      <p class="ha-eyebrow">about</p>
      <h1 class="ha-title">{{ title }}</h1>
      <p class="ha-sub" v-if="subtitle">{{ subtitle }}</p>
      <div class="ha-tagline">
        <span class="ha-tagline-word" v-for="(w, i) in taglineWords" :key="i"
          :style="{ animationDelay: (0.6 + i * 0.15) + 's' }"
        >{{ w }}</span>
      </div>
    </div>

    <!-- 波浪（单弧深波） -->
    <svg class="wave ha-wave" viewBox="0 0 1440 120" preserveAspectRatio="none">
      <path fill="currentColor" style="color:var(--c-bg);opacity:.95"
        d="M0,90 C360,20 720,100 1080,35 C1200,15 1360,50 1440,60 L1440,120 L0,120 Z" />
      <path fill="currentColor" style="color:rgba(255,255,255,.3)"
        d="M0,105 C300,60 600,110 900,70 C1200,30 1360,80 1440,85 L1440,120 L0,120 Z" />
    </svg>
  </div>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  title: { type: String, required: true },
  subtitle: { type: String, default: '' }
})

// 从副标题中拆出词作逐字动画
const taglineWords = computed(() => {
  if (!props.subtitle) return []
  return props.subtitle.replace(/[，,。.、\s]+/g, ' ').split(/\s+/).filter(Boolean)
})
</script>

<style scoped lang="scss">
.hero-about {
  position: relative;
  overflow: hidden;
  background:
    radial-gradient(ellipse 100% 70% at 50% 10%, rgba(56,189,248,0.22) 0%, transparent 55%),
    radial-gradient(ellipse 60% 50% at 80% 70%, rgba(251,191,36,0.1) 0%, transparent 45%),
    linear-gradient(180deg, #f0f9ff 0%, #ffffff 60%);
  padding: 100px 0 120px;
  text-align: center;
  margin-bottom: 50px;
}

/* 光屏扫描 */
.ha-light-scan {
  position: absolute;
  top: 0;
  left: -50%;
  width: 200%;
  height: 100%;
  background: linear-gradient(105deg,
    transparent 0%, transparent 40%,
    rgba(255,255,255,0.3) 45%, rgba(255,255,255,0.15) 48%,
    transparent 50%, transparent 100%);
  z-index: 1;
  pointer-events: none;
  animation: scan 8s ease-in-out infinite;
}
@keyframes scan {
  0%, 100% { transform: translateX(-10%); }
  50% { transform: translateX(10%); }
}

/* 柔光 */
.ha-aura {
  position: absolute;
  border-radius: 50%;
  filter: blur(80px);
  pointer-events: none;
}
.ha-aura-1 {
  top: -20%;
  left: 30%;
  width: 300px;
  height: 300px;
  background: rgba(56,189,248,0.12);
  animation: float 10s ease-in-out infinite;
}
.ha-aura-2 {
  bottom: 0;
  right: 10%;
  width: 250px;
  height: 250px;
  background: rgba(251,191,36,0.1);
  animation: float 14s ease-in-out infinite -5s;
}

/* 时间轴标记线 */
.ha-timeline-marks {
  position: absolute;
  left: 8%;
  top: 15%;
  bottom: 40%;
  width: 2px;
  background: rgba(56,189,248,0.08);
  pointer-events: none;
}
.ha-tl-mark {
  position: absolute;
  left: -4px;
  width: 10px;
  height: 10px;
  border-radius: 50%;
  background: var(--c-botany-500);
  box-shadow: 0 0 8px rgba(56,189,248,0.3);
  animation: tl-fade 6s ease-in-out infinite;
}
@for $i from 1 through 6 {
  .ha-tl-mark:nth-child(#{$i}) {
    top: #{10 + ($i - 1) * 16}%;
  }
}
@keyframes tl-fade {
  0%, 100% { opacity: 0.2; transform: scale(0.8); }
  50% { opacity: 0.7; transform: scale(1.3); }
}

/* 内容 */
.ha-inner {
  position: relative;
  z-index: 2;
}
.ha-eyebrow {
  font-size: 11px;
  letter-spacing: 0.25em;
  text-transform: uppercase;
  color: var(--c-botany-500);
  margin: 0 0 12px;
  font-weight: 500;
}
.ha-title {
  font-family: var(--font-serif);
  font-size: 44px;
  font-weight: 700;
  color: var(--c-ink);
  margin: 0 0 16px;
}
.ha-sub {
  font-size: 16px;
  color: var(--c-ink-soft);
  margin: 0 0 20px;
  font-weight: 300;
}
.ha-tagline {
  display: flex;
  justify-content: center;
  gap: 8px;
  flex-wrap: wrap;
}
.ha-tagline-word {
  display: inline-block;
  font-size: 14px;
  color: var(--c-botany-500);
  opacity: 0;
  animation: word-reveal 0.6s ease-out forwards;
}
@keyframes word-reveal {
  from { opacity: 0; transform: translateY(8px); }
  to { opacity: 0.7; transform: translateY(0); }
}

@keyframes float {
  0%, 100% { transform: translateY(0) scale(1); }
  50% { transform: translateY(-15px) scale(1.1); }
}

.ha-wave {
  position: absolute;
  bottom: -2px;
  left: 0;
  right: 0;
  z-index: 1;
  pointer-events: none;
}

@media (max-width: 768px) {
  .hero-about { padding: 70px 0 80px; }
  .ha-title { font-size: 30px; }
  .ha-timeline-marks { display: none; }
}
</style>
