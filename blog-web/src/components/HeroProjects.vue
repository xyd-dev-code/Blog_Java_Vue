<template>
  <div class="hero-projects">
    <!-- 网格背景 -->
    <div class="hp-grid"></div>

    <!-- 扫描线 -->
    <div class="hp-scan-line"></div>

    <!-- 代码粒子（CSS-only 方块） -->
    <div class="hp-particles" aria-hidden="true">
      <span v-for="p in particles" :key="p.id"
        class="hp-particle"
        :style="{
          left: p.x + '%',
          animationDelay: p.delay + 's',
          animationDuration: p.dur + 's',
          width: p.s + 'px',
          height: p.s + 'px'
        }"
      ></span>
    </div>

    <!-- 光柱 -->
    <div class="hp-beam"></div>

    <div class="container hp-inner">
      <p class="hp-eyebrow">projects</p>
      <h1 class="hp-title">
        {{ title }}<span class="hp-cursor">█</span>
      </h1>
      <p class="hp-sub" v-if="subtitle">{{ subtitle }}</p>
    </div>

    <!-- 锯齿波浪（技术感） -->
    <svg class="wave hp-wave" viewBox="0 0 1440 120" preserveAspectRatio="none">
      <path fill="currentColor" style="color:var(--c-bg);opacity:.95"
        d="M0,60 L60,45 L120,55 L180,35 L240,50 L300,30 L360,48 L420,25 L480,42 L540,20 L600,40 L660,22 L720,38 L780,18 L840,36 L900,15 L960,32 L1020,12 L1080,28 L1140,10 L1200,26 L1260,8 L1320,24 L1380,6 L1440,22 L1440,120 L0,120 Z" />
      <path fill="currentColor" style="color:rgba(255,255,255,.25)"
        d="M0,78 L72,65 L144,72 L216,55 L288,68 L360,50 L432,62 L504,45 L576,58 L648,40 L720,54 L792,38 L864,52 L936,34 L1008,48 L1080,30 L1152,44 L1224,28 L1296,42 L1368,24 L1440,40 L1440,120 L0,120 Z" />
    </svg>
  </div>
</template>

<script setup>
defineProps({
  title: { type: String, required: true },
  subtitle: { type: String, default: '' }
})

// 代码粒子方块（位置、尺寸、延时随机）
const particles = Array.from({ length: 14 }, (_, i) => ({
  id: i,
  x: 5 + (i * 85) / 14,
  delay: i * 0.7,
  dur: 4 + (i % 3) * 2,
  s: 3 + (i % 4)
}))
</script>

<style scoped lang="scss">
.hero-projects {
  position: relative;
  overflow: hidden;
  background:
    radial-gradient(ellipse 70% 50% at 50% 20%, rgba(56,189,248,0.2) 0%, transparent 60%),
    linear-gradient(180deg, #f0f9ff 0%, #e0f7ff 30%, #ffffff 100%);
  padding: 50px 0 70px;
  text-align: center;
  margin-bottom: 50px;
}

/* 网格背景 */
.hp-grid {
  position: absolute;
  inset: 0;
  background-image:
    linear-gradient(rgba(56,189,248,0.06) 1px, transparent 1px),
    linear-gradient(90deg, rgba(56,189,248,0.06) 1px, transparent 1px);
  background-size: 36px 36px;
  mask-image: radial-gradient(ellipse 70% 70% at 50% 30%, #000 0%, transparent 100%);
  pointer-events: none;
}

/* 扫描线 */
.hp-scan-line {
  position: absolute;
  left: 0;
  right: 0;
  height: 1px;
  background: linear-gradient(90deg, transparent, rgba(56,189,248,0.5), transparent);
  animation: hp-scan 4s ease-in-out infinite;
  pointer-events: none;
}
@keyframes hp-scan {
  0%, 100% { top: 10%; opacity: 0; }
  25% { opacity: 0.7; }
  50% { top: 65%; opacity: 0.3; }
  75% { opacity: 0.6; }
}

/* 代码粒子 */
.hp-particle {
  position: absolute;
  top: -10px;
  background: var(--c-botany-500);
  opacity: 0;
  border-radius: 1px;
  pointer-events: none;
  animation: hp-fall ease-in-out infinite;
}
@keyframes hp-fall {
  0% { transform: translateY(0) rotate(0deg); opacity: 0; }
  10% { opacity: 0.5; }
  90% { opacity: 0.2; }
  100% { transform: translateY(500px) rotate(180deg); opacity: 0; }
}

/* 柔光柱 */
.hp-beam {
  position: absolute;
  top: -20%;
  left: 40%;
  width: 2px;
  height: 140%;
  background: linear-gradient(180deg, transparent 0%, rgba(56,189,248,0.2) 40%, rgba(56,189,248,0.1) 70%, transparent 100%);
  transform: rotate(8deg);
  pointer-events: none;
  animation: hp-beam-sway 12s ease-in-out infinite;
}
@keyframes hp-beam-sway {
  0%, 100% { transform: rotate(6deg) translateX(0); }
  50% { transform: rotate(10deg) translateX(30px); }
}

/* 内容 */
.hp-inner {
  position: relative;
  z-index: 2;
}
.hp-eyebrow {
  display: inline-block;
  font-family: 'Consolas', 'Monaco', 'Courier New', monospace;
  font-size: 12px;
  letter-spacing: 0.15em;
  color: var(--c-botany-500);
  margin: 0 0 12px;
  padding: 3px 12px;
  background: rgba(56,189,248,0.08);
  border: 1px solid rgba(56,189,248,0.2);
  border-radius: 4px;
  font-weight: 500;
}
.hp-title {
  font-family: var(--font-serif);
  font-size: 36px;
  font-weight: 700;
  color: var(--c-ink);
  margin: 0 0 12px;
}
.hp-cursor {
  color: var(--c-botany-500);
  font-weight: 300;
  animation: cursor-blink 1.2s step-end infinite;
}
@keyframes cursor-blink {
  0%, 100% { opacity: 1; }
  50% { opacity: 0; }
}
.hp-sub {
  font-size: 16px;
  color: var(--c-ink-soft);
  margin: 0;
  font-weight: 300;
}

.hp-wave {
  position: absolute;
  bottom: -2px;
  left: 0;
  right: 0;
  z-index: 1;
  pointer-events: none;
}

@media (max-width: 768px) {
  .hero-projects { padding: 36px 0 50px; }
  .hp-title { font-size: 26px; }
}
</style>
