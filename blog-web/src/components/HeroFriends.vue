<template>
  <div class="hero-friends">
    <!-- 星座连线（CSS-only：伪元素 + border） -->
    <div class="hf-constellation" aria-hidden="true">
      <span class="hf-dot" v-for="d in dots" :key="d.id"
        :style="{ left: d.x + '%', top: d.y + '%', animationDelay: d.delay + 's' }"
      ></span>
    </div>

    <!-- 暖色光晕 -->
    <div class="hf-glow hf-glow-1"></div>
    <div class="hf-glow hf-glow-2"></div>

    <div class="container hf-inner">
      <p class="hf-eyebrow">friends</p>
      <h1 class="hf-title">
        {{ title }}
        <span class="hf-connect-icon">
          <svg width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
            <circle cx="7" cy="6" r="2.5"/>
            <circle cx="17" cy="6" r="2.5"/>
            <circle cx="12" cy="17" r="2.5"/>
            <line x1="9" y1="7.5" x2="11" y2="15"/>
            <line x1="15" y1="7.5" x2="13" y2="15"/>
            <line x1="10.5" y1="18.5" x2="13.5" y2="18.5"/>
          </svg>
        </span>
      </h1>
      <p class="hf-sub" v-if="subtitle">{{ subtitle }}</p>
    </div>

    <!-- 双弧波浪 -->
    <svg class="wave hf-wave" viewBox="0 0 1440 120" preserveAspectRatio="none">
      <path fill="currentColor" style="color:var(--c-bg);opacity:.95"
        d="M0,45 C120,80 300,10 480,50 C660,90 780,20 960,55 C1140,85 1320,15 1440,48 L1440,120 L0,120 Z" />
      <path fill="currentColor" style="color:rgba(255,255,255,.35)"
        d="M0,70 C200,105 400,35 600,65 C800,95 1000,30 1200,60 C1350,80 1420,55 1440,60 L1440,120 L0,120 Z" />
    </svg>
  </div>
</template>

<script setup>
defineProps({
  title: { type: String, required: true },
  subtitle: { type: String, default: '' }
})

// 星座节点位置（8 个固定 + 随机抖动感）
const dots = Array.from({ length: 12 }, (_, i) => ({
  id: i,
  x: 5 + (i * 85) / 11,
  y: 10 + ((i * 7 + 3) % 13) * 6,
  delay: i * 0.6
}))
</script>

<style scoped lang="scss">
.hero-friends {
  position: relative;
  overflow: hidden;
  background:
    radial-gradient(ellipse 80% 60% at 60% 30%, rgba(56,189,248,0.18) 0%, transparent 60%),
    radial-gradient(ellipse 50% 40% at 30% 70%, rgba(251,191,36,0.15) 0%, transparent 55%),
    linear-gradient(170deg, #f0f9ff 0%, #e0f7ff 35%, #fffbeb 70%, #ffffff 100%);
  padding: 90px 0 110px;
  text-align: center;
  margin-bottom: 50px;
}

/* 光晕 */
.hf-glow {
  position: absolute;
  border-radius: 50%;
  filter: blur(60px);
  pointer-events: none;
}
.hf-glow-1 {
  top: -10%;
  right: 5%;
  width: 220px;
  height: 220px;
  background: rgba(56,189,248,0.15);
  animation: float 10s ease-in-out infinite;
}
.hf-glow-2 {
  bottom: 10%;
  left: 8%;
  width: 180px;
  height: 180px;
  background: rgba(251,191,36,0.12);
  animation: float 12s ease-in-out infinite -4s;
}

/* 星座节点 */
.hf-constellation {
  position: absolute;
  inset: 0;
  z-index: 0;
  pointer-events: none;
  /* 连接线用伪元素画在容器上 */
  &::before {
    content: '';
    position: absolute;
    inset: 0;
    background:
      /* 水平连接 */
      repeating-linear-gradient(90deg,
        transparent 0px, transparent 144px,
        rgba(56,189,248,0.06) 144px, rgba(56,189,248,0.06) 145px
      );
  }
}
.hf-dot {
  position: absolute;
  width: 4px;
  height: 4px;
  border-radius: 50%;
  background: var(--c-botany-500);
  box-shadow: 0 0 6px rgba(56,189,248,0.4), 0 0 12px rgba(56,189,248,0.15);
  animation: pulse-dot 3s ease-in-out infinite;
}
@keyframes pulse-dot {
  0%, 100% { transform: scale(1); opacity: 0.5; }
  50% { transform: scale(2.2); opacity: 1; }
}
@keyframes float {
  0%, 100% { transform: translateY(0) scale(1); }
  50% { transform: translateY(-15px) scale(1.1); }
}

/* 内容 */
.hf-inner {
  position: relative;
  z-index: 2;
}
.hf-eyebrow {
  font-size: 11px;
  letter-spacing: 0.25em;
  text-transform: uppercase;
  color: var(--c-autumn-500);
  margin: 0 0 12px;
  font-weight: 500;
}
.hf-title {
  font-family: var(--font-serif);
  font-size: 44px;
  font-weight: 700;
  color: var(--c-ink);
  margin: 0 0 16px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
}
.hf-connect-icon {
  display: inline-flex;
  color: var(--c-autumn-500);
  animation: float 4s ease-in-out infinite;
  svg { width: 28px; height: 28px; }
}
.hf-sub {
  font-size: 16px;
  color: var(--c-ink-soft);
  margin: 0;
  font-weight: 300;
}

.hf-wave {
  position: absolute;
  bottom: -2px;
  left: 0;
  right: 0;
  z-index: 1;
  pointer-events: none;
}

@media (max-width: 768px) {
  .hero-friends { padding: 60px 0 70px; }
  .hf-title { font-size: 30px; }
}
</style>
