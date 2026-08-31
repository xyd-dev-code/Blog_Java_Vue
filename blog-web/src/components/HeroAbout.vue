<template>
  <div class="hero-about">
    <GodRays :origin-y="55" />
    <!-- 柔光光晕 -->
    <div class="ha-aura ha-aura-1"></div>
    <div class="ha-aura ha-aura-2"></div>
    <div class="ha-aura ha-aura-3"></div>

    <!-- 光束（从顶部斜射） -->
    <div class="ha-beam ha-beam-1"></div>
    <div class="ha-beam ha-beam-2"></div>

    <!-- 漂浮几何图形 -->
    <div class="ha-shapes" aria-hidden="true">
      <span class="ha-shape ha-shape-1"></span>
      <span class="ha-shape ha-shape-2"></span>
      <span class="ha-shape ha-shape-3"></span>
      <span class="ha-shape ha-shape-4"></span>
      <span class="ha-shape ha-shape-5"></span>
      <span class="ha-shape ha-shape-6"></span>
    </div>

    <!-- 散点星尘 -->
    <div class="ha-dust" aria-hidden="true">
      <span v-for="i in 18" :key="i" class="ha-d"
        :style="{
          left: (4 + (i * 13) % 96) + '%',
          top: (6 + (i * 7) % 88) + '%',
          width: (2 + (i % 3)) + 'px',
          height: (2 + (i % 3)) + 'px',
          animationDelay: (i * 0.45) % 5 + 's',
          animationDuration: (3 + (i % 3) * 1.5) + 's'
        }"
      ></span>
    </div>

    <!-- 光屏扫过 -->
    <div class="ha-light-scan"></div>

    <div class="container ha-inner">
      <p class="ha-eyebrow">about</p>
      <h1 class="ha-title"><WuxiaHeadingLettering :text="title" /></h1>
      <div class="ha-tagline" v-if="subtitle">
        <span class="ha-tagline-word" v-for="(w, i) in taglineWords" :key="i"
          :style="{ animationDelay: (0.6 + i * 0.15) + 's' }"
        >{{ w }}</span>
      </div>
    </div>
  </div>
</template>

<script setup>
import WuxiaHeadingLettering from '@/components/WuxiaHeadingLettering.vue'
import { computed } from 'vue'
import GodRays from '@/components/effects/GodRays.vue'

const props = defineProps({
  title: { type: String, required: true },
  subtitle: { type: String, default: '' }
})

// 从副标题中拆出词作逐字动画
const taglineWords = computed(() => {
  if (!props.subtitle) return []
  return props.subtitle.replace(/[，,。.、\s]+/g, ' ').split(/\s+/).filter(Boolean)
})
</script>

<style scoped lang="scss">
.hero-about {
  position: relative;
  /* 不裁切——光束和漂浮图形会溢出 */
  /* 不铺不透明底色——让全局天空透上来 */
  background:
    radial-gradient(ellipse 100% 70% at 50% 10%, rgba(var(--theme-primary-rgb), 0.08) 0%, transparent 55%),
    radial-gradient(ellipse 60% 50% at 80% 70%, rgba(var(--theme-accent-rgb), 0.04) 0%, transparent 45%);
  padding: 56px 0 68px;
  text-align: center;
  margin-bottom: 20px;
  overflow: hidden;
}

/* ===== 光晕 ===== */
.ha-aura {
  position: absolute;
  border-radius: 50%;
  filter: blur(70px);
  pointer-events: none;
}
.ha-aura-1 {
  top: -15%; left: 25%;
  width: 280px; height: 280px;
  background: rgba(var(--theme-primary-rgb), 0.14);
  animation: aura-float 10s ease-in-out infinite;
}
.ha-aura-2 {
  bottom: 5%; right: 8%;
  width: 220px; height: 220px;
  background: rgba(var(--theme-accent-rgb), 0.09);
  animation: aura-float 14s ease-in-out infinite -5s;
}
.ha-aura-3 {
  top: 40%; left: 60%;
  width: 160px; height: 160px;
  background: rgba(var(--theme-primary-light-rgb), 0.08);
  animation: aura-float 12s ease-in-out infinite -3s;
}
@keyframes aura-float {
  0%, 100% { transform: translateY(0) scale(1); }
  50% { transform: translateY(-20px) scale(1.08); }
}

/* ===== 光束 ===== */
.ha-beam {
  position: absolute;
  top: -30%;
  width: 1.5px;
  height: 150%;
  pointer-events: none;
}
.ha-beam-1 {
  left: 22%;
  background: linear-gradient(180deg,
    transparent 0%,
    rgba(var(--theme-primary-rgb), 0.20) 25%,
    rgba(var(--theme-primary-rgb), 0.10) 55%,
    transparent 100%);
  transform: rotate(6deg);
  animation: beam-sway 12s ease-in-out infinite;
}
.ha-beam-2 {
  left: 72%;
  width: 2px;
  background: linear-gradient(180deg,
    transparent 0%,
    rgba(var(--theme-accent-rgb), 0.14) 30%,
    rgba(var(--theme-accent-rgb), 0.06) 60%,
    transparent 100%);
  transform: rotate(-4deg);
  animation: beam-sway 16s ease-in-out infinite -6s;
}
@keyframes beam-sway {
  0%, 100% { transform: rotate(6deg) translateX(0); }
  50% { transform: rotate(9deg) translateX(15px); }
}

/* ===== 漂浮几何图形 ===== */
.ha-shapes {
  position: absolute;
  inset: 0;
  pointer-events: none;
  overflow: hidden;
}
.ha-shape {
  position: absolute;
  border-radius: 50%;
  opacity: 0;
  animation: shape-float var(--dur, 8s) ease-in-out infinite;
  animation-delay: var(--del, 0s);
}
/* 圆环 */
.ha-shape-1 {
  width: 48px; height: 48px;
  border: 2px solid rgba(var(--theme-primary-rgb), 0.25);
  background: transparent;
  top: 12%; right: 18%;
  --dur: 9s; --del: 0s;
}
.ha-shape-2 {
  width: 28px; height: 28px;
  border: 2px solid rgba(var(--theme-accent-rgb), 0.20);
  background: transparent;
  top: 55%; left: 10%;
  --dur: 11s; --del: -3s;
}
/* 实心圆点 */
.ha-shape-3 {
  width: 10px; height: 10px;
  background: rgba(var(--theme-primary-rgb), 0.35);
  top: 28%; left: 35%;
  --dur: 7s; --del: -1.5s;
}
.ha-shape-4 {
  width: 6px; height: 6px;
  background: rgba(var(--theme-accent-rgb), 0.30);
  top: 68%; right: 32%;
  --dur: 8s; --del: -4s;
}
/* 菱形（旋转正方形） */
.ha-shape-5 {
  width: 16px; height: 16px;
  background: rgba(var(--theme-primary-light-rgb), 0.18);
  border-radius: 3px;
  top: 38%; right: 12%;
  --dur: 10s; --del: -2s;
  transform: rotate(45deg);
}
.ha-shape-6 {
  width: 12px; height: 12px;
  background: rgba(var(--theme-primary-rgb), 0.22);
  border-radius: 3px;
  top: 75%; left: 28%;
  --dur: 9s; --del: -5.5s;
  transform: rotate(30deg);
}
@keyframes shape-float {
  0%   { opacity: 0; transform: translateY(20px) scale(0.8) rotate(var(--rot, 0deg)); }
  20%  { opacity: 1; }
  80%  { opacity: 0.8; }
  100% { opacity: 0; transform: translateY(-25px) scale(1.1) rotate(calc(var(--rot, 0deg) + 30deg)); }
}

/* ===== 散点星尘 ===== */
.ha-dust {
  position: absolute;
  inset: 0;
  pointer-events: none;
  overflow: hidden;
}
.ha-d {
  position: absolute;
  border-radius: 50%;
  background: rgba(var(--theme-primary-rgb), 0.45);
  box-shadow: 0 0 3px rgba(var(--theme-primary-rgb), 0.25);
  animation: dust-pulse 3.5s ease-in-out infinite;
}
@keyframes dust-pulse {
  0%, 100% { opacity: 0.25; transform: scale(1); }
  50% { opacity: 0.85; transform: scale(1.6); }
}

/* ===== 光屏扫描 ===== */
.ha-light-scan {
  position: absolute;
  top: 0; left: -50%;
  width: 200%; height: 100%;
  background: linear-gradient(105deg,
    transparent 0%, transparent 40%,
    rgba(var(--theme-paper-rgb), 0.25) 45%, rgba(var(--theme-paper-rgb), 0.10) 48%,
    transparent 50%, transparent 100%);
  z-index: 1;
  pointer-events: none;
  animation: scan 8s ease-in-out infinite;
}
@keyframes scan {
  0%, 100% { transform: translateX(-10%); }
  50% { transform: translateX(10%); }
}

/* ===== 内容区 ===== */
.ha-inner {
  position: relative;
  z-index: 2;
}
.ha-eyebrow {
  font-size: 12px;
  letter-spacing: 0.25em;
  text-transform: uppercase;
  color: var(--c-botany-500);
  margin: 0 0 8px;
  font-weight: 500;
}
.ha-title {
  font-family: var(--font-serif);
  font-size: clamp(32px, 4.5vw, 46px);
  font-weight: 700;
  color: var(--c-ink);
  margin: 0 0 10px;
  line-height: 1.2;
}
.ha-tagline {
  display: flex;
  justify-content: center;
  gap: 8px;
  flex-wrap: wrap;
}
.ha-tagline-word {
  display: inline-block;
  font-size: 14px;
  color: var(--c-botany-600);
  opacity: 0;
  animation: word-reveal 0.6s ease-out forwards;
}
@keyframes word-reveal {
  from { opacity: 0; transform: translateY(8px); }
  to   { opacity: 0.75; transform: translateY(0); }
}

@media (max-width: 768px) {
  .hero-about { padding: 44px 0 52px; }
  .ha-title { font-size: 28px; }
  .ha-shapes { display: none; }
  .ha-dust { display: none; }
}
</style>
