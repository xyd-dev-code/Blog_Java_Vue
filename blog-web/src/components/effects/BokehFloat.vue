<template>
  <div class="bokeh-field" aria-hidden="true">
    <span
      v-for="(b, i) in orbs"
      :key="i"
      class="bokeh"
      :style="b"
    ></span>
  </div>
</template>

<script setup>
/**
 * BokehFloat — 浮光
 *
 * 少量柔焦光点自底部缓缓升起，带轻微横向漂移与透明度呼吸，
 * 像阳光里浮动的微尘，给晴空添一层airy的空气感。数量默认 10、
 * 透明度压到极低（0.14~0.38），是"柔光"而非用户排斥的密集粒子。
 * 纯 CSS 动画，尊重 prefers-reduced-motion。
 */
import { computed } from 'vue'

const props = defineProps({
  count: { type: Number, default: 10 }
})

const rand = (min, max) => min + Math.random() * (max - min)

const orbs = computed(() =>
  Array.from({ length: props.count }, () => {
    const size = rand(8, 26)
    const dur = rand(14, 30)
    const delay = -rand(0, 30)
    const dx = rand(-6, 6)
    const op = rand(0.14, 0.38)
    return {
      left: `${rand(0, 100).toFixed(2)}%`,
      width: `${size.toFixed(1)}px`,
      height: `${size.toFixed(1)}px`,
      '--dur': `${dur.toFixed(2)}s`,
      '--delay': `${delay.toFixed(2)}s`,
      '--dx': `${dx.toFixed(2)}vw`,
      '--op': op.toFixed(2)
    }
  })
)
</script>

<style scoped lang="scss">
.bokeh-field {
  position: fixed;
  inset: 0;
  z-index: 0;
  pointer-events: none;
  /* 必须裁切：光点自底升起、横向漂移会探出视口，
     屏幕外重绘拖慢帧率；光点透明尾段被裁无硬边。 */
  overflow: hidden;
}

.bokeh {
  position: absolute;
  bottom: 0;
  border-radius: 50%;
  filter: blur(1.5px);
  background: radial-gradient(
    circle,
    rgba(var(--theme-paper-rgb), 0.75) 0%,
    rgba(var(--theme-primary-light-rgb), 0.28) 55%,
    rgba(var(--theme-primary-light-rgb), 0) 72%
  );
  will-change: transform, opacity;
  animation: bokeh-rise var(--dur) linear var(--delay) infinite;
}

@keyframes bokeh-rise {
  0% {
    transform: translate(0, 12vh) scale(0.8);
    opacity: 0;
  }
  15% {
    opacity: var(--op);
  }
  85% {
    opacity: var(--op);
  }
  100% {
    transform: translate(var(--dx), -116vh) scale(1.15);
    opacity: 0;
  }
}

@media (prefers-reduced-motion: reduce) {
  .bokeh-field { display: none; }
}

/* 移动端：暂停浮光持续重绘（静止即不可见，无视觉损失） */
@media (max-width: 768px) {
  .bokeh { animation: none; }
}
</style>
