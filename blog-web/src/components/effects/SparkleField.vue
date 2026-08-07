<template>
  <div class="sparkle-field" aria-hidden="true">
    <span
      v-for="s in sparkles"
      :key="s.id"
      class="sp"
      :style="s.style"
    />
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, onBeforeUnmount } from 'vue'

const COUNT = 44
const sparkles = ref([])
const mouse = reactive({ x: -999, y: -999, active: false })
let rafId = null

function rand(min, max) {
  return Math.random() * (max - min) + min
}

function generateSparkles() {
  const items = []
  for (let i = 0; i < COUNT; i++) {
    const size = rand(2, 5.5)
    const hue = Math.random() < 0.35 ? 40 : Math.random() < 0.5 ? 195 : 210 // gold / sky / cyan
    const sat = rand(60, 100)
    const light = rand(65, 92)
    const baseX = rand(0, 100)
    const baseY = rand(0, 100)
    items.push({
      id: i,
      baseX,
      baseY,
      size,
      color: `hsla(${hue}, ${sat}%, ${light}%, 0)`,
      colorPeak: `hsla(${hue}, ${sat}%, ${light}%, 0.9)`,
      dur: rand(3.5, 9),
      delay: rand(0, 7),
      driftX: rand(-18, 18),
      driftY: rand(-14, 8),
      style: {
        width: `${size}px`,
        height: `${size}px`,
        left: `${baseX}%`,
        top: `${baseY}%`,
        '--sp-color': `hsla(${hue}, ${sat}%, ${light}%, 0.9)`,
        '--sp-dur': `${rand(3.5, 9)}s`,
        '--sp-delay': `${rand(0, 7)}s`,
        '--sp-drift-x': `${rand(-18, 18)}px`,
        '--sp-drift-y': `${rand(-14, 8)}px`,
      },
    })
  }
  return items
}

// 鼠标靠近的光点会被轻微吸引
function updateMouseAttract() {
  const list = sparkles.value
  if (!mouse.active) {
    // 鼠标离开窗口：只清空一次，避免每帧对 44 个元素做无意义的空写
    for (const s of list) {
      if (!s._cleared) {
        s.style.transform = ''
        s.style.opacity = ''
        s._cleared = true
      }
    }
    return
  }
  // 视口尺寸
  const vw = window.innerWidth
  const vh = window.innerHeight
  for (const s of list) {
    const sx = (s.baseX / 100) * vw
    const sy = (s.baseY / 100) * vh
    const dx = mouse.x - sx
    const dy = mouse.y - sy
    const dist = Math.sqrt(dx * dx + dy * dy)
    const range = 220
    if (dist < range) {
      const force = (1 - dist / range) * 0.55
      const tx = dx * force
      const ty = dy * force
      s.style.transform = `translate(${tx}px, ${ty}px)`
      s.style.opacity = String(0.55 + force * 0.45)
      s._cleared = false
    } else if (!s._cleared) {
      // 超出范围：同样只在状态变化时清空一次
      s.style.transform = ''
      s.style.opacity = ''
      s._cleared = true
    }
  }
}

function onMove(e) {
  mouse.x = e.clientX
  mouse.y = e.clientY
  if (!mouse.active) mouse.active = true
  lastMoveTime = performance.now()
  startLoop()
}

function onLeave() {
  mouse.active = false
  updateMouseAttract() // 清空一次
  stopLoop()
}

function frame() {
  updateMouseAttract()
  // 鼠标静止超过阈值即停止循环，避免无意义的每帧空转占用主线程
  if (performance.now() - lastMoveTime > IDLE_MS) {
    rafId = null
    return
  }
  rafId = requestAnimationFrame(frame)
}

function startLoop() {
  if (rafId == null && !document.hidden) {
    rafId = requestAnimationFrame(frame)
  }
}

function stopLoop() {
  if (rafId != null) {
    cancelAnimationFrame(rafId)
    rafId = null
  }
}

function onVisibility() {
  if (document.hidden) {
    stopLoop()
  } else if (mouse.active) {
    // 回到前台且指针仍在：续一段空闲窗口再跑，避免静止时持续空转
    lastMoveTime = performance.now()
    startLoop()
  }
}

let lastMoveTime = 0
const IDLE_MS = 140

onMounted(() => {
  sparkles.value = generateSparkles()
  window.addEventListener('mousemove', onMove, { passive: true })
  window.addEventListener('mouseleave', onLeave)
  document.addEventListener('visibilitychange', onVisibility)
  // 不在挂载即开循环：星点靠 CSS 闪烁/飘移，JS 仅在鼠标移动时接管吸引偏移
})

onBeforeUnmount(() => {
  window.removeEventListener('mousemove', onMove)
  window.removeEventListener('mouseleave', onLeave)
  document.removeEventListener('visibilitychange', onVisibility)
  stopLoop()
})
</script>

<style scoped>
.sparkle-field {
  position: fixed;
  inset: 0;
  z-index: 0;
  pointer-events: none;
  overflow: hidden;
}

.sp {
  position: absolute;
  border-radius: 50%;
  background: var(--sp-color);
  box-shadow: 0 0 calc(var(--sp-size, 3px) * 1.5) var(--sp-color);
  animation:
    sp-twinkle var(--sp-dur, 5s) ease-in-out var(--sp-delay, 0s) infinite,
    sp-drift calc(var(--sp-dur, 5s) * 2.2) ease-in-out var(--sp-delay, 0s) infinite alternate;
  will-change: transform, opacity;
  transition: opacity 0.35s ease;
}

@keyframes sp-twinkle {
  0%, 100% { opacity: 0.15; }
  35%      { opacity: 0.85; }
  50%      { opacity: 0.9; }
  65%      { opacity: 0.25; }
}

@keyframes sp-drift {
  0%   { transform: translate(0, 0); }
  100% { transform: translate(var(--sp-drift-x, 0), var(--sp-drift-y, 0)); }
}

/* 移动端：减少光点数量带来的视觉压力，保留一半 */
@media (max-width: 768px) {
  .sp { opacity: 0.35 !important; }
}
</style>
