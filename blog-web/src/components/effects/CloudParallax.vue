<template>
  <div ref="root" class="cloud-parallax" aria-hidden="true">
    <div
      v-for="c in clouds"
      :key="c.id"
      class="pc"
      :class="[`pc-${c.layer}`]"
      :style="c.style"
    ></div>
  </div>
</template>

<script setup>
/**
 * CloudParallax — 云朵视差滚动
 *
 * 远景云走得慢、近景云走得快，滚动时形成天空纵深感。
 * 位移用 transform 驱动并在 rAF 中批量写入，不触发重排。
 * 全部 pointer-events: none，不干扰任何交互。
 */
import { ref, onMounted, onBeforeUnmount } from 'vue'

const props = defineProps({
  // 整体强度系数，0 = 关闭视差
  intensity: { type: Number, default: 1 }
})

/**
 * layer: far(远/慢/淡/小) → mid → near(近/快/清晰/大)
 * speed: 相对滚动的位移系数（正数 = 比页面滚得慢，产生"漂浮在后方"感）
 */
const clouds = [
  { id: 1, layer: 'far',  speed: 0.10, style: { top: '6vh',  left: '4%',   '--w': '150px' } },
  { id: 2, layer: 'far',  speed: 0.13, style: { top: '30vh', right: '8%',  '--w': '120px' } },
  { id: 3, layer: 'far',  speed: 0.09, style: { top: '78vh', left: '22%',  '--w': '135px' } },
  { id: 4, layer: 'mid',  speed: 0.22, style: { top: '14vh', right: '3%',  '--w': '210px' } },
  { id: 5, layer: 'mid',  speed: 0.26, style: { top: '56vh', left: '2%',   '--w': '180px' } },
  { id: 6, layer: 'mid',  speed: 0.20, style: { top: '104vh', right: '16%', '--w': '195px' } },
  { id: 7, layer: 'near', speed: 0.40, style: { top: '40vh', left: '12%',  '--w': '250px' } },
  { id: 8, layer: 'near', speed: 0.46, style: { top: '90vh', right: '6%',  '--w': '230px' } }
]

const root = ref(null)
let ticking = false
let reduce = false

const apply = () => {
  const y = window.scrollY || document.documentElement.scrollTop || 0
  const nodes = root.value?.children
  if (nodes) {
    for (let i = 0; i < nodes.length; i++) {
      const speed = clouds[i]?.speed ?? 0.2
      // 负号：云朵回移，视觉上比正文滚得慢
      const dy = -y * speed * props.intensity
      nodes[i].style.transform = `translate3d(0, ${dy.toFixed(2)}px, 0)`
    }
  }
  ticking = false
}

const onScroll = () => {
  if (ticking || reduce) return
  ticking = true
  requestAnimationFrame(apply)
}

onMounted(() => {
  reduce = window.matchMedia?.('(prefers-reduced-motion: reduce)').matches ?? false
  if (reduce) return
  window.addEventListener('scroll', onScroll, { passive: true })
  apply()
})
onBeforeUnmount(() => {
  window.removeEventListener('scroll', onScroll)
})
</script>

<style scoped lang="scss">
.cloud-parallax {
  position: absolute;
  inset: 0;
  /* 必须裁切：云朵尺寸 150~250px 且随机定位，部分会探出容器，
     屏幕外重绘拖慢帧率；云朵本身是柔白模糊团，被裁处无硬边。 */
  overflow: hidden;
  pointer-events: none;
  z-index: 0;
}

/* 纯 CSS 蓬松白云：主体 + 两个圆鼓包 */
.pc {
  position: absolute;
  width: var(--w, 180px);
  height: calc(var(--w, 180px) * 0.30);
  border-radius: 999px;
  background: var(--c-paper);
  will-change: transform;

  &::before,
  &::after {
    content: '';
    position: absolute;
    background: var(--c-paper);
    border-radius: 50%;
  }
  /* 左侧高鼓包 */
  &::before {
    width: 52%;
    height: 165%;
    top: -85%;
    left: 14%;
  }
  /* 右侧矮鼓包 */
  &::after {
    width: 38%;
    height: 120%;
    top: -58%;
    right: 16%;
  }
}

/* 远景：淡、糊，营造纵深 */
.pc-far {
  opacity: 0.42;
  filter: blur(3px);
}
/* 中景 */
.pc-mid {
  opacity: 0.62;
  filter: blur(1px);
  box-shadow: 0 10px 26px rgba(var(--theme-primary-light-rgb), 0.14);
}
/* 近景：清晰、有投影 */
.pc-near {
  opacity: 0.78;
  filter: drop-shadow(0 12px 28px rgba(var(--theme-primary-light-rgb), 0.20));
}

/* 移动端减少云朵数量，避免遮挡与性能损耗 */
@media (max-width: 768px) {
  .pc-far { display: none; }
  .pc-near { opacity: 0.55; }
}
</style>
