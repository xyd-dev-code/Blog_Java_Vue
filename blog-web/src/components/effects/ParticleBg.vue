<template>
  <canvas ref="canvas" class="particle-bg"></canvas>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount } from 'vue'

const props = defineProps({
  color:  { type: String, default: 'rgba(56,189,248,' },  // 主色前缀
  count:  { type: Number, default: 45 },
  size:   { type: [Number, Array], default: () => [0.6, 2.4] },
  speed:  { type: [Number, Array], default: () => [0.15, 0.5] },
  opacity:{ type: [Number, Array], default: () => [0.08, 0.28] },
})

const canvas = ref(null)
let ctx = null, animId = null
let particles = []
let w = 0, h = 0

const rand = (min, max) => min + Math.random() * (max - min)
const randItem = (v) => Array.isArray(v) ? rand(v[0], v[1]) : v

onMounted(() => {
  ctx = canvas.value.getContext('2d')
  const resize = () => {
    const c = canvas.value
    const dpr = devicePixelRatio || 1
    w = window.innerWidth
    h = window.innerHeight
    c.width = w * dpr
    c.height = h * dpr
    c.style.width = w + 'px'
    c.style.height = h + 'px'
    ctx.setTransform(dpr, 0, 0, dpr, 0, 0)
  }
  resize()
  window.addEventListener('resize', resize)

  const init = () => {
    particles = []
    for (let i = 0; i < props.count; i++) {
      particles.push({
        x: Math.random() * w,
        y: Math.random() * h,
        r: randItem(props.size),
        vx: (Math.random() - 0.5) * randItem(props.speed),
        vy: (Math.random() - 0.5) * randItem(props.speed),
        o: randItem(props.opacity),
      })
    }
  }
  init()

  const loop = () => {
    ctx.clearRect(0, 0, w, h)
    for (const p of particles) {
      p.x += p.vx
      p.y += p.vy
      if (p.x < -20) p.x = w + 20
      if (p.x > w + 20) p.x = -20
      if (p.y < -20) p.y = h + 20
      if (p.y > h + 20) p.y = -20
      ctx.beginPath()
      ctx.arc(p.x, p.y, p.r, 0, Math.PI * 2)
      ctx.fillStyle = props.color + p.o.toFixed(3) + ')'
      ctx.fill()
    }
    animId = requestAnimationFrame(loop)
  }
  animId = requestAnimationFrame(loop)

  onBeforeUnmount(() => {
    cancelAnimationFrame(animId)
    window.removeEventListener('resize', resize)
  })
})
</script>

<style scoped>
.particle-bg {
  position: absolute; inset: 0; z-index: 0;
  pointer-events: none;
}
</style>