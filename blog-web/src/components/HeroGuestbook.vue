<template>
  <div class="hero-guestbook">
    <!-- 气泡 -->
    <div class="hg-bubble hg-bubble-1"></div>
    <div class="hg-bubble hg-bubble-2"></div>
    <div class="hg-bubble hg-bubble-3"></div>
    <div class="hg-bubble hg-bubble-4"></div>
    <div class="hg-bubble hg-bubble-5"></div>

    <!-- 涟漪圆环 -->
    <div class="hg-ripple hg-ripple-1"></div>
    <div class="hg-ripple hg-ripple-2"></div>

    <!-- 暖光 -->
    <div class="hg-warm"></div>

    <div class="container hg-inner">
      <p class="hg-eyebrow">guestbook</p>
      <h1 class="hg-title">{{ title }}</h1>
      <p class="hg-sub" v-if="subtitle">{{ subtitle }}</p>
      <div class="hg-stats-row" v-if="stats">
        <div class="hg-stat" v-for="s in stats" :key="s.label">
          <span class="hg-stat-num">{{ s.value }}</span>
          <span class="hg-stat-label">{{ s.label }}</span>
        </div>
      </div>
    </div>

    <!-- 深弧波浪 -->
    <svg class="wave hg-wave" viewBox="0 0 1440 120" preserveAspectRatio="none">
      <path fill="currentColor" style="color:var(--c-bg);opacity:.95"
        d="M0,40 C120,80 300,0 480,60 C660,120 840,20 1020,50 C1200,80 1350,20 1440,45 L1440,120 L0,120 Z" />
      <path fill="currentColor" style="color:rgba(255,255,255,.28)"
        d="M0,65 C200,20 440,75 620,40 C840,0 1040,60 1240,35 C1360,20 1420,55 1440,50 L1440,120 L0,120 Z" />
    </svg>
  </div>
</template>

<script setup>
defineProps({
  title: { type: String, required: true },
  subtitle: { type: String, default: '' },
  stats: { type: Array, default: null }  // [{ label, value }]
})
</script>

<style scoped lang="scss">
.hero-guestbook {
  position: relative;
  overflow: hidden;
  background:
    radial-gradient(ellipse at 50% 10%, rgba(255,255,255,0.4) 0%, transparent 40%),
    radial-gradient(ellipse at 30% 70%, rgba(56,189,248,0.35) 0%, transparent 45%),
    radial-gradient(ellipse at 70% 30%, rgba(14,165,233,0.28) 0%, transparent 50%),
    radial-gradient(ellipse at 80% 80%, rgba(34,211,238,0.2) 0%, transparent 40%),
    linear-gradient(135deg, #075985 0%, #0369a1 25%, #0ea5e9 55%, #38bdf8 80%, #7dd3fc 100%);
  color: #fff;
  padding: 90px 0 115px;
  text-align: center;
  margin-bottom: 50px;
}

/* 气泡 */
.hg-bubble {
  position: absolute;
  border-radius: 50%;
  background: radial-gradient(circle at 35% 30%, rgba(255,255,255,0.35) 0%, rgba(255,255,255,0.08) 70%, transparent 100%);
  border: 1px solid rgba(255,255,255,0.15);
  pointer-events: none;
  animation: bubble-rise linear infinite;
}
@for $i from 1 through 5 {
  .hg-bubble-#{$i} {
    left: #{5 + ($i * 18)}%;
    width: #{24 + ($i % 3) * 12}px;
    height: #{24 + ($i % 3) * 12}px;
    animation-duration: #{6 + ($i * 1.8)}s;
    animation-delay: #{($i - 1) * 1.2}s;
    opacity: #{0.3 + ($i % 3) * 0.2};
  }
}
@keyframes bubble-rise {
  0% { transform: translateY(120%) scale(0.5); opacity: 0; }
  15% { opacity: 0.6; }
  85% { opacity: 0.3; }
  100% { transform: translateY(-120%) scale(1.2); opacity: 0; }
}

/* 涟漪 */
.hg-ripple {
  position: absolute;
  border-radius: 50%;
  border: 1.5px solid rgba(255,255,255,0.18);
  pointer-events: none;
  animation: ripple-out 4s ease-out infinite;
}
.hg-ripple-1 {
  width: 60px; height: 60px;
  top: 50%; left: 50%;
  transform: translate(-50%, -50%);
}
.hg-ripple-2 {
  width: 60px; height: 60px;
  top: 50%; left: 50%;
  transform: translate(-50%, -50%);
  animation-delay: -2s;
}
@keyframes ripple-out {
  0% { width: 60px; height: 60px; opacity: 0.5; }
  100% { width: 500px; height: 500px; opacity: 0; }
}

/* 暖光 */
.hg-warm {
  position: absolute;
  top: 10%;
  right: 15%;
  width: 180px;
  height: 180px;
  border-radius: 50%;
  background: radial-gradient(circle, rgba(251,191,36,0.25) 0%, transparent 70%);
  filter: blur(30px);
  pointer-events: none;
  animation: float 8s ease-in-out infinite;
}
@keyframes float {
  0%, 100% { transform: translateY(0) scale(1); }
  50% { transform: translateY(-15px) scale(1.1); }
}

/* 内容 */
.hg-inner {
  position: relative;
  z-index: 2;
}
.hg-eyebrow {
  font-size: 11px;
  letter-spacing: 0.25em;
  text-transform: uppercase;
  color: rgba(255,255,255,0.8);
  margin: 0 0 12px;
  font-weight: 500;
}
.hg-title {
  font-family: var(--font-serif);
  font-size: 44px;
  font-weight: 700;
  margin: 0 0 16px;
  text-shadow: 0 2px 20px rgba(2, 132, 199, 0.3), 0 0 30px rgba(255,255,255,0.15);
}
.hg-sub {
  font-size: 16px;
  color: rgba(255,255,255,0.85);
  margin: 0 0 28px;
  font-weight: 300;
}

/* 统计数字 */
.hg-stats-row {
  display: inline-flex;
  gap: 36px;
  align-items: center;
}
.hg-stat { text-align: center; }
.hg-stat-num {
  display: block;
  font-size: 28px;
  font-weight: 700;
  font-family: var(--font-serif);
  background: linear-gradient(135deg, #fff, rgba(255,255,255,0.85));
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
}
.hg-stat-label {
  font-size: 12px;
  color: rgba(255,255,255,0.7);
  letter-spacing: 0.08em;
}

.hg-wave {
  position: absolute;
  bottom: -2px;
  left: 0;
  right: 0;
  z-index: 1;
  pointer-events: none;
}

@media (max-width: 768px) {
  .hero-guestbook { padding: 60px 0 75px; }
  .hg-title { font-size: 30px; }
  .hg-stats-row { gap: 24px; }
  .hg-stat-num { font-size: 22px; }
}
</style>
