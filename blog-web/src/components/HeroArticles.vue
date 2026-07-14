<template>
  <div class="hero-articles">
    <!-- 光柱 -->
    <div class="ha-beam ha-beam-1"></div>
    <div class="ha-beam ha-beam-2"></div>

    <!-- 飘落页（8 片，CSS 驱动） -->
    <div class="ha-page ha-page-1"></div>
    <div class="ha-page ha-page-2"></div>
    <div class="ha-page ha-page-3"></div>
    <div class="ha-page ha-page-4"></div>
    <div class="ha-page ha-page-5"></div>
    <div class="ha-page ha-page-6"></div>
    <div class="ha-page ha-page-7"></div>
    <div class="ha-page ha-page-8"></div>

    <!-- 阳光圆斑 -->
    <div class="ha-sun-spot"></div>

    <div class="container ha-inner">
      <p class="ha-eyebrow">articles</p>
      <h1 class="ha-title">
        {{ title }}<span class="ha-cursor">|</span>
      </h1>
      <p class="ha-sub" v-if="subtitle">{{ subtitle }}</p>
    </div>

    <!-- 波浪（柔和长波） -->
    <svg class="wave ha-wave" viewBox="0 0 1440 120" preserveAspectRatio="none">
      <path fill="currentColor" style="color:var(--c-bg);opacity:.95"
        d="M0,60 C180,30 360,80 540,50 C720,20 900,60 1080,45 C1260,30 1380,55 1440,50 L1440,120 L0,120 Z" />
      <path fill="currentColor" style="color:#fff;opacity:.25"
        d="M0,80 C240,50 480,90 720,70 C960,50 1200,80 1440,65 L1440,120 L0,120 Z" />
    </svg>
  </div>
</template>

<script setup>
defineProps({
  title: { type: String, required: true },
  subtitle: { type: String, default: '' }
})
</script>

<style scoped lang="scss">
.hero-articles {
  position: relative;
  overflow: hidden;
  background:
    radial-gradient(ellipse 120% 80% at 50% -10%, rgba(56,189,248,0.25) 0%, transparent 60%),
    radial-gradient(ellipse 60% 50% at 20% 80%, rgba(251,191,36,0.12) 0%, transparent 50%),
    linear-gradient(175deg, #e0f7ff 0%, #f0f9ff 40%, #ffffff 100%);
  padding: 90px 0 110px;
  text-align: center;
  margin-bottom: 50px;
}

/* 柔和光柱 */
.ha-beam {
  position: absolute;
  top: -30%;
  width: 1px;
  height: 160%;
  background: linear-gradient(180deg, transparent 0%, rgba(56,189,248,0.25) 30%, rgba(56,189,248,0.15) 60%, transparent 100%);
  transform: rotate(5deg);
  pointer-events: none;
}
.ha-beam-1 { left: 25%; animation: beam-sway 12s ease-in-out infinite; }
.ha-beam-2 { left: 70%; width: 2px; animation: beam-sway 16s ease-in-out infinite -6s; }

@keyframes beam-sway {
  0%, 100% { transform: rotate(3deg) translateX(0); }
  50% { transform: rotate(7deg) translateX(20px); }
}

/* 飘落页 */
.ha-page {
  position: absolute;
  width: 20px;
  height: 26px;
  background: rgba(255,255,255,0.55);
  border-radius: 2px 8px 2px 8px;
  box-shadow: 0 2px 8px rgba(56,189,248,0.1);
  pointer-events: none;
  animation: page-fall linear infinite;
}
@for $i from 1 through 8 {
  .ha-page-#{$i} {
    left: #{10 + ($i * 10)}%;
    animation-duration: #{8 + ($i * 1.5)}s;
    animation-delay: #{-($i * 2)}s;
    opacity: #{0.3 + ($i % 4) * 0.15};
    width: #{16 + ($i % 5) * 3}px;
    height: #{20 + ($i % 4) * 4}px;
  }
}

@keyframes page-fall {
  0% { transform: translateY(-20px) rotate(0deg) scale(1); opacity: 0; }
  10% { opacity: 0.7; }
  80% { opacity: 0.4; }
  100% { transform: translateY(400px) rotate(360deg) scale(0.3); opacity: 0; }
}

/* 阳光圆斑 */
.ha-sun-spot {
  position: absolute;
  top: 12%;
  right: 10%;
  width: 120px;
  height: 120px;
  border-radius: 50%;
  background: radial-gradient(circle, rgba(251,191,36,0.2) 0%, rgba(251,191,36,0.05) 50%, transparent 70%);
  filter: blur(15px);
  pointer-events: none;
  animation: float 8s ease-in-out infinite;
}

@keyframes float {
  0%, 100% { transform: translateY(0) scale(1); }
  50% { transform: translateY(-15px) scale(1.1); }
}

/* 内容区 */
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
  line-height: 1.2;
}
.ha-cursor {
  display: inline-block;
  color: var(--c-botany-500);
  font-weight: 400;
  animation: cursor-blink 1.2s step-end infinite;
}
@keyframes cursor-blink {
  0%, 100% { opacity: 1; }
  50% { opacity: 0; }
}
.ha-sub {
  font-size: 16px;
  color: var(--c-ink-soft);
  margin: 0;
  font-weight: 300;
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
  .hero-articles { padding: 60px 0 70px; }
  .ha-title { font-size: 30px; }
}
</style>
