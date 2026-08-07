<template>
  <div ref="mount" class="three-bg"></div>
</template>

<script setup>
/**
 * ThreeBg — 纯 Three.js 粒子环/几何场 3D 背景。
 *
 * 依赖：需要 `npm install three`
 * 用法：<ThreeBg />  作为 hero 容器的第一个子元素（position: absolute 铺满）。
 */
import { ref, onMounted, onBeforeUnmount } from 'vue'

const props = defineProps({
  color:  { type: String, default: '#38bdf8' },
  count:  { type: Number, default: 200 },
  speed:  { type: Number, default: 0.25 },
})

const mount = ref(null)
let renderer, scene, camera, group, animId = null

onMounted(async () => {
  const THREE = await import('three')

  const el = mount.value
  const w = el.offsetWidth
  const h = el.offsetHeight

  // 渲染器
  renderer = new THREE.WebGLRenderer({ alpha: true, antialias: true })
  renderer.setSize(w, h)
  renderer.setPixelRatio(Math.min(devicePixelRatio, 2))
  el.appendChild(renderer.domElement)

  // 场景
  scene = new THREE.Scene()
  camera = new THREE.PerspectiveCamera(50, w / h, 0.1, 100)
  camera.position.z = 28

  // 粒子环
  const geometry = new THREE.BufferGeometry()
  const positions = new Float32Array(props.count * 3)
  const r1 = 9, r2 = 15
  for (let i = 0; i < props.count; i++) {
    const angle = (i / props.count) * Math.PI * 2
    const r = i % 2 === 0 ? r1 : r2
    const y = (i / props.count - 0.5) * 14
    positions[i * 3]     = Math.cos(angle) * r + (Math.random() - 0.5) * 2
    positions[i * 3 + 1] = y + (Math.random() - 0.5) * 2
    positions[i * 3 + 2] = Math.sin(angle) * r + (Math.random() - 0.5) * 2
  }
  geometry.setAttribute('position', new THREE.BufferAttribute(positions, 3))

  const material = new THREE.PointsMaterial({
    color: new THREE.Color(props.color),
    size: 0.14,
    transparent: true,
    blending: THREE.AdditiveBlending,
    depthWrite: false,
  })

  group = new THREE.Points(geometry, material)
  scene.add(group)

  // 窗口缩放
  const onResize = () => {
    const nw = el.offsetWidth
    const nh = el.offsetHeight
    renderer.setSize(nw, nh)
    camera.aspect = nw / nh
    camera.updateProjectionMatrix()
  }
  window.addEventListener('resize', onResize)

  // 动画循环
  const animate = () => {
    animId = requestAnimationFrame(animate)
    group.rotation.y += props.speed * 0.01
    group.rotation.x += props.speed * 0.004
    group.rotation.z += props.speed * 0.003
    renderer.render(scene, camera)
  }
  animate()

  onBeforeUnmount(() => {
    cancelAnimationFrame(animId)
    window.removeEventListener('resize', onResize)
    renderer?.dispose()
    geometry?.dispose()
    material?.dispose()
    el?.removeChild(renderer?.domElement)
  })
})
</script>

<style scoped>
.three-bg {
  position: absolute; inset: 0; z-index: 0;
  overflow: hidden;
  pointer-events: none;
}
.three-bg :deep(canvas) { display: block; }
</style>