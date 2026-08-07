<template>
  <div class="leaf-field" aria-hidden="true">
    <div
      v-for="(l, i) in leaves"
      :key="i"
      class="leaf"
      :style="l.style"
    >
      <span class="leaf-sway" :style="l.swayStyle">
        <svg class="leaf-svg" :style="l.spinStyle" viewBox="0 0 24 24">
          <path :d="leafPath" :fill="l.color" />
        </svg>
      </span>
    </div>
  </div>
</template>

<script setup>
/**
 * FloatingLeaves — 枫叶飘落
 *
 * 少量叶片随风飘落：外层负责竖向下落，内层负责左右轻摆，
 * 叶片自身缓慢自转，三层 CSS 动画叠加出自然的"风起叶落"。
 * 数量默认 7（属"点缀"而非用户排斥的密集粒子），颜色以暖秋为主、
 * 掺一两片天蓝呼应主题。全部用负延迟打散，加载即满屏分布、不从顶端空降。
 */
import { computed } from 'vue'

const props = defineProps({
  count: { type: Number, default: 7 },
  // 最高透明度，整体压低以免抢内容
  maxOpacity: { type: Number, default: 0.82 }
})

// Material "eco" 叶形路径：干净利落的一片叶
const leafPath =
  'M6.05 8.05c-2.73 2.73-2.73 7.15-.02 9.88 1.47-3.4 4.09-6.24 7.36-7.93-2.77 2.34-4.71 5.61-5.39 9.32 2.6 1.23 5.8.78 7.95-1.37C19.43 14.47 20 4 20 4S9.53 4.57 6.05 8.05z'

// 暖秋为主，掺天蓝 / 薄荷，呼应晴天与玻璃质感
const palette = ['#f4a261', '#ef8354', '#e76f51', '#f6bd60', '#e9c46a', '#7dd3fc', '#a7f3d8']

const rand = (min, max) => min + Math.random() * (max - min)

const leaves = computed(() =>
  Array.from({ length: props.count }, () => {
    const size = rand(14, 26)
    const dur = rand(11, 20)
    const delay = -rand(0, 22)
    const swayDur = rand(3.5, 6)
    const swayAmp = rand(2.4, 4.6)
    const spinDur = rand(7, 15)
    const spinDir = Math.random() > 0.5 ? 'normal' : 'reverse'
    const color = palette[Math.floor(Math.random() * palette.length)]
    const opacity = rand(0.5, props.maxOpacity)
    return {
      color,
      style: {
        left: `${rand(0, 100).toFixed(2)}%`,
        '--dur': `${dur.toFixed(2)}s`,
        '--delay': `${delay.toFixed(2)}s`,
        '--size': `${size.toFixed(1)}px`,
        '--op': opacity.toFixed(2)
      },
      swayStyle: {
        '--sway': `${swayDur.toFixed(2)}s`,
        '--delay': `${delay.toFixed(2)}s`,
        '--sway-amp': `${swayAmp.toFixed(2)}vw`
      },
      spinStyle: {
        '--spin': `${spinDur.toFixed(2)}s`,
        '--spin-dir': spinDir
      }
    }
  })
)
</script>

<style scoped lang="scss">
.leaf-field {
  position: fixed;
  inset: 0;
  z-index: 0;
  pointer-events: none;
  /* 必须裁切：叶片从顶部 -14vh 下落、横摆会探出视口，
     屏幕外重绘拖慢帧率；叶片淡出在视口外，被裁无硬边。 */
  overflow: hidden;
}

.leaf {
  position: absolute;
  top: 0;
  will-change: transform;
  animation: leaf-fall var(--dur) linear var(--delay) infinite;
}

/* 内层只管左右轻摆 */
.leaf-sway {
  display: inline-block;
  will-change: transform;
  animation: leaf-sway var(--sway) ease-in-out var(--delay) infinite alternate;
}

/* 叶片自身缓慢自转，方向随机 */
.leaf-svg {
  display: block;
  width: var(--size);
  height: var(--size);
  opacity: var(--op);
  transform-origin: 50% 50%;
  animation-name: leaf-spin;
  animation-duration: var(--spin);
  animation-timing-function: linear;
  animation-iteration-count: infinite;
  animation-direction: var(--spin-dir, normal);
}

@keyframes leaf-fall {
  from { transform: translateY(-14vh); }
  to { transform: translateY(114vh); }
}
@keyframes leaf-sway {
  from { transform: translateX(calc(var(--sway-amp) * -1)); }
  to { transform: translateX(var(--sway-amp)); }
}
@keyframes leaf-spin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

/* 减少动态：直接隐去，保持背景安静 */
@media (prefers-reduced-motion: reduce) {
  .leaf-field { display: none; }
}

/* 移动端叶片更淡，避免与内容争视线 */
@media (max-width: 768px) {
  .leaf-svg { opacity: 0.6; }
}
</style>
