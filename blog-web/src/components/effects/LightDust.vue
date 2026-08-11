<template>
  <div class="light-dust" aria-hidden="true">
    <span v-for="n in count" :key="n" class="dust-speck" :style="specks[n - 1]" />
  </div>
</template>

<script setup>
/**
 * LightDust — 光尘漫舞
 *
 * 全屏 12-15 个极光点缓慢上升飘移，像阳光穿透树叶缝隙的微尘。
 * 天蓝 / 暖金 / 薄荷三色间歇闪烁，纯 CSS animation，零 JS 轮询。
 * 密度刻意压低 (不超 15)，不沦为密集粒子场。
 */
defineProps({ count: { type: Number, default: 14 } })

const hues = ['rgba(125,211,252,', 'rgba(251,191,36,', 'rgba(167,243,208,', 'rgba(56,189,248,']
const rand = (i, mod) => ((i * 173 + 41) % (mod * 10)) / 10
const specks = Array.from({ length: 16 }, (_, i) => {
  const hue = hues[i % hues.length]
  const left = (i * 37 + 7) % 100
  const delay = (i * 2.3 + 0.7).toFixed(1)
  const dur = (8 + (i * 1.7) % 9).toFixed(1)
  const size = (3 + (i % 3) * 1.5).toFixed(1)
  const peak = (0.25 + (i % 5) * 0.12).toFixed(2)
  return {
    left: left + '%',
    animationDelay: delay + 's',
    animationDuration: dur + 's',
    width: size + 'px',
    height: size + 'px',
    '--dust-color': hue + peak + ')',
    '--dust-peak': hue + (peak * 1.6).toFixed(2) + ')',
  }
})
</script>

<style scoped lang="scss">
.light-dust {
  position: fixed;
  inset: 0;
  z-index: 0;
  pointer-events: none;
  overflow: hidden;
}

.dust-speck {
  position: absolute;
  top: 110%;
  border-radius: 50%;
  opacity: 0;
  background: var(--dust-color);
  will-change: transform, opacity;
  animation: dust-rise var(--duration, 10s) var(--delay, 0s) ease-in infinite;
}

@keyframes dust-rise {
  0% {
    opacity: 0;
    transform: translate3d(0, 0, 0);
  }
  10% {
    opacity: 0.55;
  }
  35% {
    opacity: 0.7;
    background: var(--dust-peak);
  }
  70% {
    opacity: 0.3;
    transform: translate3d(calc(var(--dust-sway, 0) * 20px), -70vh, 0);
  }
  100% {
    opacity: 0;
    transform: translate3d(calc(var(--dust-sway, 0) * 40px), -120vh, 0);
  }
}

.dust-speck:nth-child(odd)  { --dust-sway: 1; }
.dust-speck:nth-child(even) { --dust-sway: -1; }
.dust-speck:nth-child(3n)   { --dust-sway: 0.5; }
.dust-speck:nth-child(3n+1) { --dust-sway: -0.7; }

@media (prefers-reduced-motion: reduce) {
  .dust-speck { animation: none; display: none; }
}

@media (max-width: 768px) {
  /* 粒子起点在视口外且 opacity:0，暂停动画即不可见，零持续重绘成本 */
  .dust-speck { animation: none; }
}
</style>
