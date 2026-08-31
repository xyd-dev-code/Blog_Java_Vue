<template>
  <Teleport to="body">
    <transition name="btt">
      <button
        v-show="visible"
        class="back-to-top"
        type="button"
        aria-label="回到顶部"
        title="回到顶部"
        @click="toTop"
      >
        <!-- 云朵托着向上的箭头，呼应晴天主题 -->
        <svg class="btt-icon" viewBox="0 0 48 48" width="30" height="30" aria-hidden="true">
          <!-- 云朵 -->
          <g class="btt-cloud">
            <ellipse cx="17" cy="33" rx="11" ry="6" />
            <ellipse cx="26" cy="30" rx="9" ry="7.5" />
            <ellipse cx="33" cy="33" rx="8" ry="5.5" />
            <rect x="12" y="31" width="26" height="7" rx="3.5" />
          </g>
          <!-- 上升箭头 -->
          <path
            class="btt-arrow"
            d="M24 22 L24 9 M17.5 15.5 L24 9 L30.5 15.5"
            fill="none"
            stroke-width="3"
            stroke-linecap="round"
            stroke-linejoin="round"
          />
        </svg>
        <!-- 进度环：显示当前阅读位置 -->
        <svg class="btt-ring" viewBox="0 0 44 44" aria-hidden="true">
          <circle class="btt-ring-track" cx="22" cy="22" r="20" />
          <circle
            class="btt-ring-bar"
            cx="22"
            cy="22"
            r="20"
            :style="{ strokeDashoffset: dashOffset }"
          />
        </svg>
      </button>
    </transition>
  </Teleport>
</template>

<script setup>
import { ref, computed, onMounted, onBeforeUnmount } from 'vue'

const props = defineProps({
  // 滚动超过多少像素后浮现
  threshold: { type: Number, default: 320 }
})

const visible = ref(false)
const progress = ref(0)

const CIRCUMFERENCE = 2 * Math.PI * 20
const dashOffset = computed(() => CIRCUMFERENCE * (1 - progress.value))

let ticking = false

const update = () => {
  const y = window.scrollY || document.documentElement.scrollTop || 0
  visible.value = y > props.threshold

  const max = document.documentElement.scrollHeight - window.innerHeight
  progress.value = max > 0 ? Math.min(y / max, 1) : 0
  ticking = false
}

const onScroll = () => {
  if (ticking) return
  ticking = true
  requestAnimationFrame(update)
}

const toTop = () => {
  const reduce = window.matchMedia?.('(prefers-reduced-motion: reduce)').matches
  window.scrollTo({ top: 0, behavior: reduce ? 'auto' : 'smooth' })
}

onMounted(() => {
  window.addEventListener('scroll', onScroll, { passive: true })
  window.addEventListener('resize', onScroll, { passive: true })
  update()
})
onBeforeUnmount(() => {
  window.removeEventListener('scroll', onScroll)
  window.removeEventListener('resize', onScroll)
})
</script>

<style scoped lang="scss">
.back-to-top {
  position: fixed;
  right: 32px;
  bottom: 40px;
  z-index: 900;
  width: 52px;
  height: 52px;
  padding: 0;
  border: 1px solid rgba(var(--theme-paper-rgb), 0.75);
  border-radius: 50%;
  background: rgba(var(--theme-paper-rgb), 0.82);
  backdrop-filter: blur(12px);
  -webkit-backdrop-filter: blur(12px);
  box-shadow: 0 6px 22px rgba(var(--theme-primary-strong-rgb), 0.18);
  cursor: pointer;
  display: grid;
  place-items: center;
  transition:
    transform 0.28s cubic-bezier(0.22, 1, 0.36, 1),
    box-shadow 0.28s ease,
    background 0.28s ease;

  &:hover {
    transform: translateY(-4px);
    background: var(--c-paper);
    box-shadow: 0 12px 30px rgba(var(--theme-primary-strong-rgb), 0.28);

    .btt-cloud { fill: var(--c-botany-300); }
    .btt-arrow { stroke: var(--c-botany-900); }
    .btt-icon  { transform: translateY(-1px); }
  }

  &:active { transform: translateY(-1px); }

  &:focus-visible {
    outline: 2px solid var(--c-botany-500);
    outline-offset: 3px;
  }
}

.btt-icon {
  grid-area: 1 / 1;
  transition: transform 0.28s ease;
}
.btt-cloud {
  fill: var(--c-botany-100);
  transition: fill 0.28s ease;
}
.btt-arrow {
  stroke: var(--c-botany-700);
  transition: stroke 0.28s ease;
}

/* 阅读进度环 */
.btt-ring {
  grid-area: 1 / 1;
  width: 52px;
  height: 52px;
  transform: rotate(-90deg);
  pointer-events: none;
}
.btt-ring-track {
  fill: none;
  stroke: rgba(var(--theme-primary-rgb), 0.14);
  stroke-width: 2;
}
.btt-ring-bar {
  fill: none;
  stroke: var(--c-botany-500);
  stroke-width: 2;
  stroke-linecap: round;
  stroke-dasharray: 125.66; /* 2πr, r=20 */
  transition: stroke-dashoffset 0.15s linear;
}

/* 浮现 / 隐去 */
.btt-enter-active,
.btt-leave-active {
  transition:
    opacity 0.32s ease,
    transform 0.32s cubic-bezier(0.22, 1, 0.36, 1);
}
.btt-enter-from,
.btt-leave-to {
  opacity: 0;
  transform: translateY(16px) scale(0.9);
}

@media (max-width: 768px) {
  .back-to-top {
    right: 18px;
    bottom: 26px;
    width: 46px;
    height: 46px;
  }
  .btt-ring { width: 46px; height: 46px; }
}
</style>
