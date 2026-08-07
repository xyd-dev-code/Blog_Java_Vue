<template>
  <div ref="trail" class="star-trail" aria-hidden="true">
    <svg
      v-for="(s, i) in stars"
      :key="i"
      class="st-star"
      :style="{
        width: `${s.size}px`,
        height: `${s.size}px`,
        opacity: s.alpha,
        filter: `drop-shadow(0 0 4px ${s.glow})`
      }"
      viewBox="0 0 24 24"
    >
      <!-- 每颗星独立渐变：沿拖尾由暖金渐变为浅蓝，呼应晴天主题 -->
      <defs>
        <linearGradient :id="`st-g-${uid}-${i}`" x1="0" y1="0" x2="1" y2="1">
          <stop offset="0%" :stop-color="s.from" />
          <stop offset="100%" :stop-color="s.to" />
        </linearGradient>
      </defs>
      <path
        d="M12 1.6l2.6 6.3 6.8.55-5.17 4.45 1.57 6.64L12 15.9l-5.8 3.64 1.57-6.64L2.6 8.45l6.8-.55z"
        :fill="`url(#st-g-${uid}-${i})`"
      />
    </svg>
  </div>
</template>

<script setup>
/**
 * StarTrail — 鼠标星星拖尾
 *
 * 一串五角星链式追随鼠标：首星追指针，其余各星追前一颗，
 * 形成自然的甩尾轨迹。尺寸、透明度、颜色沿尾递减/渐变，
 * 并带缓慢自转，星光感更足。
 *
 * ⚠️ 不使用 mix-blend-mode：screen 在白底会完全隐形
 * （白色是 screen 的吸收态），multiply 则让暖色发灰发脏。
 * 浅色主题下直接用实色 + 低透明度最可控。
 */
import { ref, onMounted, onBeforeUnmount, getCurrentInstance } from 'vue'

const props = defineProps({
  // 星星数量。保持个位数，是"拖尾"而非密集粒子
  count: { type: Number, default: 8 },
  // 跟随系数：越小拖尾越长越飘
  lerp: { type: Number, default: 0.3 }
})

// SVG 渐变 id 必须全局唯一，否则多实例会互相覆盖
const uid = getCurrentInstance()?.uid ?? Math.floor(Math.random() * 1e6)

const trail = ref(null)

// 沿拖尾：由大到小、由亮到淡、由暖金渐变到浅蓝
const stars = Array.from({ length: props.count }, (_, i) => {
  const t = i / Math.max(props.count - 1, 1)
  return {
    size: 20 - t * 12,          // 20px → 8px
    alpha: 0.92 - t * 0.72,     // 0.92 → 0.20
    from: mix('#fde68a', '#bae6fd', t),   // 暖金 → 浅蓝
    to: mix('#f59e0b', '#38bdf8', t),     // 琥珀 → 天蓝
    glow: mix('#fbbf24', '#38bdf8', t)    // 光晕：暖金 → 天蓝，呼应星色
  }
})

// 十六进制颜色线性插值
function mix (a, b, t) {
  const p = (h) => [1, 3, 5].map((i) => parseInt(h.slice(i, i + 2), 16))
  const [r1, g1, b1] = p(a)
  const [r2, g2, b2] = p(b)
  const c = (x, y) => Math.round(x + (y - x) * t).toString(16).padStart(2, '0')
  return `#${c(r1, r2)}${c(g1, g2)}${c(b1, b2)}`
}

let raf = null
let mx = -9999, my = -9999
const pts = stars.map(() => ({ x: -9999, y: -9999 }))
let started = false
let spin = 0
// 鼠标静止超过该阈值即停止循环，避免无意义的每帧空转占用主线程
let lastMoveTime = 0
const IDLE_MS = 220

function startLoop() {
  if (raf == null && !document.hidden) raf = requestAnimationFrame(loop)
}
function stopLoop() {
  if (raf != null) {
    cancelAnimationFrame(raf)
    raf = null
  }
}
function onVisibility() {
  // 标签页隐藏时暂停；回到前台后由下次鼠标移动重新启动
  if (document.hidden) stopLoop()
}

const onMove = (e) => {
  mx = e.clientX
  my = e.clientY
  lastMoveTime = performance.now()
  if (!started) {
    // 首次进入时全部归位到鼠标，避免从屏幕外飞入
    started = true
    pts.forEach((p) => { p.x = mx; p.y = my })
    if (trail.value) trail.value.style.opacity = '1'
  }
  startLoop()
}

const onLeave = () => {
  if (trail.value) trail.value.style.opacity = '0'
  stopLoop()
}

const onEnter = () => {
  if (started && trail.value) trail.value.style.opacity = '1'
}

const loop = () => {
  spin = (spin + 0.6) % 360
  const nodes = trail.value?.children
  for (let i = 0; i < pts.length; i++) {
    // 链式：首星追鼠标，其余各星追前一颗
    const target = i === 0 ? { x: mx, y: my } : pts[i - 1]
    pts[i].x += (target.x - pts[i].x) * props.lerp
    pts[i].y += (target.y - pts[i].y) * props.lerp
    const n = nodes?.[i]
    if (n) {
      // 越靠尾部转得越慢，层次更自然
      const rot = spin * (1 - i / (pts.length * 1.6))
      n.style.transform =
        `translate3d(${pts[i].x.toFixed(1)}px, ${pts[i].y.toFixed(1)}px, 0) translate(-50%, -50%) rotate(${rot.toFixed(1)}deg)`
    }
  }
  // 鼠标静止超过阈值即停循环，避免无意义的每帧空转（标签页隐藏时也已暂停）
  if (performance.now() - lastMoveTime > IDLE_MS) {
    raf = null
    return
  }
  raf = requestAnimationFrame(loop)
}

onMounted(() => {
  const reduce = window.matchMedia?.('(prefers-reduced-motion: reduce)').matches ?? false
  // 触摸为主的设备没有真实指针，跳过
  const hasFinePointer = window.matchMedia?.('(pointer: fine)').matches ?? true
  if (reduce || !hasFinePointer) return

  window.addEventListener('mousemove', onMove, { passive: true })
  document.addEventListener('mouseleave', onLeave)
  document.addEventListener('mouseenter', onEnter)
  document.addEventListener('visibilitychange', onVisibility)
  // 不在挂载即开循环：指针未动时无需每帧重写，首次移动由 onMove 启动
})

onBeforeUnmount(() => {
  stopLoop()
  window.removeEventListener('mousemove', onMove)
  document.removeEventListener('mouseleave', onLeave)
  document.removeEventListener('mouseenter', onEnter)
  document.removeEventListener('visibilitychange', onVisibility)
})
</script>

<style scoped>
.star-trail {
  position: fixed;
  inset: 0;
  pointer-events: none;
  z-index: 9998;
  opacity: 0;
  transition: opacity 0.4s ease;
}
.st-star {
  /* 父层已 fixed 铺满视口，此处用 absolute 相对它定位，
     避免祖先的 transform/filter 改变 fixed 的 containing block */
  position: absolute;
  top: 0;
  left: 0;
  pointer-events: none;
  will-change: transform;
  filter: drop-shadow(0 0 4px rgba(251, 191, 36, 0.45));
}
</style>
