<template>
  <canvas ref="canvasRef" class="particle-bg"></canvas>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount } from 'vue'

const canvasRef = ref(null)
let ctx, w, h, particles, animationId

class Particle {
  constructor() {
    this.reset()
    this.y = Math.random() * h
  }
  reset() {
    this.x = Math.random() * (w + 200) - 100
    this.y = -20
    this.size = Math.random() * 3 + 1
    this.speed = Math.random() * 0.6 + 0.2
    this.opacity = Math.random() * 0.5 + 0.1
    this.wobble = Math.random() * 0.5
    this.phase = Math.random() * Math.PI * 2
  }
  update() {
    this.y += this.speed
    this.x += Math.sin(this.y * 0.02 + this.phase) * this.wobble
    if (this.y > h + 20) this.reset()
  }
  draw() {
    ctx.beginPath()
    ctx.arc(this.x, this.y, this.size, 0, Math.PI * 2)
    ctx.fillStyle = `rgba(56, 189, 248, ${this.opacity})`
    ctx.fill()
  }
}

const init = () => {
  const canvas = canvasRef.value
  if (!canvas) return
  ctx = canvas.getContext('2d')
  resize()
  particles = Array.from({ length: 50 }, () => new Particle())
  animate()
}

const resize = () => {
  const canvas = canvasRef.value
  if (!canvas || !canvas.parentElement) return
  w = canvas.width = canvas.parentElement.offsetWidth
  h = canvas.height = canvas.parentElement.offsetHeight
}

const animate = () => {
  ctx.clearRect(0, 0, w, h)
  particles.forEach(p => { p.update(); p.draw() })
  animationId = requestAnimationFrame(animate)
}

onMounted(init)
onBeforeUnmount(() => animationId && cancelAnimationFrame(animationId))
</script>

<style scoped>
.particle-bg {
  position: absolute;
  top: 0; left: 0;
  width: 100%; height: 100%;
  pointer-events: none;
  z-index: 0;
}
</style>
