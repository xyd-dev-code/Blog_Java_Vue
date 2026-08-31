<template>
  <div class="light-pool" aria-hidden="true">
    <span class="lp-blob lp-blob-1"></span>
    <span class="lp-blob lp-blob-2"></span>
    <span class="lp-blob lp-blob-3"></span>
    <span class="lp-blob lp-blob-4"></span>
    <span class="lp-blob lp-blob-5"></span>
  </div>
</template>

<script setup>
/**
 * LightPool — 底部光池
 *
 * 页面底部约 200px 高的区域放置 5 个缓缓流动的椭圆光斑，
 * 天蓝→暖金→天蓝错位叠加，像傍晚湖面倒影。
 * 极低透明度 (0.06-0.16)，不抢内容，只在滚到底部时隐约可见。
 * 纯 CSS，GPU 友好。
 */
</script>

<style scoped lang="scss">
.light-pool {
  position: fixed;
  inset: 0;
  z-index: 0;
  pointer-events: none;
  overflow: hidden;
}

.lp-blob {
  position: absolute;
  border-radius: 50%;
  filter: blur(36px);
  will-change: transform, opacity;
}

/* 天蓝大光斑：左，最大，最淡 */
.lp-blob-1 {
  width: 50vw;
  height: 28vh;
  left: -8vw;
  bottom: -12vh;
  opacity: 0.14;
  background: radial-gradient(ellipse, rgba(var(--theme-primary-light-rgb), 0.38) 0%, rgba(var(--theme-primary-light-rgb), 0) 72%);
  animation: pool-drift-1 24s ease-in-out infinite;
}

/* 暖金光斑：中右，呼应夕阳 */
.lp-blob-2 {
  width: 38vw;
  height: 22vh;
  right: 6vw;
  bottom: -8vh;
  opacity: 0.11;
  background: radial-gradient(ellipse, rgba(var(--theme-accent-rgb), 0.26) 0%, rgba(var(--theme-accent-rgb), 0) 72%);
  animation: pool-drift-2 28s ease-in-out infinite;
}

/* 天蓝中光斑：中间，过渡 */
.lp-blob-3 {
  width: 44vw;
  height: 24vh;
  left: 22vw;
  bottom: -16vh;
  opacity: 0.12;
  background: radial-gradient(ellipse, rgba(var(--theme-primary-rgb), 0.3) 0%, rgba(var(--theme-primary-rgb), 0) 72%);
  animation: pool-drift-3 22s ease-in-out infinite;
}

/* 薄荷淡光斑：右，轻绿补色 */
.lp-blob-4 {
  width: 32vw;
  height: 18vh;
  right: -4vw;
  bottom: -14vh;
  opacity: 0.09;
  background: radial-gradient(ellipse, rgba(var(--theme-mint-rgb), 0.28) 0%, rgba(var(--theme-mint-rgb), 0) 72%);
  animation: pool-drift-1 26s ease-in-out infinite;
}

/* 暖金小光斑：左中，活泼 */
.lp-blob-5 {
  width: 28vw;
  height: 14vh;
  left: 4vw;
  bottom: -10vh;
  opacity: 0.1;
  background: radial-gradient(ellipse, rgba(var(--theme-accent-rgb), 0.22) 0%, rgba(var(--theme-accent-rgb), 0) 72%);
  animation: pool-drift-2 30s ease-in-out infinite;
}

@keyframes pool-drift-1 {
  0%, 100% { transform: translate3d(0, 0, 0) scale(1); }
  50% { transform: translate3d(4vw, -3vh, 0) scale(1.15); }
}
@keyframes pool-drift-2 {
  0%, 100% { transform: translate3d(0, 0, 0) scale(1); }
  50% { transform: translate3d(-3vw, -2vh, 0) scale(1.12); }
}
@keyframes pool-drift-3 {
  0%, 100% { transform: translate3d(0, 0, 0) scale(1); }
  50% { transform: translate3d(2vw, -4vh, 0) scale(1.18); }
}

@media (prefers-reduced-motion: reduce) {
  .lp-blob { animation: none; }
}

@media (max-width: 768px) {
  .lp-blob {
    opacity: 0.05;
    animation: none;
    will-change: auto;
  }
}
</style>
