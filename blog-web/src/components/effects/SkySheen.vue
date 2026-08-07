<template>
  <div class="sky-sheen" aria-hidden="true">
    <span class="sheen sheen-1"></span>
    <span class="sheen sheen-2"></span>
    <span class="sheen sheen-3"></span>
  </div>
</template>

<script setup>
/**
 * SkySheen — 天光流影
 *
 * 三团柔焦渐变光晕（天蓝 / 暖金 / 薄荷）在晴空中缓缓漂移、缩放，
 * 让原本静态的渐变天幕有了"呼吸感"。纯 CSS 动画，GPU 友好，
 * 不引任何粒子。整体透明度压得很低，只作背景氛围，不抢内容。
 *
 * 浅色主题下光晕用实色 + 低透明度常规叠加，绝不用 mix-blend-mode
 * （screen 在白底是吸收态会隐形，multiply 则发灰发脏）。
 */
</script>

<style scoped lang="scss">
.sky-sheen {
  position: fixed;
  inset: 0;
  z-index: 0;
  pointer-events: none;
  /* 必须裁切：72vw + blur(64px) 的光斑若在屏幕外大片重绘会拖垮帧率。
     光斑靠径向渐变在 70% 处化为透明，被裁的只是看不见的尾巴，无硬边。 */
  overflow: hidden;
}

.sheen {
  position: absolute;
  border-radius: 50%;
  filter: blur(64px);
  will-change: transform, opacity;
}

/* 天蓝：左上，最大，最醒目 */
.sheen-1 {
  width: 54vw;
  height: 54vw;
  left: -12vw;
  top: -14vh;
  opacity: 0.5;
  background: radial-gradient(circle, rgba(125, 211, 252, 0.55) 0%, rgba(125, 211, 252, 0) 70%);
  animation: sheen-drift-1 27s ease-in-out infinite;
}

/* 暖金：右上，呼应晴天夕照，最淡 */
.sheen-2 {
  width: 46vw;
  height: 46vw;
  right: -10vw;
  top: -2vh;
  opacity: 0.42;
  background: radial-gradient(circle, rgba(251, 191, 36, 0.32) 0%, rgba(251, 191, 36, 0) 70%);
  animation: sheen-drift-2 33s ease-in-out infinite;
}

/* 薄荷：底部，补一点清新绿意 */
.sheen-3 {
  width: 58vw;
  height: 58vw;
  left: 12vw;
  bottom: -24vh;
  opacity: 0.4;
  background: radial-gradient(circle, rgba(167, 243, 208, 0.42) 0%, rgba(167, 243, 208, 0) 70%);
  animation: sheen-drift-3 39s ease-in-out infinite;
}

@keyframes sheen-drift-1 {
  0%, 100% { transform: translate3d(0, 0, 0) scale(1); }
  50% { transform: translate3d(6vw, 5vh, 0) scale(1.12); }
}
@keyframes sheen-drift-2 {
  0%, 100% { transform: translate3d(0, 0, 0) scale(1); }
  50% { transform: translate3d(-5vw, 7vh, 0) scale(1.15); }
}
@keyframes sheen-drift-3 {
  0%, 100% { transform: translate3d(0, 0, 0) scale(1); }
  50% { transform: translate3d(4vw, -6vh, 0) scale(1.1); }
}

/* 尊重系统的"减少动态"偏好：彻底静止，保持晴空干净 */
@media (prefers-reduced-motion: reduce) {
  .sheen { animation: none; }
}

/* 移动端弱化：光晕范围收窄，省电省重绘 */
@media (max-width: 768px) {
  .sheen-1, .sheen-2, .sheen-3 { opacity: 0.3; }
}
</style>
