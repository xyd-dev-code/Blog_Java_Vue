<template>
  <div class="god-rays" aria-hidden="true" :style="{ '--rays-origin-y': originY + '%' }">
    <span class="gr-ray gr-ray-1"></span>
    <span class="gr-ray gr-ray-2"></span>
    <span class="gr-ray gr-ray-3"></span>
    <span class="gr-ray gr-ray-4"></span>
    <span class="gr-ray gr-ray-5"></span>
    <span class="gr-ray gr-ray-6"></span>
    <span class="gr-ray gr-ray-7"></span>
  </div>
</template>

<script setup>
/**
 * GodRays — 圣光射线
 *
 * 从 hero 标题位置向上发散的 7 道光柱，天蓝 + 暖金交替，
 * 模拟阳光穿透云隙的效果。透明度极低 (0.04-0.10)，
 * 轻微左右微摆（scaleX 呼吸），不抢内容。
 *
 * Props: originY — 射线源点在容器中的垂直位置百分比，默认 75（靠近底部）。
 * 不同 hero 可以微调这个值来对齐各自的标题位置。
 */
defineProps({
  originY: { type: Number, default: 75 }
})
</script>

<style scoped lang="scss">
.god-rays {
  position: absolute;
  inset: 0;
  overflow: hidden;
  pointer-events: none;
}

.gr-ray {
  position: absolute;
  bottom: calc((100% - var(--rays-origin-y, 75%)));
  width: 1px;
  transform-origin: bottom center;
  will-change: transform;
}

/* 暖金主导：共 4 道 (1, 3, 5, 7)，交错排列 */
.gr-ray-1 {
  height: 170%;
  left: 18%;
  opacity: 0.07;
  background: linear-gradient(to top, rgba(251,191,36,0.12), transparent);
  animation: ray-sway-1 18s ease-in-out infinite;
}
.gr-ray-2 {
  height: 160%;
  left: 28%;
  opacity: 0.05;
  background: linear-gradient(to top, rgba(56,189,248,0.1), transparent);
  animation: ray-sway-2 22s ease-in-out infinite;
}
.gr-ray-3 {
  height: 180%;
  left: 42%;
  opacity: 0.08;
  background: linear-gradient(to top, rgba(251,191,36,0.14), transparent);
  animation: ray-sway-3 20s ease-in-out infinite;
}
.gr-ray-4 {
  height: 150%;
  left: 53%;
  opacity: 0.06;
  background: linear-gradient(to top, rgba(14,165,233,0.11), transparent);
  animation: ray-sway-1 25s ease-in-out infinite;
}
.gr-ray-5 {
  height: 175%;
  left: 64%;
  opacity: 0.07;
  background: linear-gradient(to top, rgba(251,191,36,0.1), transparent);
  animation: ray-sway-2 19s ease-in-out infinite;
}
.gr-ray-6 {
  height: 140%;
  left: 74%;
  opacity: 0.05;
  background: linear-gradient(to top, rgba(125,211,252,0.09), transparent);
  animation: ray-sway-3 23s ease-in-out infinite;
}
.gr-ray-7 {
  height: 165%;
  left: 82%;
  opacity: 0.06;
  background: linear-gradient(to top, rgba(251,191,36,0.11), transparent);
  animation: ray-sway-1 21s ease-in-out infinite;
}

@keyframes ray-sway-1 {
  0%, 100% { transform: scaleX(1) translateY(0); }
  50% { transform: scaleX(1.8) translateY(-1vh); }
}
@keyframes ray-sway-2 {
  0%, 100% { transform: scaleX(1) translateY(0); }
  50% { transform: scaleX(1.5) translateY(-0.8vh); }
}
@keyframes ray-sway-3 {
  0%, 100% { transform: scaleX(1) translateY(0); }
  50% { transform: scaleX(2.0) translateY(-1.2vh); }
}

@media (prefers-reduced-motion: reduce) {
  .gr-ray { animation: none; }
}

@media (max-width: 768px) {
  .gr-ray { opacity: 0.03; }
}
</style>
