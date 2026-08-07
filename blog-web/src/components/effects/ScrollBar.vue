<template>
  <div class="scroll-progress" :style="{ width: percent + '%' }"></div>
</template>

<script setup>
/**
 * ScrollBar — 页面顶部极细进度条，随滚动从 0 → 100%。
 * 放到 FrontLayout 模板里即可（position:fixed 独立文档流）。
 */
import { ref, onMounted, onBeforeUnmount } from 'vue'

const percent = ref(0)
let ticking = false

// 用 rAF 合并滚动帧，避免每次 scroll 事件都触发 Vue 重渲染
const onScroll = () => {
  if (ticking) return
  ticking = true
  requestAnimationFrame(() => {
    const h = document.documentElement
    const total = h.scrollHeight - h.clientHeight
    percent.value = total > 0 ? Math.min(100, (h.scrollTop / total) * 100) : 0
    ticking = false
  })
}

onMounted(() => {
  window.addEventListener('scroll', onScroll, { passive: true })
  onScroll()
})
onBeforeUnmount(() => window.removeEventListener('scroll', onScroll))
</script>

<style scoped>
.scroll-progress {
  position: fixed; top: 0; left: 0; z-index: 10001;
  height: 3px;
  background: linear-gradient(90deg, #38bdf8, #0ea5e9, #22d3ee);
  border-radius: 0 2px 2px 0;
  transition: width 0.15s linear;
  pointer-events: none;
}
</style>