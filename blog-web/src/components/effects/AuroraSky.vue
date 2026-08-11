<template>
  <div class="aurora-sky" aria-hidden="true">
    <span class="au-band au-band-1"></span>
    <span class="au-band au-band-2"></span>
    <span class="au-band au-band-3"></span>
  </div>
</template>

<script setup>
/**
 * AuroraSky — 极光天幕
 *
 * 三层柔焦光带（天蓝 / 暖金 / 薄荷）在 hero 顶部区域缓缓水平漂移，
 * 与 CloudParallax 云层叠加，营造「极光映晴空」的纵深。
 * 纯 CSS，GPU 友好，预热主题，不抢内容。
 */
</script>

<style scoped lang="scss">
.aurora-sky {
  position: fixed;
  inset: 0;
  z-index: 0;
  pointer-events: none;
  overflow: hidden;
}

.au-band {
  position: absolute;
  left: -20%;
  width: 140%;
  height: 22vh;
  filter: blur(48px);
  will-change: transform, opacity;
}

/* 天蓝：顶层，最宽最淡，主导 */
.au-band-1 {
  top: 4vh;
  opacity: 0.38;
  background: radial-gradient(ellipse at 50% 0%, rgba(125,211,252,0.45) 0%, rgba(125,211,252,0) 75%);
  animation: aurora-drift-1 30s ease-in-out infinite;
}

/* 暖金：中层，窄一点，呼应夕照 */
.au-band-2 {
  top: 10vh;
  opacity: 0.32;
  height: 18vh;
  background: radial-gradient(ellipse at 60% 0%, rgba(251,191,36,0.3) 0%, rgba(251,191,36,0) 75%);
  animation: aurora-drift-2 35s ease-in-out infinite;
}

/* 薄荷：底层，最窄，淡绿清新 */
.au-band-3 {
  top: 16vh;
  opacity: 0.28;
  height: 14vh;
  background: radial-gradient(ellipse at 40% 0%, rgba(167,243,208,0.35) 0%, rgba(167,243,208,0) 75%);
  animation: aurora-drift-3 40s ease-in-out infinite;
}

@keyframes aurora-drift-1 {
  0%, 100% { transform: translate3d(0, 0, 0); }
  50% { transform: translate3d(3vw, 2vh, 0); }
}
@keyframes aurora-drift-2 {
  0%, 100% { transform: translate3d(0, 0, 0); }
  50% { transform: translate3d(-4vw, 3vh, 0); }
}
@keyframes aurora-drift-3 {
  0%, 100% { transform: translate3d(0, 0, 0); }
  50% { transform: translate3d(2vw, -2vh, 0); }
}

@media (prefers-reduced-motion: reduce) {
  .au-band { animation: none; }
}

@media (max-width: 768px) {
  .au-band {
    opacity: 0.18;
    animation: none;
    will-change: auto;
  }
}
</style>
