<template>
  <div class="hero-friends">
    <GodRays :origin-y="68" />
    <!-- 满天星散点（三色：浅蓝 / 深蓝 / 暖橙） -->
    <div class="hf-constellation" aria-hidden="true">
      <span
        v-for="(d, i) in blueDots" :key="`b${i}`"
        class="hf-dot hf-dot-blue"
        :style="{ left: d.x + '%', top: d.y + '%', width: d.size + 'px', height: d.size + 'px', animationDelay: d.delay + 's' }"
      ></span>
      <span
        v-for="(d, i) in deepDots" :key="`d${i}`"
        class="hf-dot hf-dot-deep"
        :style="{ left: d.x + '%', top: d.y + '%', width: d.size + 'px', height: d.size + 'px', animationDelay: d.delay + 's' }"
      ></span>
      <span
        v-for="(d, i) in warmDots" :key="`w${i}`"
        class="hf-dot hf-dot-warm"
        :style="{ left: d.x + '%', top: d.y + '%', width: d.size + 'px', height: d.size + 'px', animationDelay: d.delay + 's' }"
      ></span>
    </div>

    <!-- 暖色光晕 -->
    <div class="hf-glow hf-glow-1"></div>
    <div class="hf-glow hf-glow-2"></div>

    <div class="container hf-inner">
      <p class="hf-eyebrow">friends</p>
      <h1 class="hf-title">
        <WuxiaHeadingLettering :text="title" />
        <span class="hf-connect-icon">
          <svg width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
            <circle cx="7" cy="6" r="2.5"/>
            <circle cx="17" cy="6" r="2.5"/>
            <circle cx="12" cy="17" r="2.5"/>
            <line x1="9" y1="7.5" x2="11" y2="15"/>
            <line x1="15" y1="7.5" x2="13" y2="15"/>
            <line x1="10.5" y1="18.5" x2="13.5" y2="18.5"/>
          </svg>
        </span>
      </h1>
      <p class="hf-sub" v-if="subtitle">{{ subtitle }}</p>
    </div>

    <!-- 渐变过渡已移除：不再需要 .hf-fade 硬过渡线，
         全局天空渐变自然透出，hero 与内容区一体衔接 -->
  </div>
</template>

<script setup>
import WuxiaHeadingLettering from '@/components/WuxiaHeadingLettering.vue'
import GodRays from '@/components/effects/GodRays.vue'
defineProps({
  title: { type: String, required: true },
  subtitle: { type: String, default: '' }
})

// 三色满天星散点（浅蓝 / 深蓝 / 暖橙）
// 位置用确定性算式生成 + 区间取模，避免水合不一致；尺寸 2-5 px，分布稀疏
const blueDots = Array.from({ length: 14 }, (_, i) => ({
  x: 4 + ((i * 91 + 13) % 100) * 0.92,
  y: 6 + ((i * 23 + 5) % 17) * 5.2,
  size: 2 + (i % 3),
  delay: (i * 0.55) % 4
}))
const deepDots = Array.from({ length: 7 }, (_, i) => ({
  x: 8 + ((i * 73 + 17) % 100) * 0.86,
  y: 12 + ((i * 19 + 3) % 14) * 5.4,
  size: 3 + (i % 2),
  delay: (i * 0.95) % 4
}))
const warmDots = Array.from({ length: 4 }, (_, i) => ({
  x: 16 + ((i * 47 + 11) % 100) * 0.74,
  y: 30 + ((i * 17 + 7) % 10) * 5.5,
  size: 2 + (i % 2),
  delay: (i * 1.3) % 4
}))
</script>

<style scoped lang="scss">
.hero-friends {
  position: relative;
  /* 不在根元素上 overflow:hidden ——光晕(blur 60px)和星座节点会溢出，
     被硬切后在视口边缘形成截断线。改用局部裁切（见 .hf-constellation）*/
  /* 不设不透明底色——让全局 html,body 的天空渐变透上来，实现"一体"感。
     只留极淡的 alpha 光斑作为装饰性氛围 */
  background:
    radial-gradient(ellipse 70% 60% at 50% 0%, rgba(var(--theme-primary-light-rgb), 0.08) 0%, transparent 65%),
    radial-gradient(ellipse 60% 50% at 18% 80%, rgba(var(--theme-accent-rgb), 0.04) 0%, transparent 70%),
    radial-gradient(ellipse 50% 40% at 90% 80%, rgba(var(--theme-primary-rgb), 0.05) 0%, transparent 70%);
  padding: 48px 0 56px;
  text-align: center;
  margin-bottom: 18px;
  /* useFoldFit 会按屏高写入 min-height，让首行友链卡刚好落在折痕之上。
     flex 居中保证撑高后标题仍在视觉中心 */
  display: flex;
  flex-direction: column;
  justify-content: center;
}

/* 光晕 */
.hf-glow {
  position: absolute;
  border-radius: 50%;
  filter: blur(60px);
  pointer-events: none;
}
.hf-glow-1 {
  top: -10%;
  right: 5%;
  width: 220px;
  height: 220px;
  background: rgba(var(--theme-primary-rgb), 0.15);
  animation: float 10s ease-in-out infinite;
}
.hf-glow-2 {
  bottom: 10%;
  left: 8%;
  width: 180px;
  height: 180px;
  background: rgba(var(--theme-accent-rgb), 0.12);
  animation: float 12s ease-in-out infinite -4s;
}

/* 星座节点 ——唯一需要裁切的子层：散点不应溢出 hero 区域 */
.hf-constellation {
  position: absolute;
  inset: 0;
  z-index: 0;
  pointer-events: none;
  overflow: hidden;
  /* 连接线用伪元素画在容器上 */
  &::before {
    content: '';
    position: absolute;
    inset: 0;
    background:
      /* 水平连接 */
      repeating-linear-gradient(90deg,
        transparent 0px, transparent 144px,
        rgba(var(--theme-primary-rgb), 0.06) 144px, rgba(var(--theme-primary-rgb), 0.06) 145px
      );
  }
}
.hf-dot {
  position: absolute;
  border-radius: 50%;
  animation: pulse-dot 3s ease-in-out infinite;
  pointer-events: none;
}
.hf-dot-blue {
  background: rgba(var(--theme-primary-light-rgb), 0.75);
  box-shadow: 0 0 4px rgba(var(--theme-primary-light-rgb), 0.5);
}
.hf-dot-deep {
  background: rgba(var(--theme-primary-strong-rgb), 0.85);
  box-shadow: 0 0 6px rgba(var(--theme-primary-strong-rgb), 0.5);
}
.hf-dot-warm {
  background: rgba(var(--theme-accent-rgb), 0.85);
  box-shadow: 0 0 6px rgba(var(--theme-accent-rgb), 0.55);
}
@keyframes pulse-dot {
  0%, 100% { transform: scale(1); opacity: 0.5; }
  50% { transform: scale(2.2); opacity: 1; }
}
@keyframes float {
  0%, 100% { transform: translateY(0) scale(1); }
  50% { transform: translateY(-15px) scale(1.1); }
}

/* 内容 */
.hf-inner {
  position: relative;
  z-index: 2;
}
.hf-eyebrow {
  font-size: 12px;
  letter-spacing: 0.25em;
  text-transform: uppercase;
  color: var(--c-autumn-500);
  margin: 0 0 8px;
  font-weight: 500;
}
.hf-title {
  font-family: var(--font-serif);
  font-size: 32px;
  font-weight: 700;
  color: var(--c-ink);
  margin: 0 0 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
}
.hf-connect-icon {
  display: inline-flex;
  color: var(--c-autumn-500);
  animation: float 4s ease-in-out infinite;
  svg { width: 22px; height: 22px; }
}
.hf-sub {
  font-size: 14px;
  color: var(--c-ink-soft);
  margin: 0;
  font-weight: 300;
  letter-spacing: 0.04em;
}

/* .hf-fade 已移除——全局天空自然透出，无需硬过渡线 */

@media (max-width: 768px) {
  .hero-friends { padding: 36px 0 42px; }
  .hf-title { font-size: 24px; }
  .hf-fade { height: 28px; }
}
</style>
