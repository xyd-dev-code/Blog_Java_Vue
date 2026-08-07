<template>
  <div
    ref="el"
    class="hero-light"
    :style="lightStyle"
    aria-hidden="true"
  >
    <div class="hl-orb hl-orb--1" :style="orbStyle1" />
    <div class="hl-orb hl-orb--2" :style="orbStyle2" />
    <div class="hl-beam" :style="beamStyle" />
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, onBeforeUnmount } from 'vue'

const el = ref(null)
const active = ref(false)
const pos = reactive({ x: 0.5, y: 0.5 })
const target = reactive({ x: 0.5, y: 0.5 })

const lightStyle = ref({})
const orbStyle1 = ref({})
const orbStyle2 = ref({})
const beamStyle = ref({})

let rafId = null

function lerp(a, b, t) {
  return a + (b - a) * t
}

function updateStyles() {
  const x = pos.x
  const y = pos.y

  // 整体光晕位移
  const ox = (x - 0.5) * 30
  const oy = (y - 0.5) * 18
  orbStyle1.value = {
    transform: `translate(${ox * 1.2}px, ${oy * 0.8}px)`,
    opacity: active.value ? 1 : 0.3,
  }
  orbStyle2.value = {
    transform: `translate(${-ox * 0.7}px, ${-oy * 1.1}px)`,
    opacity: active.value ? 0.8 : 0.2,
  }
  beamStyle.value = {
    transform: `translate(${ox * 0.5}px, ${oy * 0.5}px) rotate(${(x - 0.5) * 5}deg)`,
    opacity: active.value ? 0.6 : 0.15,
  }
}

function animate() {
  pos.x = lerp(pos.x, target.x, 0.06)
  pos.y = lerp(pos.y, target.y, 0.06)
  updateStyles()
  rafId = requestAnimationFrame(animate)
}

function onMove(e) {
  if (!el.value) return
  const rect = el.value.getBoundingClientRect()
  target.x = (e.clientX - rect.left) / rect.width
  target.y = (e.clientY - rect.top) / rect.height
  if (!active.value) active.value = true
}

function onLeave() {
  active.value = false
}

onMounted(() => {
  const dom = el.value
  if (!dom) return
  dom.addEventListener('mousemove', onMove, { passive: true })
  dom.addEventListener('mouseleave', onLeave)
  rafId = requestAnimationFrame(animate)
})

onBeforeUnmount(() => {
  const dom = el.value
  if (dom) {
    dom.removeEventListener('mousemove', onMove)
    dom.removeEventListener('mouseleave', onLeave)
  }
  if (rafId) cancelAnimationFrame(rafId)
})
</script>

<style scoped>
.hero-light {
  position: absolute;
  inset: 0;
  pointer-events: none;
  z-index: 1;
  overflow: hidden;
}

.hl-orb {
  position: absolute;
  border-radius: 50%;
  filter: blur(60px);
  transition: opacity 0.7s ease;
}

.hl-orb--1 {
  width: 320px;
  height: 320px;
  background: radial-gradient(circle,
    rgba(255, 255, 255, 0.55) 0%,
    rgba(251, 191, 36, 0.25) 30%,
    transparent 70%
  );
  top: -80px;
  right: 10%;
}

.hl-orb--2 {
  width: 260px;
  height: 260px;
  background: radial-gradient(circle,
    rgba(255, 255, 255, 0.5) 0%,
    rgba(56, 189, 248, 0.3) 30%,
    transparent 70%
  );
  bottom: -60px;
  left: 5%;
}

.hl-beam {
  position: absolute;
  top: -60%;
  left: 15%;
  width: 70%;
  height: 200%;
  background: linear-gradient(
    105deg,
    transparent 0%,
    transparent 35%,
    rgba(255, 255, 255, 0.1) 48%,
    rgba(255, 255, 255, 0.18) 50%,
    rgba(255, 255, 255, 0.1) 52%,
    transparent 65%,
    transparent 100%
  );
  transition: opacity 0.7s ease;
}
</style>
