<template>
  <component
    :is="tag"
    class="gradient-border-card"
    :class="[`variant-${variant}`, { 'is-hoverable': hoverable, 'is-floating': floating }]"
  >
    <slot />
  </component>
</template>

<script setup>
defineProps({
  tag: { type: String, default: 'div' },
  variant: {
    type: String,
    default: 'sky',
    validator: (v) => ['sky', 'sun', 'mix', 'plain'].includes(v)
  },
  hoverable: { type: Boolean, default: false },
  floating: { type: Boolean, default: false }
})
</script>

<style scoped lang="scss">
.gradient-border-card {
  position: relative;
  background: var(--c-paper, var(--c-white));
  border-radius: 18px;
  padding: 24px;
  isolation: isolate;
  transition: transform 0.35s cubic-bezier(0.16, 1, 0.3, 1),
              box-shadow 0.35s ease;
}

// 渐变描边（用伪元素 + mask）
.gradient-border-card::before {
  content: '';
  position: absolute;
  inset: -1px;
  border-radius: inherit;
  padding: 1px;
  pointer-events: none;
  z-index: -1;
  opacity: 0.85;
  transition: opacity 0.4s ease;
}

// 晴空渐变（蓝 → 青）
.gradient-border-card.variant-sky::before {
  background: linear-gradient(135deg,
    var(--c-botany-300) 0%, var(--c-botany-500) 35%, var(--c-cyan-500) 70%, var(--c-botany-500) 100%);
  background-size: 200% 200%;
  -webkit-mask: linear-gradient(var(--c-black) 0 0) content-box, linear-gradient(var(--c-black) 0 0);
  -webkit-mask-composite: xor;
          mask-composite: exclude;
  animation: gb-flow 8s linear infinite paused;
}

// 暖阳渐变（金 → 蓝）
.gradient-border-card.variant-sun::before {
  background: linear-gradient(135deg,
    var(--c-autumn-300) 0%, var(--c-autumn-500) 40%, var(--c-botany-500) 100%);
  -webkit-mask: linear-gradient(var(--c-black) 0 0) content-box, linear-gradient(var(--c-black) 0 0);
  -webkit-mask-composite: xor;
          mask-composite: exclude;
}

// 混合（三色）
.gradient-border-card.variant-mix::before {
  background: linear-gradient(135deg,
    var(--c-botany-500) 0%, var(--c-cyan-500) 35%, var(--c-autumn-500) 70%, var(--c-botany-500) 100%);
  -webkit-mask: linear-gradient(var(--c-black) 0 0) content-box, linear-gradient(var(--c-black) 0 0);
  -webkit-mask-composite: xor;
          mask-composite: exclude;
}

// 纯净白
.gradient-border-card.variant-plain::before { display: none; }

// hover：渐变流光跑起来 + 抬升
.gradient-border-card.is-hoverable {
  cursor: pointer;
}
.gradient-border-card.is-hoverable:hover {
  transform: translateY(-4px);
  box-shadow: 0 16px 40px rgba(var(--theme-primary-strong-rgb), 0.12),
              0 24px 60px rgba(var(--theme-primary-strong-rgb), 0.14);
}
.gradient-border-card.is-hoverable:hover::before {
  animation-play-state: running;
  opacity: 1;
}

// floating：微微漂浮
.gradient-border-card.is-floating {
  animation: gb-float 7s ease-in-out infinite;
}

@keyframes gb-flow {
  0%   { background-position: 0% 50%; }
  100% { background-position: 200% 50%; }
}
@keyframes gb-float {
  0%, 100% { transform: translateY(0); }
  50%      { transform: translateY(-10px); }
}

@media (prefers-reduced-motion: reduce) {
  .gradient-border-card,
  .gradient-border-card::before {
    animation: none !important;
    transition: none !important;
  }
}
</style>