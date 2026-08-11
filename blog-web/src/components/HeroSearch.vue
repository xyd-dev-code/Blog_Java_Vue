<template>
  <div class="hero-search">
    <GodRays :origin-y="62" />

    <!-- 中心：放大镜水印 + 声纳涟漪（搜索主题的标志性视觉，与 HeroArticles 的飘落页区分） -->
    <div class="hs-stage" aria-hidden="true">
      <svg class="hs-lens" viewBox="0 0 100 100" fill="none" stroke="currentColor"
        stroke-width="2.4" stroke-linecap="round">
        <circle cx="42" cy="42" r="26" />
        <line x1="61" y1="61" x2="82" y2="82" />
      </svg>
      <span class="hs-ring hs-ring-1"></span>
      <span class="hs-ring hs-ring-2"></span>
      <span class="hs-ring hs-ring-3"></span>
    </div>

    <!-- 被「聚焦」的暖色微粒 -->
    <div class="hs-sparkles" aria-hidden="true">
      <span
        v-for="i in 4" :key="i" class="hs-sparkle"
        :style="{ left: (12 + i * 20) + '%', top: (18 + (i % 2) * 52) + '%', animationDelay: (i * 0.8) + 's' }"
      ></span>
    </div>

    <div class="container hs-inner">
      <p class="hs-eyebrow">search</p>
      <h1 class="hs-title">{{ title }}</h1>
      <p class="hs-sub" v-if="subtitle">{{ subtitle }}</p>
    </div>
  </div>
</template>

<script setup>
import GodRays from '@/components/effects/GodRays.vue'
defineProps({
  title: { type: String, required: true },
  subtitle: { type: String, default: '' }
})
</script>

<style scoped lang="scss">
.hero-search {
  position: relative;
  padding: 48px 0 12px;
  text-align: center;
  margin-bottom: 0;
  display: flex;
  flex-direction: column;
  justify-content: center;
  background:
    radial-gradient(ellipse 110% 70% at 50% -5%, rgba(56, 189, 248, 0.06) 0%, transparent 62%);
}

/* 放大镜 + 涟漪舞台（整体微浮动） */
.hs-stage {
  position: absolute;
  left: 50%;
  top: 40%;
  width: 260px;
  height: 260px;
  transform: translate(-50%, -50%);
  pointer-events: none;
  z-index: 1;
  animation: stage-bob 7s ease-in-out infinite;
}
.hs-lens {
  position: absolute;
  left: 50%; top: 50%;
  width: 132px; height: 132px;
  margin-left: -66px; margin-top: -66px;
  color: var(--c-botany-500);
  opacity: 0.11;
  filter: drop-shadow(0 0 26px rgba(56, 189, 248, 0.28));
}
.hs-ring {
  position: absolute;
  left: 50%; top: 50%;
  width: 150px; height: 150px;
  margin-left: -75px; margin-top: -75px;
  border-radius: 50%;
  border: 1.5px solid rgba(56, 189, 248, 0.28);
  opacity: 0;
  animation: ring-expand 4s ease-out infinite;
}
.hs-ring-2 { animation-delay: 1.33s; }
.hs-ring-3 { animation-delay: 2.66s; }
@keyframes stage-bob {
  0%, 100% { transform: translate(-50%, -50%); }
  50%      { transform: translate(-50%, -54%); }
}
@keyframes ring-expand {
  0%   { transform: scale(0.22); opacity: 0.6; border-width: 2px; }
  70%  { opacity: 0.16; }
  100% { transform: scale(1.55); opacity: 0; border-width: 1px; }
}

/* 暖色微粒 */
.hs-sparkles {
  position: absolute;
  inset: 0;
  pointer-events: none;
  z-index: 1;
}
.hs-sparkle {
  position: absolute;
  width: 4px; height: 4px;
  border-radius: 50%;
  background: radial-gradient(circle, #fbbf24 0%, transparent 70%);
  opacity: 0;
  animation: sparkle-drift 6s ease-in-out infinite;
}
@keyframes sparkle-drift {
  0%, 100% { transform: translate(0, 0) scale(0.6); opacity: 0; }
  20%      { opacity: 0.85; }
  50%      { transform: translate(-26px, -18px) scale(1); opacity: 0.55; }
  80%      { opacity: 0.25; }
}

/* 文字层（置于装饰之上） */
.hs-inner { position: relative; z-index: 3; }
.hs-eyebrow {
  font-size: 12px;
  letter-spacing: 0.28em;
  text-transform: uppercase;
  color: var(--c-botany-500);
  margin: 0 0 12px;
  font-weight: 500;
}
.hs-title {
  font-family: var(--font-serif);
  font-size: clamp(30px, 4vw, 42px);
  font-weight: 700;
  color: var(--c-ink);
  margin: 0 0 10px;
  line-height: 1.2;
}
.hs-sub {
  font-size: 16px;
  color: var(--c-ink-soft);
  margin: 0;
  font-weight: 300;
}

@media (max-width: 768px) {
  .hero-search { padding: 40px 0 12px; }
  .hs-title { font-size: 28px; }
  .hs-stage { width: 200px; height: 200px; top: 42%; }
  .hs-lens { width: 100px; height: 100px; margin-left: -50px; margin-top: -50px; }
  .hs-ring { width: 120px; height: 120px; margin-left: -60px; margin-top: -60px; }
}
</style>